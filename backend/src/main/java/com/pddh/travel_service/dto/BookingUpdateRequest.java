package com.pddh.travel_service.dto;

public record BookingUpdateRequest(
        String customerName,
        String email,
        String phone,
        Integer guests,
        String startDate,
        String endDate,
        String tripId,
        String hotelId) {
}
