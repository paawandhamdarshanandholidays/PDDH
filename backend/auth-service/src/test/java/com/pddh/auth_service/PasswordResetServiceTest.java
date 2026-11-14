package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.PasswordChangeRequest;
import com.pddh.auth_service.dto.PasswordResetApprovalRequest;
import com.pddh.auth_service.dto.PasswordResetOtpConfirmRequest;
import com.pddh.auth_service.dto.PasswordResetOtpRequest;
import com.pddh.auth_service.dto.UserProfileResponse;
import com.pddh.auth_service.entities.PasswordResetOtp;
import com.pddh.auth_service.entities.PasswordResetRequest;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.PasswordResetOtpRepository;
import com.pddh.auth_service.repository.PasswordResetRequestRepository;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.service.PasswordResetService;
import com.pddh.auth_service.service.ProfileSyncService;

class PasswordResetServiceTest {
    private UserRepository userRepository;
    private PasswordResetOtpRepository otpRepository;
    private PasswordResetRequestRepository requestRepository;
    private PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpRepository = mock(PasswordResetOtpRepository.class);
        requestRepository = mock(PasswordResetRequestRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        mailSender = mock(JavaMailSender.class);
        service = new PasswordResetService(
                userRepository, otpRepository, requestRepository, passwordEncoder, mailSender,
                mock(ProfileSyncService.class),
                "smtp.example.com", "noreply@example.com");
    }

    @Test
    void adminCanListPasswordResetTargetsUsingLightweightProjections() {
        User actor = user(1L, "ADMIN", "password");
        UserRepository.PasswordResetTarget target = mock(UserRepository.PasswordResetTarget.class);
        when(target.getId()).thenReturn(2L);
        when(target.getName()).thenReturn("Customer user");
        when(target.getUsername()).thenReturn("customer");
        when(target.getEmail()).thenReturn("customer@example.com");
        when(target.getMobile()).thenReturn("1234567890");
        when(target.getRole()).thenReturn("CUSTOMER");
        when(target.isActive()).thenReturn(true);
        when(userRepository.findAllProjectedBy()).thenReturn(List.of(target));

        List<UserProfileResponse> results = service.listPasswordResetTargets(actor);

        assertThat(results).containsExactly(new UserProfileResponse(
                "2", "Customer user", "customer", "customer@example.com", "1234567890", "CUSTOMER", true));
        verify(userRepository).findAllProjectedBy();
    }

    @Test
    void customerCanChangePasswordWithCurrentPassword() {
        User customer = user(1L, "CUSTOMER", "old-password");

        String response = service.changeOwnCustomerPassword(
                customer, new PasswordChangeRequest("old-password", "new-password"));

        assertThat(response).isEqualTo("Password changed successfully");
        assertThat(passwordEncoder.matches("new-password", customer.getPassword())).isTrue();
        verify(userRepository).saveAndFlush(customer);
    }

    @Test
    void customerPasswordChangeRejectsAnIncorrectCurrentPassword() {
        User customer = user(1L, "CUSTOMER", "old-password");

        assertThatThrownBy(() -> service.changeOwnCustomerPassword(
                customer, new PasswordChangeRequest("wrong-password", "new-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void customerCanResetPasswordUsingAnEmailCode() {
        User customer = user(1L, "CUSTOMER", "old-password");
        when(userRepository.findByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", customer.getEmail()))
                .thenReturn(Optional.of(customer));
        when(otpRepository.findAllByEmailIgnoreCaseAndConsumedFalse(customer.getEmail())).thenReturn(List.of());
        service.requestCustomerOtp(new PasswordResetOtpRequest(customer.getEmail()));

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        String code = mail.getValue().getText().split(" ")[5].replace(".", "");
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(customer.getEmail());
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(Instant.now());
        otp.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(otpRepository.findActiveForUpdate(customer.getEmail())).thenReturn(List.of(otp));

        String response = service.confirmCustomerOtp(
                new PasswordResetOtpConfirmRequest(customer.getEmail(), code, "reset-password"));

        assertThat(response).isEqualTo("Password reset successfully");
        assertThat(passwordEncoder.matches("reset-password", customer.getPassword())).isTrue();
        assertThat(otp.isConsumed()).isTrue();
    }

    @Test
    void incorrectOtpIsPersistedAndEventuallyConsumed() {
        User customer = user(1L, "CUSTOMER", "old-password");
        when(userRepository.findByRoleIgnoreCaseAndEmailIgnoreCase("CUSTOMER", customer.getEmail()))
                .thenReturn(Optional.of(customer));
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(customer.getEmail());
        otp.setCodeHash(passwordEncoder.encode("123456"));
        otp.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        otp.setFailedAttempts(4);
        when(otpRepository.findActiveForUpdate(customer.getEmail())).thenReturn(List.of(otp));

        assertThatThrownBy(() -> service.confirmCustomerOtp(
                new PasswordResetOtpConfirmRequest(customer.getEmail(), "000000", "reset-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(otp.getFailedAttempts()).isEqualTo(5);
        assertThat(otp.isConsumed()).isTrue();
        verify(otpRepository).saveAndFlush(otp);
    }

    @Test
    void privilegedPasswordResetRequiresCeoApprovalBeforeChangingPassword() {
        User admin = user(1L, "ADMIN", "old-password");
        when(requestRepository.existsByTargetUserIdAndStatus(1L, "PENDING")).thenReturn(false);
        when(requestRepository.saveAndFlush(any(PasswordResetRequest.class)))
                .thenAnswer(invocation -> {
                    PasswordResetRequest request = invocation.getArgument(0);
                    request.setId(8L);
                    return request;
                });

        var response = service.requestRolePasswordReset(admin, new PasswordResetApprovalRequest(null, "new-password"));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(passwordEncoder.matches("new-password", admin.getPassword())).isFalse();
        assertThat(response.id()).isEqualTo("8");
    }

    @Test
    void onlyAnotherCeoCanApproveAPendingReset() {
        User requester = user(1L, "ADMIN", "admin-password");
        User target = user(3L, "STAFF", "old-password");
        User ceo = user(2L, "CEO", "ceo-password");
        PasswordResetRequest reset = new PasswordResetRequest();
        reset.setId(10L);
        reset.setRequester(requester);
        reset.setTargetUser(target);
        reset.setStatus("PENDING");
        reset.setNewPasswordHash(passwordEncoder.encode("approved-password"));
        reset.setRequestedAt(Instant.now());
        when(requestRepository.findById(10L)).thenReturn(Optional.of(reset));
        when(requestRepository.saveAndFlush(reset)).thenReturn(reset);

        var response = service.reviewPasswordReset(ceo, 10L, true);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(passwordEncoder.matches("approved-password", target.getPassword())).isTrue();
        assertThat(reset.getNewPasswordHash()).isEqualTo("DISCARDED");
    }

    private User user(Long id, String role, String rawPassword) {
        User user = new User();
        user.setId(id);
        user.setName(role + " user");
        user.setUsername(role.toLowerCase() + id);
        user.setEmail(role.toLowerCase() + id + "@example.com");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
