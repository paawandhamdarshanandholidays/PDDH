package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.LoginRequest;
import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.RoleAuthenticationResponse;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.JwtService;
import com.pddh.auth_service.service.AuthServiceImpl;
import com.pddh.auth_service.service.ProfileSyncService;
import com.pddh.auth_service.service.RegistrationIdentityService;
import com.pddh.auth_service.service.RoleAuthenticationClient;

import reactor.core.publisher.Mono;

class AuthServiceImplTest {
    private UserRepository users;
    private PasswordEncoder passwords;
    private ProfileSyncService profileSync;
    private RoleAuthenticationClient roleAuthentication;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwords = mock(PasswordEncoder.class);
        profileSync = mock(ProfileSyncService.class);
        roleAuthentication = mock(RoleAuthenticationClient.class);
        service = new AuthServiceImpl(users, passwords,
                new JwtService("12345678901234567890123456789012"), profileSync, roleAuthentication,
                new RegistrationIdentityService(users));
    }

    @Test
    void registerPersistsCustomerAndQueuesProfileSync() {
        RegisterRequest request = new RegisterRequest(" John ", " john ", "secret", "CUSTOMER",
                " john@example.com ", " 123 ", " Male ");
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "john@example.com")).thenReturn(false);
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "john")).thenReturn(false);
        when(passwords.encode("secret")).thenReturn("hashed");

        assertThat(service.register(request)).isEqualTo("User registered successfully");
        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("john");
        assertThat(userCaptor.getValue().getGender()).isEqualTo("Male");
        verify(profileSync).upsert(any(User.class));
    }

    @Test
    void registerRejectsPrivilegedRoleAndMissingRequiredFields() {
        assertStatus(() -> service.register(new RegisterRequest("Admin", "a", "secret", "ADMIN",
                "a@example.com", null, null)), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.register(new RegisterRequest(" ", "a", "secret", "CUSTOMER",
                "a@example.com", null, null)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(users, profileSync);
    }

    @Test
    void registerRejectsDuplicateEmailButGeneratesAvailableUsernameForTheSameRole() {
        RegisterRequest request = new RegisterRequest("John", "john", "secret", null,
                "john@example.com", null, "Others");
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "john@example.com")).thenReturn(true);
        assertStatus(() -> service.register(request), HttpStatus.CONFLICT);

        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "john@example.com")).thenReturn(false);
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "john")).thenReturn(true);
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "john1")).thenReturn(false);
        when(passwords.encode("secret")).thenReturn("hashed");

        assertThat(service.register(request)).isEqualTo("User registered successfully");
        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("john1");
    }

    @Test
    void registrationAllowsDetailsAvailableForTheCustomerRole() {
        RegisterRequest request = new RegisterRequest("John", "john", "secret", "CUSTOMER",
                "john@example.com", null, "Others");
        when(users.existsByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", "john@example.com")).thenReturn(false);
        when(users.existsByRoleIgnoreCaseAndUsernameIgnoreCase("CUSTOMER", "john")).thenReturn(false);
        when(passwords.encode("secret")).thenReturn("hashed");

        assertThat(service.register(request)).isEqualTo("User registered successfully");
        verify(users).saveAndFlush(any(User.class));
    }

    @Test
    void availableRolesReturnsSupportedRolesPresentInActiveProfiles() {
        when(users.findDistinctActiveRoles()).thenReturn(List.of("STAFF", "customer", "UNSUPPORTED", "ADMIN"));

        assertThat(service.availableRoles()).containsExactly(
                com.pddh.auth_service.security.UserRole.ADMIN,
                com.pddh.auth_service.security.UserRole.STAFF,
                com.pddh.auth_service.security.UserRole.CUSTOMER);
        verify(users).findDistinctActiveRoles();
    }

    @Test
    void loginUsesSelectedRoleStoreAndReturnsSignedToken() {
        when(roleAuthentication.authenticate("CUSTOMER", "john@example.com", "secret"))
                .thenReturn(Mono.just(new RoleAuthenticationResponse("john", "john@example.com", "CUSTOMER")));

        var response = service.login(new LoginRequest(" john@example.com ", "secret", " customer ")).block();

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isEqualTo("Logged in successfully");
        verify(roleAuthentication).authenticate("CUSTOMER", "john@example.com", "secret");
        verify(users).updateLastLoginAt(eq("john"), eq("CUSTOMER"), any());
    }

    @Test
    void loginRejectsMissingOrUnsupportedCredentialsBeforeCallingRoleStore() {
        assertStatus(() -> service.login(new LoginRequest(" ", "secret", "CUSTOMER")).block(),
                HttpStatus.UNAUTHORIZED);
        assertStatus(() -> service.login(new LoginRequest("john", "secret", "NOT_A_ROLE")).block(),
                HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(roleAuthentication, users);
    }

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(status);
    }
}
