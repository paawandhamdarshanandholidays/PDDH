package com.pddh.travel_service.dto;

import java.util.List;

public record BookingPageResponse(
        List<BookingRecordResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
