package com.pddh.trips_service.service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.trips_service.repository.ContentItemRepository;
import com.pddh.saga_events.BookingCreatedEvent;
import com.pddh.saga_events.BookingValidationEvent;

@Component
@ConditionalOnProperty(name = "app.saga.enabled", havingValue = "true")
public class BookingValidationListener {
    private static final Logger logger = LoggerFactory.getLogger(BookingValidationListener.class);
    private final ContentItemRepository contentRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String configuredType;
    private final String validationResultsTopic;
    private final Retry retry;
    private final MeterRegistry meterRegistry;

    public BookingValidationListener(
            ContentItemRepository contentRepository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.content.type}") String configuredType,
            @Value("${app.saga.validation-results-topic}") String validationResultsTopic,
            RetryRegistry retryRegistry,
            MeterRegistry meterRegistry) {
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.configuredType = configuredType.toLowerCase(Locale.ROOT);
        this.validationResultsTopic = validationResultsTopic;
        this.retry = retryRegistry.retry("booking-validation-publish");
        this.meterRegistry = meterRegistry;
        this.retry.getEventPublisher().onRetry(event ->
                this.meterRegistry.counter("pddh.saga.validation.retries").increment());
    }

    @KafkaListener(topics = "${app.saga.booking-created-topic}", groupId = "${spring.application.name}")
    @Transactional
    public void validateBooking(String message) {
        final BookingCreatedEvent event;
        try {
            event = objectMapper.readValue(message, BookingCreatedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid booking-created saga event", exception);
        }
        String expectedType = "trips".equals(configuredType) ? "TRIP" : "STAY";
        if (!expectedType.equalsIgnoreCase(event.bookingType())) {
            logger.debug("Ignoring booking {} on the {} validation queue", event.bookingId(), configuredType);
            return;
        }
        String itemId = "TRIP".equals(expectedType) ? event.tripId() : event.hotelId();
        boolean accepted = itemId != null && contentRepository.findByIdAndType(itemId, configuredType).isPresent();
        String reason = accepted ? null : "Selected " + configuredType + " item is unavailable";
        BookingValidationEvent result = new BookingValidationEvent(
                event.bookingId(), expectedType, accepted, reason);
        try {
            String serializedResult = objectMapper.writeValueAsString(result);
            Retry.decorateRunnable(retry, () -> publishValidation(event.bookingId(), serializedResult)).run();
            meterRegistry.counter("pddh.saga.validations.published").increment();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Booking validation result could not be serialized", exception);
        } catch (KafkaException exception) {
            meterRegistry.counter("pddh.saga.validation.publish.failures").increment();
            throw exception;
        }
    }

    private void publishValidation(String bookingId, String payload) {
        try {
            kafkaTemplate.send(validationResultsTopic, bookingId, payload).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing booking validation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaException("Could not publish booking validation", exception);
        }
    }
}
