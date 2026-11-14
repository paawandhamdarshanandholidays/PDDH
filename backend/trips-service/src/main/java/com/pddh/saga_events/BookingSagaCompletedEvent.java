package com.pddh.saga_events;

public record BookingSagaCompletedEvent(String bookingId, String bookingType, String status) {
}
