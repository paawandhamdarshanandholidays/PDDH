package com.pddh.auth_service.dto;

public record UserProfileUpdateRequest(
        String name,
        String email,
        String mobile,
        String role) {
}
