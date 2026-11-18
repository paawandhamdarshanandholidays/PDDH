package com.pddh.contact_service.dto;

public record EnquiryUpdateRequest(String name, String email, String phone, String subject, String message) {
}
