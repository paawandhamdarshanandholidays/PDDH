package com.pddh.auth_service.service;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.UserRole;

@Service
public class CeoRegistrationService {
    private static final long REGISTRATION_LOCK_ID = 764321905L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ProfileSyncService profileSyncService;
    private final RegistrationIdentityService registrationIdentityService;

    public CeoRegistrationService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate,
            ProfileSyncService profileSyncService, RegistrationIdentityService registrationIdentityService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.profileSyncService = profileSyncService;
        this.registrationIdentityService = registrationIdentityService;
    }

    @Transactional
    public String register(User actor, RegisterRequest request) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> acquireRegistrationLock(connection));
        boolean ceoExists = userRepository.existsByRoleIgnoreCase(UserRole.CEO.name());
        if (ceoExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A CEO profile already exists");
        }
        if (request == null || blank(request.name()) || blank(request.email())
                || blank(request.password()) || request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Name, email, and a password of at least 8 characters are required");
        }
        String email = request.email().trim();
        String mobile = blankToNull(request.mobile());
        String username = registrationIdentityService.usernameFor(
                email, request.username(), UserRole.CEO, null);
        String gender = ProfileRegistrationValues.normalizeGender(request.gender());
        registrationIdentityService.validateUnique(UserRole.CEO, email, username, mobile, null);

        User ceo = new User();
        ceo.setName(request.name().trim());
        ceo.setUsername(username);
        ceo.setEmail(email);
        ceo.setPassword(passwordEncoder.encode(request.password()));
        ceo.setRole(UserRole.CEO.name());
        ceo.setMobile(mobile);
        ceo.setGender(gender);
        ceo.setActive(true);
        userRepository.saveAndFlush(ceo);
        profileSyncService.upsert(ceo);
        return "CEO account registered successfully";
    }

    private Void acquireRegistrationLock(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, REGISTRATION_LOCK_ID);
            statement.execute();
        }
        return null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }
}
