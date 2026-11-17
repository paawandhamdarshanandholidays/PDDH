package com.pddh.auth_service.dto;

public record UsernameAvailabilityResponse(
        String username,
        boolean usernameAvailable,
        boolean emailAvailable,
        boolean mobileAvailable) {
}
