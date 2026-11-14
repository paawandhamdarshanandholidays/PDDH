package com.pddh.saga_events;

public record BookingCreatedEvent(String bookingId, String bookingType, String tripId, String hotelId) {
}
