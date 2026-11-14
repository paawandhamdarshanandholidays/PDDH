package com.pddh.travel_service.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import com.pddh.travel_service.entity.OutboxEvent;
import com.pddh.travel_service.repository.OutboxEventRepository;

@Component
public class OutboxPublisher {
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String bookingCreatedTopic;
    private final String completedTopic;
    private final Retry retry;
    private final MeterRegistry meterRegistry;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.saga.booking-created-topic}") String bookingCreatedTopic,
            @Value("${app.saga.completed-topic}") String completedTopic,
            RetryRegistry retryRegistry,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.bookingCreatedTopic = bookingCreatedTopic;
        this.completedTopic = completedTopic;
        this.retry = retryRegistry.retry("booking-saga-publish");
        this.meterRegistry = meterRegistry;
        this.retry.getEventPublisher().onRetry(event ->
                this.meterRegistry.counter("pddh.saga.publish.retries").increment());
    }

    @Scheduled(fixedDelayString = "${app.saga.outbox-poll-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = repository
                .findTop100ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant.now());
        for (OutboxEvent event : pending) {
            try {
                Retry.decorateRunnable(retry, () -> publish(event)).run();
                event.setPublishedAt(Instant.now());
                meterRegistry.counter("pddh.saga.events.published").increment();
            } catch (KafkaException exception) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                long delaySeconds = Math.min(1L << Math.min(attempts, 8), 300L);
                event.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
                meterRegistry.counter("pddh.saga.events.failed").increment();
                logger.warn("Could not publish saga event {} for booking {}; it will be retried",
                        event.getId(), event.getAggregateId(), exception);
            }
        }
        repository.saveAll(pending);
    }

    private void publish(OutboxEvent event) {
        String topic = "booking.created".equals(event.getRoutingKey()) ? bookingCreatedTopic : completedTopic;
        try {
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing booking saga event", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaException("Could not publish booking saga event", exception);
        }
    }
}
