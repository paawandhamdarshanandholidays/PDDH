package com.pddh.ceo_service.dto;

import java.util.List;

public record RoleDashboardResponse(
        String role, String displayName, String hierarchy, List<String> capabilities) {
}
