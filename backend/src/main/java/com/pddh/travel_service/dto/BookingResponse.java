package com.pddh.travel_service.dto;

import java.time.LocalDate;

public record BookingResponse(String reference, String message, LocalDate bookingDate) {
}
