package com.pddh.customer_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.customer_service.api_common.resilience.RateLimitGuard;
import com.pddh.customer_service.config.RoleDashboardProperties;
import com.pddh.customer_service.dto.RoleDashboardResponse;
import com.pddh.customer_service.security.RoleIdentityService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/roles/customer")
public class RoleDashboardController {
    private final RoleDashboardProperties properties;
    private final RoleIdentityService identityService;
    private final RateLimitGuard rateLimitGuard;

    public RoleDashboardController(
            RoleDashboardProperties properties,
            RoleIdentityService identityService,
            RateLimitGuard rateLimitGuard) {
        this.properties = properties;
        this.identityService = identityService;
        this.rateLimitGuard = rateLimitGuard;
    }

    @GetMapping("/dashboard")
    public RoleDashboardResponse dashboard(HttpServletRequest request) {
        String role = identityService.requireRole(request);
        if (!properties.expected().equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This dashboard is available only to " + properties.displayName() + " accounts");
        }
        rateLimitGuard.check("role.dashboard." + role.toLowerCase());
        return new RoleDashboardResponse(
                role, properties.displayName(), properties.hierarchy(), properties.capabilities());
    }
}
