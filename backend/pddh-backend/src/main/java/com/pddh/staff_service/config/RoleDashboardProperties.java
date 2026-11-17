package com.pddh.staff_service.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.role.staff")
public record RoleDashboardProperties(
        String expected, String path, String displayName, String hierarchy, List<String> capabilities) {
}
