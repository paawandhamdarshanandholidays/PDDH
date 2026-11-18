package com.pddh.auth_service.dto;

public record UserDirectoryResponse(
        String id,
        String source,
        String name,
        String contactNumber,
        String email,
        String address,
        String referredBy,
        String serviceProvided) {
}
