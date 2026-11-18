package com.pddh.auth_service.dto;

import java.util.List;

public record UserDirectoryPageResponse(
        List<UserDirectoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
