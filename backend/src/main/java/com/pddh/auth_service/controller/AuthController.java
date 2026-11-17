package com.pddh.auth_service.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pddh.auth_service.dto.PasswordResetOtpConfirmRequest;
import com.pddh.auth_service.dto.PasswordResetOtpRequest;
import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.LoginRequest;
import com.pddh.auth_service.dto.LoginResponse;
import com.pddh.auth_service.dto.UsernameAvailabilityResponse;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.security.CurrentUserService;
import com.pddh.auth_service.security.UserRole;
import com.pddh.auth_service.service.AuthService;
import com.pddh.auth_service.service.CeoRegistrationService;
import com.pddh.auth_service.service.PasswordResetService;
import com.pddh.api_common.resilience.RateLimitGuard;
import com.pddh.api_common.dto.ApiMessageResponse;
import com.pddh.auth_service.idempotency.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	static Logger logger=LoggerFactory.getLogger(AuthController.class);
	@GetMapping
	public String auth() {
		logger.debug("auth controller auth method called");
		return "auth";
	}

	@GetMapping("/roles")
	public List<UserRole> availableRoles() {
		return authService.availableRoles();
	}
	
	private final AuthService authService;
	private final RateLimitGuard rateLimitGuard;
	private final IdempotencyService idempotencyService;
	private final CeoRegistrationService ceoRegistrationService;
	private final CurrentUserService currentUserService;
	private final PasswordResetService passwordResetService;

	@Autowired
    public AuthController(
            AuthService authService,
            RateLimitGuard rateLimitGuard,
            IdempotencyService idempotencyService,
            CeoRegistrationService ceoRegistrationService,
            CurrentUserService currentUserService,
            PasswordResetService passwordResetService) {
        this.authService = authService;
        this.rateLimitGuard = rateLimitGuard;
        this.idempotencyService = idempotencyService;
        this.ceoRegistrationService = ceoRegistrationService;
        this.currentUserService = currentUserService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiMessageResponse> register(
            @RequestBody RegisterRequest request) {
        rateLimitGuard.check("auth.register");
        logger.debug("Register endpoint called");
        String scope = request.email() == null ? "public" : request.email().trim().toLowerCase(Locale.ROOT);
        ApiMessageResponse response = idempotencyService.execute(
                scope, "auth.register", emailIdempotencyKey(scope), fingerprintRequest(request),
                ApiMessageResponse.class,
                () -> new ApiMessageResponse(authService.register(request)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username-availability")
    public UsernameAvailabilityResponse checkUsernameAvailability(
            @RequestParam String email,
            @RequestParam UserRole role,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String mobile) {
        rateLimitGuard.check("auth.username-availability");
        return authService.checkUsernameAvailability(email, role, username, mobile);
    }

    @PostMapping(value = "/register-ceo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiMessageResponse> registerCeo(
            @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        rateLimitGuard.check("auth.register-ceo");
        String authorization = servletRequest.getHeader("Authorization");
        User actor = authorization == null ? null : currentUserService.requireUser(servletRequest);
        return ResponseEntity.status(201)
                .body(new ApiMessageResponse(ceoRegistrationService.register(actor, request)));
    }

    @PostMapping(value = "/password-reset/otp/request", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiMessageResponse> requestCustomerPasswordResetOtp(
            @RequestBody PasswordResetOtpRequest request) {
        rateLimitGuard.check("auth.password-reset.otp-request");
        return ResponseEntity.ok(new ApiMessageResponse(passwordResetService.requestCustomerOtp(request)));
    }

    @PostMapping(value = "/password-reset/otp/confirm", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiMessageResponse> confirmCustomerPasswordResetOtp(
            @RequestBody PasswordResetOtpConfirmRequest request) {
        rateLimitGuard.check("auth.password-reset.otp-confirm");
        return ResponseEntity.ok(new ApiMessageResponse(passwordResetService.confirmCustomerOtp(request)));
    }

    private RegisterRequest fingerprintRequest(RegisterRequest request) {
        if (request == null || request.email() == null) {
            return request;
        }
        return new RegisterRequest(request.name(), request.username(), request.password(), request.role(),
                request.email().trim().toLowerCase(Locale.ROOT), request.mobile(), request.gender());
    }

    private String emailIdempotencyKey(String normalizedEmail) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(normalizedEmail.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        rateLimitGuard.check("auth.login");
        return ResponseEntity.ok(authService.login(request));
    }
	
	

}
