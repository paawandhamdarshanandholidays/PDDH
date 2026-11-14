package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.service.CeoRegistrationService;
import com.pddh.auth_service.service.ProfileSyncService;
import com.pddh.auth_service.service.RegistrationIdentityService;

class CeoRegistrationServiceTest {
    private UserRepository userRepository;
    private JdbcTemplate jdbcTemplate;
    private CeoRegistrationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        service = new CeoRegistrationService(userRepository, passwordEncoder, jdbcTemplate,
                mock(ProfileSyncService.class), new RegistrationIdentityService(userRepository));
    }

    @Test
    void firstCeoCanBeRegisteredDuringBootstrap() {
        RegisterRequest request = new RegisterRequest(
                "Chief Executive", "chief", "long-password", null, "chief@example.com", null, "Male");
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(null);
        when(userRepository.existsByRoleIgnoreCase("CEO")).thenReturn(false);
        when(userRepository.existsByRoleIgnoreCaseAndEmailIgnoreCase("CEO", request.email())).thenReturn(false);
        when(userRepository.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CEO", request.username())).thenReturn(false);

        String response = service.register(null, request);

        assertThat(response).isEqualTo("CEO account registered successfully");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void firstCeoCanBeRegisteredWhenRequestHasAnExistingNonCeoToken() {
        RegisterRequest request = new RegisterRequest(
                "Chief Executive", "chief", "long-password", "CEO", "chief@example.com", null, "Female");
        User existingAdmin = new User();
        existingAdmin.setRole("ADMIN");
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(null);
        when(userRepository.existsByRoleIgnoreCase("CEO")).thenReturn(false);
        when(userRepository.existsByRoleIgnoreCaseAndEmailIgnoreCase("CEO", request.email())).thenReturn(false);
        when(userRepository.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CEO", request.username())).thenReturn(false);

        String response = service.register(existingAdmin, request);

        assertThat(response).isEqualTo("CEO account registered successfully");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void bootstrapIsClosedAfterACeoExists() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(null);
        when(userRepository.existsByRoleIgnoreCase("CEO")).thenReturn(true);

        assertThatThrownBy(() -> service.register(null,
                new RegisterRequest("Chief", "chief", "long-password", "CEO", "chief@example.com", null, "Others")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("A CEO profile already exists")
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void nonCeoCannotRegisterAnotherCeoAfterBootstrap() {
        User existingAdmin = new User();
        existingAdmin.setRole("ADMIN");
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(null);
        when(userRepository.existsByRoleIgnoreCase("CEO")).thenReturn(true);

        assertThatThrownBy(() -> service.register(existingAdmin,
                new RegisterRequest("Chief", "chief", "long-password", "CEO", "chief@example.com", null, "Others")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void existingCeoCannotRegisterAnotherCeo() {
        User existingCeo = new User();
        existingCeo.setRole("CEO");
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(null);
        when(userRepository.existsByRoleIgnoreCase("CEO")).thenReturn(true);

        assertThatThrownBy(() -> service.register(existingCeo,
                new RegisterRequest("Chief", "chief", "long-password", "CEO", "chief@example.com", null, "Others")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("A CEO profile already exists")
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
