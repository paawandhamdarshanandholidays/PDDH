package com.pddh.manager_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.manager_service.api_common.resilience.RateLimitGuard;
import com.pddh.manager_service.config.RoleDashboardProperties;
import com.pddh.manager_service.security.RoleIdentityService;

import jakarta.servlet.http.HttpServletRequest;

class RoleDashboardControllerTest {
    @Test
    void matchingRoleGetsDashboardCapabilities() {
        RoleDashboardProperties properties = new RoleDashboardProperties(
                "ADMIN", "/api/roles/admin", "Administrator", "CEO > ADMIN > MANAGER > STAFF > CUSTOMER",
                List.of("manage-content"));
        RoleIdentityService identityService = mock(RoleIdentityService.class);
        RateLimitGuard rateLimitGuard = mock(RateLimitGuard.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(identityService.requireRole(request)).thenReturn("ADMIN");
        RoleDashboardController controller = new RoleDashboardController(properties, identityService, rateLimitGuard);

        var response = controller.dashboard(request);

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.capabilities()).containsExactly("manage-content");
        verify(rateLimitGuard).check("role.dashboard.admin");
    }

    @Test
    void differentRoleCannotUseDashboardService() {
        RoleDashboardProperties properties = new RoleDashboardProperties(
                "CEO", "/api/roles/ceo", "CEO", "CEO > ADMIN > MANAGER > STAFF > CUSTOMER",
                List.of("full-organization-control"));
        RoleIdentityService identityService = mock(RoleIdentityService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(identityService.requireRole(request)).thenReturn("ADMIN");
        RoleDashboardController controller = new RoleDashboardController(
                properties, identityService, mock(RateLimitGuard.class));

        assertThatThrownBy(() -> controller.dashboard(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("available only to CEO");
    }
}
