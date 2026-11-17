package com.pddh.manager_service.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.role.manager")
public record RoleDashboardProperties(
        String expected, String path, String displayName, String hierarchy, List<String> capabilities) {
}
