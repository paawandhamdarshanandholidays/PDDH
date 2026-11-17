package com.pddh.auth_service.dto;

public record UserProfileResponse(
        String id,
        String name,
        String username,
        String email,
        String mobile,
        String role,
        boolean active) {
}
