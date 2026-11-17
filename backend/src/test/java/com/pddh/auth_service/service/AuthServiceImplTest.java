package com.pddh.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
