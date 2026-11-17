package com.pddh.auth_service.service;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.PasswordResetApprovalRequest;
import com.pddh.auth_service.dto.PasswordChangeRequest;
import com.pddh.auth_service.dto.PasswordResetOtpConfirmRequest;
import com.pddh.auth_service.dto.PasswordResetOtpRequest;
import com.pddh.auth_service.dto.PasswordResetResponse;
import com.pddh.auth_service.dto.UserProfileResponse;
import com.pddh.auth_service.entities.PasswordResetOtp;
import com.pddh.auth_service.entities.PasswordResetRequest;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.PasswordResetOtpRepository;
import com.pddh.auth_service.repository.PasswordResetRequestRepository;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.UserRole;

@Service
public class PasswordResetService {
    private static final String PENDING = "PENDING";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordResetRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final ProfileSyncService profileSyncService;
    private final String smtpHost;
    private final String senderAddress;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            PasswordResetRequestRepository requestRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            ProfileSyncService profileSyncService,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.mail.from:}") String senderAddress) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.profileSyncService = profileSyncService;
        this.smtpHost = smtpHost;
        this.senderAddress = senderAddress;
    }

    @Transactional
    public String requestCustomerOtp(PasswordResetOtpRequest request) {
        requireMailConfiguration();
        if (request == null || blank(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByRoleIgnoreCaseAndEmailIgnoreCase(UserRole.CUSTOMER.name(), email).orElse(null);
        if (user == null || !UserRole.CUSTOMER.name().equalsIgnoreCase(user.getRole()) || !user.isActive()) {
            return "If an active customer account matches that email, a reset code has been sent";
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        otpRepository.findAllByEmailIgnoreCaseAndConsumedFalse(email).forEach(otp -> otp.setConsumed(true));
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(email);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(Instant.now());
        otp.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        otpRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(email);
        message.setSubject("Your password reset code");
        message.setText("Your password reset code is " + code + ". It expires in 10 minutes.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password reset email could not be sent. Please try again later.", exception);
        }
        return "If an active customer account matches that email, a reset code has been sent";
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String confirmCustomerOtp(PasswordResetOtpConfirmRequest request) {
        if (request == null || blank(request.email()) || blank(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and reset code are required");
        }
        validateNewPassword(request.newPassword());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByRoleIgnoreCaseAndEmailIgnoreCase(UserRole.CUSTOMER.name(), email)
                .filter(candidate -> candidate.isActive()
                        && UserRole.CUSTOMER.name().equalsIgnoreCase(candidate.getRole()))
                .orElseThrow(() -> invalidResetCode());
        PasswordResetOtp otp = otpRepository.findActiveForUpdate(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> invalidResetCode());
        if (!otp.getExpiresAt().isAfter(Instant.now()) || otp.getFailedAttempts() >= 5) {
            throw invalidResetCode();
        }
        if (!passwordEncoder.matches(request.code(), otp.getCodeHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            if (otp.getFailedAttempts() >= 5) {
                otp.setConsumed(true);
            }
            otpRepository.saveAndFlush(otp);
            throw invalidResetCode();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
        profileSyncService.upsert(user);
        otp.setConsumed(true);
        otpRepository.save(otp);
        return "Password reset successfully";
    }

    @Transactional
    public String changeOwnCustomerPassword(User actor, PasswordChangeRequest request) {
        if (!UserRole.CUSTOMER.name().equalsIgnoreCase(actor.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Staff and administrators must request CEO approval to reset their passwords");
        }
        if (request == null || blank(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
        }
        validateNewPassword(request.newPassword());
        String storedPassword = actor.getPassword();
        boolean matches = storedPassword != null && (storedPassword.startsWith("$2")
                ? passwordEncoder.matches(request.currentPassword(), storedPassword)
                : MessageDigest.isEqual(
                        request.currentPassword().getBytes(StandardCharsets.UTF_8),
                        storedPassword.getBytes(StandardCharsets.UTF_8)));
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        actor.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(actor);
        profileSyncService.upsert(actor);
        invalidateCustomerOtps(actor);
        return "Password changed successfully";
    }

    @Transactional
    public PasswordResetResponse requestRolePasswordReset(User actor, PasswordResetApprovalRequest request) {
        UserRole actorRole = parseRole(actor.getRole());
        if (actorRole == UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers must use customer password recovery");
        }
        validateNewPassword(request == null ? null : request.newPassword());
        User target = request == null || request.targetUserId() == null
                ? actor
                : userRepository.findById(request.targetUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserRole targetRole = parseRole(target.getRole());
        boolean elevatedActor = actorRole == UserRole.ADMIN || actorRole == UserRole.CEO;
        if (!target.getId().equals(actor.getId()) && !elevatedActor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an admin or CEO can request a password reset for another account");
        }
        if (targetRole == UserRole.CUSTOMER && !elevatedActor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot reset this account");
        }
        if (!target.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inactive accounts cannot reset passwords");
        }
        if (requestRepository.existsByTargetUserIdAndStatus(target.getId(), PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A password reset request is already awaiting CEO approval for this account");
        }

        PasswordResetRequest reset = new PasswordResetRequest();
        reset.setTargetUser(target);
        reset.setRequester(actor);
        reset.setNewPasswordHash(passwordEncoder.encode(request.newPassword()));
        reset.setRequestedAt(Instant.now());
        reset.setStatus(PENDING);
        try {
            return toResponse(requestRepository.saveAndFlush(reset));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A password reset request is already awaiting CEO approval for this account", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listPasswordResetTargets(User actor) {
        UserRole actorRole = parseRole(actor.getRole());
        if (actorRole != UserRole.ADMIN && actorRole != UserRole.CEO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an admin or CEO can select another account for password reset");
        }
        return userRepository.findAllProjectedBy().stream()
                .map(user -> new UserProfileResponse(
                        user.getId().toString(),
                        user.getName(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getMobile(),
                        user.getRole(),
                        user.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PasswordResetResponse> listPendingPasswordResets(User actor) {
        requireCeo(actor);
        return requestRepository.findAllByStatusOrderByRequestedAtAsc(PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PasswordResetResponse reviewPasswordReset(User actor, Long requestId, boolean approve) {
        requireCeo(actor);
        PasswordResetRequest reset = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Password reset request not found"));
        if (!PENDING.equals(reset.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This password reset request was already reviewed");
        }
        if (reset.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot approve your own reset request");
        }
        if (approve) {
            User target = reset.getTargetUser();
            if (!target.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inactive accounts cannot have their password reset");
            }
            target.setPassword(reset.getNewPasswordHash());
            userRepository.saveAndFlush(target);
            profileSyncService.upsert(target);
            if (UserRole.CUSTOMER.name().equalsIgnoreCase(target.getRole())) {
                invalidateCustomerOtps(target);
            }
            reset.setStatus("APPROVED");
        } else {
            reset.setStatus("REJECTED");
        }
        reset.setReviewer(actor);
        reset.setReviewedAt(Instant.now());
        reset.setNewPasswordHash("DISCARDED");
        return toResponse(requestRepository.saveAndFlush(reset));
    }

    private void requireMailConfiguration() {
        if (blank(smtpHost) || blank(senderAddress)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Customer email recovery is unavailable until SMTP_HOST and SMTP_FROM are configured");
        }
    }

    private void requireCeo(User actor) {
        if (parseRole(actor.getRole()) != UserRole.CEO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a CEO can review reset requests");
        }
    }

    private UserRole parseRole(String role) {
        return UserRole.parse(role);
    }

    private void validateNewPassword(String password) {
        if (blank(password) || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A password of at least 8 characters is required");
        }
    }

    private ResponseStatusException invalidResetCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "The reset code is invalid or expired");
    }

    private void invalidateCustomerOtps(User user) {
        otpRepository.findAllByEmailIgnoreCaseAndConsumedFalse(user.getEmail())
                .forEach(otp -> otp.setConsumed(true));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private PasswordResetResponse toResponse(PasswordResetRequest request) {
        User target = request.getTargetUser();
        return new PasswordResetResponse(
                request.getId().toString(),
                target.getId().toString(),
                target.getName(),
                target.getUsername(),
                request.getRequester().getName(),
                request.getRequestedAt(),
                request.getStatus());
    }
}
