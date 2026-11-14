package com.pddh.auth_service.dto;

public record UserDirectoryRequest(
        String name,
        String contactNumber,
        String email,
        String address,
        String referredBy,
        String serviceProvided) {
}
