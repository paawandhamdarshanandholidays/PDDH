package com.pddh.auth_service.dto;

public record UserDirectoryResponse(
        Long id,
        String name,
        String contactNumber,
        String email,
        String address,
        String referredBy,
        String serviceProvided) {
}
