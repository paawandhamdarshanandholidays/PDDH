package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;

import com.pddh.auth_service.controller.AuthController;
import com.pddh.auth_service.dto.LoginRequest;
import com.pddh.auth_service.dto.LoginResponse;
import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.UsernameAvailabilityResponse;
import com.pddh.auth_service.service.AuthService;
import com.pddh.auth_service.service.CeoRegistrationService;
import com.pddh.auth_service.service.PasswordResetService;
import com.pddh.auth_service.security.CurrentUserService;
import com.pddh.auth_service.idempotency.IdempotencyRecord;
import com.pddh.auth_service.idempotency.IdempotencyRecordRepository;
import com.pddh.auth_service.idempotency.IdempotencyService;
import com.pddh.api_common.resilience.RateLimitGuard;
import com.pddh.api_common.dto.ApiMessageResponse;
import reactor.core.publisher.Mono;

class AuthControllerTest {

    @Test
    void auth_shouldReturnAuthString() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = controller(authService, mock(RateLimitGuard.class), idempotencyService());

        String result = controller.auth();

        assertThat(result).isEqualTo("auth");
    }

    @Test
    void availableRoles_shouldDelegateToService() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = controller(authService, mock(RateLimitGuard.class), idempotencyService());
        List<com.pddh.auth_service.security.UserRole> expected =
                Arrays.asList(com.pddh.auth_service.security.UserRole.ADMIN,
                        com.pddh.auth_service.security.UserRole.STAFF);
        when(authService.availableRoles()).thenReturn(expected);

        assertThat(controller.availableRoles()).containsExactlyElementsOf(expected);
        verify(authService).availableRoles();
    }

    @Test
    void register_shouldDelegateToService() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = controller(authService, mock(RateLimitGuard.class), idempotencyService());
        RegisterRequest request = new RegisterRequest(
                "John",
                "john123",
                "secret",
                "USER",
                "john@example.com",
                "9876543210",
                "MALE"
        );

        when(authService.register(request)).thenReturn("User registered successfully");

        ResponseEntity<ApiMessageResponse> response = controller.register(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new ApiMessageResponse("User registered successfully"));
        verify(authService).register(request);
    }

    @Test
    void registrationUsesNormalizedEmailAsItsIdempotencyIdentity() {
        AuthService authService = mock(AuthService.class);
        IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
        when(repository.findByKeyScopeAndOperationAndIdempotencyKey(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        AuthController controller = controller(authService, mock(RateLimitGuard.class), idempotencyService(repository));
        RegisterRequest first = new RegisterRequest(
                "John", "john123", "secret", "USER", "john@example.com", "9876543210", "MALE");
        RegisterRequest normalizedVariant = new RegisterRequest(
                "John", "john123", "secret", "USER", " JOHN@EXAMPLE.COM ", "9876543210", "MALE");
        when(authService.register(any())).thenReturn("User registered successfully");

        controller.register(first);
        controller.register(normalizedVariant);

        ArgumentCaptor<IdempotencyRecord> records = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(records.capture());
        List<IdempotencyRecord> saved = records.getAllValues();
        assertThat(saved.get(0).getIdempotencyKey()).hasSize(64)
                .isEqualTo(saved.get(1).getIdempotencyKey());
        assertThat(saved.get(0).getKeyScope()).isEqualTo(saved.get(1).getKeyScope());
        assertThat(saved.get(0).getRequestHash()).isEqualTo(saved.get(1).getRequestHash());
    }

    @Test
    void login_shouldDelegateToService() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = controller(authService, mock(RateLimitGuard.class), idempotencyService());
        LoginRequest request = new LoginRequest("john123", "secret", "USER");
        LoginResponse responseBody = new LoginResponse("jwt-token", null);

        when(authService.login(request)).thenReturn(Mono.just(responseBody));

        ResponseEntity<LoginResponse> response = controller.login(request).block();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseBody);
        verify(authService).login(request);
    }

    @Test
    void usernameAvailability_shouldDelegateRoleScopedCheck() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = controller(authService, mock(RateLimitGuard.class), idempotencyService());
        var expected = new UsernameAvailabilityResponse("john2", true, true, true);
        when(authService.checkUsernameAvailability(
                "john@example.com", com.pddh.auth_service.security.UserRole.CUSTOMER, "john", "123"))
                .thenReturn(expected);

        var response = controller.checkUsernameAvailability(
                "john@example.com", com.pddh.auth_service.security.UserRole.CUSTOMER, "john", "123");

        assertThat(response).isEqualTo(expected);
        verify(authService).checkUsernameAvailability(
                "john@example.com", com.pddh.auth_service.security.UserRole.CUSTOMER, "john", "123");
    }

    private IdempotencyService idempotencyService() {
        IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
        return idempotencyService(repository);
    }

    private AuthController controller(
            AuthService authService, RateLimitGuard rateLimitGuard, IdempotencyService idempotencyService) {
        return new AuthController(
                authService,
                rateLimitGuard,
                idempotencyService,
                mock(CeoRegistrationService.class),
                mock(CurrentUserService.class),
                mock(PasswordResetService.class));
    }

    private IdempotencyService idempotencyService(IdempotencyRecordRepository repository) {
        when(repository.findByKeyScopeAndOperationAndIdempotencyKey(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        return new IdempotencyService(repository, new ObjectMapper(), mock(JdbcTemplate.class), "test-scope-secret");
    }
}
