package com.pddh.travel_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookingRequest(
        @NotBlank String bookingType,
        @NotBlank String customerName,
        @NotBlank String phone,
        @Min(1) @Max(30) Integer guests,
        @NotBlank String startDate,
        String endDate,
        String tripId,
        String hotelId) {
}
