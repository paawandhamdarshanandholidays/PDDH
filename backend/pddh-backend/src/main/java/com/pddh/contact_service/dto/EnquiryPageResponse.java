package com.pddh.contact_service.dto;

import java.util.List;

public record EnquiryPageResponse(
        List<EnquiryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
