package com.pddh.auth_service.security;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class CurrentUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserService.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CurrentUserService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public User requireUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            logRejection(request, "missing_bearer_token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        String username;
        String tokenRole;
        try {
            var claims = jwtService.parseClaims(authorization.substring(7));
            username = claims.getSubject();
            tokenRole = claims.get("role", String.class);
        } catch (ExpiredJwtException exception) {
            logRejection(request, "expired_token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        } catch (JwtException | IllegalArgumentException exception) {
            logRejection(request, "invalid_token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
        if (username == null || username.isBlank() || tokenRole == null || tokenRole.isBlank()) {
            logRejection(request, "missing_identity_claims");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The access token has incomplete identity claims");
        }
        User user = userRepository.findByUsernameIgnoreCaseAndRoleIgnoreCase(username, tokenRole)
                .orElseThrow(() -> {
                    logRejection(request, "account_missing_or_role_changed");
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "The authenticated account no longer exists or its role has changed");
                });
        if (!user.isActive()) {
            logRejection(request, "account_inactive");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The authenticated account is inactive");
        }
        if (!user.getRole().equalsIgnoreCase(tokenRole)) {
            logRejection(request, "account_role_changed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "The authenticated account role has changed");
        }
        return user;
    }

    public UserRole requireRole(HttpServletRequest request) {
        UserRole role = UserRole.parse(requireUser(request).getRole());
        return role;
    }

    public String requireUsername(HttpServletRequest request) {
        return requireUser(request).getUsername();
    }

    private void logRejection(HttpServletRequest request, String reason) {
        LOGGER.warn("Current-user authentication rejected method={} path={} reason={}",
                request.getMethod(), request.getRequestURI(), reason);
    }
}
