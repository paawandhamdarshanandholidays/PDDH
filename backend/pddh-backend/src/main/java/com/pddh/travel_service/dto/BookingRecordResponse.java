package com.pddh.travel_service.dto;

import java.time.LocalDate;

public record BookingRecordResponse(
        String id, String customerId, String bookingType, String customerName, String email, String phone, int guests,
        LocalDate bookingDate, LocalDate startDate, LocalDate endDate, String tripId, String hotelId, String status) {
}
