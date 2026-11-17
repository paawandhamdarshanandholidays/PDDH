package com.pddh.auth_service.dto;

import java.util.List;

public record UserProfilePageResponse(
        List<UserProfileResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
