package com.pddh.roleprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class RoleProfileControllerTest {
    private RoleProfileService profiles;
    private RoleIdentityService identities;
    private RoleProfileController controller;

    @BeforeEach
    void setUp() {
        profiles = mock(RoleProfileService.class);
        identities = mock(RoleIdentityService.class);
        controller = new RoleProfileController(profiles, identities);
    }

    @Test
    void delegatesListingWithAuthenticatedRoleAndPaging() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        var expected = new RoleProfileService.UserProfilePage(List.of(), 2, 10, 0, 0);
        when(identities.requireRole(request)).thenReturn("MANAGER");
        when(profiles.list("MANAGER", 2, 10)).thenReturn(expected);

        assertThat(controller.list(2, 10, request)).isEqualTo(expected);
        verify(profiles).list("MANAGER", 2, 10);
    }

    @Test
    void delegatesInternalAuthenticationAndHandlesNullRequest() {
        var request = new RoleProfileService.AuthenticationRequest("name", "password");
        var expected = new RoleProfileService.AuthenticationResponse("name", "name@test.com", "STAFF");
        when(profiles.authenticate("name", "password")).thenReturn(expected);
        assertThat(controller.authenticate(request)).isEqualTo(expected);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.authenticate(null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(profiles).authenticate("name", "password");
    }
}
