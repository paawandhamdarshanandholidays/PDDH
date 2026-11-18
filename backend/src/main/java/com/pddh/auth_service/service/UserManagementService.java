package com.pddh.auth_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.UserProfilePageResponse;
import com.pddh.auth_service.dto.UserProfileResponse;
import com.pddh.auth_service.dto.UserProfileUpdateRequest;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.PasswordResetOtpRepository;
import com.pddh.auth_service.repository.PasswordResetRequestRepository;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.UserRole;

@Service
public class UserManagementService {
    private static final long CEO_REGISTRATION_LOCK_ID = 764321905L;

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ProfileSyncService profileSyncService;
    private final RegistrationIdentityService registrationIdentityService;

    public UserManagementService(
            UserRepository userRepository,
            PasswordResetRequestRepository passwordResetRequestRepository,
            PasswordResetOtpRepository passwordResetOtpRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            ProfileSyncService profileSyncService,
            RegistrationIdentityService registrationIdentityService) {
        this.userRepository = userRepository;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.profileSyncService = profileSyncService;
        this.registrationIdentityService = registrationIdentityService;
    }

    @Transactional(readOnly = true)
    public UserProfilePageResponse listManagedUsers(
            UserRole actorRole, UserRole targetRole, int page, int requestedSize) {
        if (page < 0 || requestedSize < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size positive");
        }
        if (!canViewRole(actorRole, targetRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view profiles with this role");
        }
        int size = Math.min(requestedSize, 50);
        Page<User> users = userRepository.findAllByRoleIgnoreCase(
                targetRole.name(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new UserProfilePageResponse(
                users.getContent().stream().map(this::toProfile).toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages());
    }

    @Transactional
    public UserProfileResponse createUser(UserRole actorRole, RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User details are required");
        }
        UserRole targetRole = UserRole.parse(request.role());
        if (!actorRole.canCreate(targetRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot create a user with this role");
        }
        validateRequired(request);
        if (targetRole == UserRole.CEO) {
            acquireCeoRegistrationLock();
            rejectDuplicateCeo(null);
        }
        String email = request.email().trim();
        String mobile = blankToNull(request.mobile());
        String username = registrationIdentityService.usernameFor(
                email, request.username(), targetRole, null);
        String gender = ProfileRegistrationValues.normalizeGender(request.gender());
        registrationIdentityService.validateUnique(targetRole, email, username, mobile, null);

        User user = new User();
        user.setName(request.name().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(targetRole.name());
        user.setMobile(mobile);
        user.setGender(gender);
        user.setActive(true);
        User saved = userRepository.saveAndFlush(user);
        profileSyncService.upsert(saved);
        return toProfile(saved);
    }

    @Transactional
    public UserProfileResponse updateUser(UserRole actorRole, Long id, UserProfileUpdateRequest request) {
        User user = findManagedUser(actorRole, id);
        String previousRole = user.getRole();
        if (request == null || blank(request.name()) || blank(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required");
        }
        String email = request.email().trim();
        UserRole targetRole = request.role() == null || request.role().isBlank()
                ? parseStoredRole(user.getRole())
                : UserRole.parse(request.role());
        if (!actorRole.canManage(targetRole) && targetRole != parseStoredRole(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot assign a role at or above your own");
        }
        String mobile = blankToNull(request.mobile());
        String username = registrationIdentityService.usernameFor(
                email, ProfileRegistrationValues.usernameFromEmail(email), targetRole, user.getId());
        if (targetRole == UserRole.CEO) {
            acquireCeoRegistrationLock();
            rejectDuplicateCeo(user.getId());
        }
        registrationIdentityService.validateUnique(targetRole, email, username, mobile, user.getId());
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(targetRole.name());
        }
        user.setName(request.name().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setMobile(mobile);
        User saved = userRepository.saveAndFlush(user);
        if (!previousRole.equalsIgnoreCase(saved.getRole())) {
            User oldRoleSnapshot = new User();
            oldRoleSnapshot.setId(saved.getId());
            oldRoleSnapshot.setRole(previousRole);
            oldRoleSnapshot.setName(saved.getName());
            oldRoleSnapshot.setUsername(saved.getUsername());
            oldRoleSnapshot.setEmail(saved.getEmail());
            oldRoleSnapshot.setMobile(saved.getMobile());
            oldRoleSnapshot.setGender(saved.getGender());
            oldRoleSnapshot.setActive(false);
            profileSyncService.delete(oldRoleSnapshot);
        }
        profileSyncService.upsert(saved);
        return toProfile(saved);
    }

    @Transactional
    public UserProfileResponse setUserActive(UserRole actorRole, Long id, boolean active) {
        User user = findManagedUser(actorRole, id);
        user.setActive(active);
        User saved = userRepository.saveAndFlush(user);
        profileSyncService.upsert(saved);
        return toProfile(saved);
    }

    @Transactional
    public void deleteUser(UserRole actorRole, Long id) {
        User user = findManagedUser(actorRole, id);
        passwordResetRequestRepository.deleteAllReferencingUser(user.getId());
        passwordResetOtpRepository.deleteAllByEmailIgnoreCase(user.getEmail());
        profileSyncService.delete(user);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse updateOwnProfile(User user, UserProfileUpdateRequest request) {
        if (request == null || blank(request.name()) || blank(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required");
        }
        String email = request.email().trim();
        String mobile = blankToNull(request.mobile());
        UserRole role = parseStoredRole(user.getRole());
        registrationIdentityService.validateUnique(role, email, user.getUsername(), mobile, user.getId());
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setMobile(mobile);
        User saved = userRepository.saveAndFlush(user);
        profileSyncService.upsert(saved);
        return toProfile(saved);
    }

    private User findManagedUser(UserRole actorRole, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!actorRole.canManage(parseStoredRole(user.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this user");
        }
        return user;
    }

    private boolean canViewRole(UserRole actorRole, UserRole targetRole) {
        return actorRole.canManage(targetRole)
                || actorRole == targetRole
                || actorRole == UserRole.ADMIN && targetRole == UserRole.ADMIN;
    }

    private void acquireCeoRegistrationLock() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, CEO_REGISTRATION_LOCK_ID);
                statement.execute();
            }
            return null;
        });
    }

    private void rejectDuplicateCeo(Long exceptUserId) {
        boolean ceoExists = exceptUserId == null
                ? userRepository.existsByRoleIgnoreCase(UserRole.CEO.name())
                : userRepository.existsByRoleIgnoreCaseAndIdNot(UserRole.CEO.name(), exceptUserId);
        if (ceoExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A CEO profile already exists");
        }
    }

    private UserRole parseStoredRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account has an unsupported role");
        }
    }

    private void validateRequired(RegisterRequest request) {
        if (blank(request.name()) || blank(request.email())
                || blank(request.password()) || request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Name, email, and a password of at least 8 characters are required");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private UserProfileResponse toProfile(User user) {
        UserRole role;
        try {
            role = UserRole.valueOf(user.getRole().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This profile has no valid role assigned");
        }
        return new UserProfileResponse(
                user.getId().toString(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getMobile(),
                role.name(),
                user.isActive(),
                user.getLastLoginAt());
    }
}
