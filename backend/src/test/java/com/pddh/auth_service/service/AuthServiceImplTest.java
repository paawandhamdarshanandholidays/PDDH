package com.pddh.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.LoginRequest;
import com.pddh.auth_service.dto.RoleAuthenticationResponse;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.JwtService;
import com.pddh.auth_service.security.UserRole;

class AuthServiceImplTest {
    @Test
    void exposesEverySupportedRoleEvenWhenNoCentralUserExistsForThatRole() {
        AuthServiceImpl authService = new AuthServiceImpl(
                mock(UserRepository.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(ProfileSyncService.class),
                mock(RoleAuthenticationClient.class),
                mock(RegistrationIdentityService.class));

        assertEquals(List.of(UserRole.values()), authService.availableRoles());
    }

    @Test
    void doesNotIssueTokenWhenCentralAccountIsMissingOrInactive() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        RoleAuthenticationClient roleAuthenticationClient = mock(RoleAuthenticationClient.class);
        when(roleAuthenticationClient.authenticate("CUSTOMER", "customer-name", "password"))
                .thenReturn(new RoleAuthenticationResponse("customer-name", "customer@example.com", "CUSTOMER"));
        when(userRepository.updateLastLoginAt(eq("customer-name"), eq("CUSTOMER"), any())).thenReturn(0);
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                mock(PasswordEncoder.class),
                jwtService,
                mock(ProfileSyncService.class),
                roleAuthenticationClient,
                mock(RegistrationIdentityService.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("customer-name", "password", "CUSTOMER")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}
