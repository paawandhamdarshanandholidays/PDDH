package com.pddh.contact_service.dto;

import java.time.Instant;

public record EnquiryResponse(
        String id, String name, String email, String phone, String subject, String message,
        String status, Instant createdAt) {
}
