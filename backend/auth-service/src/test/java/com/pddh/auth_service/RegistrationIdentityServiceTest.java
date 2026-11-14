package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.UserRole;
import com.pddh.auth_service.service.RegistrationIdentityService;

class RegistrationIdentityServiceTest {
    private UserRepository users;
    private RegistrationIdentityService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        service = new RegistrationIdentityService(users);
    }

    @Test
    void generatesUniqueEmailBasedUsernamesWithinTheRequestedRole() {
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "same")).thenReturn(true);
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "same1")).thenReturn(true);
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "same2")).thenReturn(false);
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "same@example.com")).thenReturn(false);

        var result = service.checkAvailability("same@example.com", UserRole.CUSTOMER, "same", null);

        assertThat(result.username()).isEqualTo("same2");
        assertThat(result.usernameAvailable()).isTrue();
        assertThat(result.emailAvailable()).isTrue();
        assertThat(result.mobileAvailable()).isTrue();
    }

    @Test
    void permitsMatchingEmailAndUsernameAcrossDifferentRoles() {
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CEO", "same")).thenReturn(false);
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CEO", "same@example.com")).thenReturn(false);

        var result = service.checkAvailability("same@example.com", UserRole.CEO, "same", null);

        assertThat(result.username()).isEqualTo("same");
        assertThat(result.emailAvailable()).isTrue();
    }

    @Test
    void checksEmailUsernameAndMobileInOneRoleScopedRequest() {
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "same")).thenReturn(false);
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "same@example.com")).thenReturn(true);
        when(users.existsByRoleIgnoreCaseAndMobileIgnoreCase("CUSTOMER", "123")).thenReturn(true);

        var result = service.checkAvailability("same@example.com", UserRole.CUSTOMER, "same", " 123 ");

        assertThat(result.username()).isEqualTo("same");
        assertThat(result.usernameAvailable()).isTrue();
        assertThat(result.emailAvailable()).isFalse();
        assertThat(result.mobileAvailable()).isFalse();
    }

    @Test
    void rejectsEmailAlreadyUsedByTheSameRole() {
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "same@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.validateUnique(
                UserRole.CUSTOMER, "same@example.com", "same", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email is already registered for this role");
    }

    @Test
    void rejectsMobileAlreadyUsedByTheSameRole() {
        when(users.existsByRoleIgnoreCaseAndMobileIgnoreCase("CUSTOMER", "123")).thenReturn(true);

        assertThatThrownBy(() -> service.validateUnique(
                UserRole.CUSTOMER, "new@example.com", "new", "123", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Mobile number is already registered for this role");
    }
}
