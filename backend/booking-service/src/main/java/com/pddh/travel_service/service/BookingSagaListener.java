package com.pddh.travel_service.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.saga_events.BookingSagaCompletedEvent;
import com.pddh.saga_events.BookingValidationEvent;
import com.pddh.travel_service.entity.Booking;
import com.pddh.travel_service.repository.BookingRepository;

@Component
public class BookingSagaListener {
    private static final Logger logger = LoggerFactory.getLogger(BookingSagaListener.class);
    private final BookingRepository bookingRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public BookingSagaListener(
            BookingRepository bookingRepository, OutboxService outboxService, ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.saga.validation-results-topic}", groupId = "${spring.application.name}")
    @Transactional
    public void handleValidation(String message) {
        final BookingValidationEvent result;
        try {
            result = objectMapper.readValue(message, BookingValidationEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid booking validation event", exception);
        }
        Optional<Booking> resultBooking = bookingRepository.findById(result.bookingId());
        if (resultBooking.isEmpty()) {
            logger.warn("Ignoring saga result for missing booking {}", result.bookingId());
            return;
        }
        Booking booking = resultBooking.get();
        if (!"PENDING".equals(booking.getStatus())) {
            return;
        }
        String status = result.accepted() ? "CONFIRMED" : "REJECTED";
        booking.setStatus(status);
        bookingRepository.saveAndFlush(booking);
        outboxService.enqueue(booking.getId(), "booking." + status.toLowerCase(),
                new BookingSagaCompletedEvent(booking.getId(), booking.getBookingType(), status));
    }
}
