package com.pddh.saga_events;

public record BookingValidationEvent(String bookingId, String bookingType, boolean accepted, String reason) {
}
