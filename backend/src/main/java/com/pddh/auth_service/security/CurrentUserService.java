package com.pddh.auth_service.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class CurrentUserService {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CurrentUserService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public User requireUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        try {
            var claims = jwtService.parseClaims(authorization.substring(7));
            String username = claims.getSubject();
            String tokenRole = claims.get("role", String.class);
            User user = userRepository.findByUsernameIgnoreCaseAndRoleIgnoreCase(username, tokenRole)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "The authenticated account no longer exists"));
            if (!user.isActive() || tokenRole == null || !user.getRole().equalsIgnoreCase(tokenRole)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "The authenticated account is inactive or its role has changed");
            }
            return user;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }

    public UserRole requireRole(HttpServletRequest request) {
        UserRole role = UserRole.parse(requireUser(request).getRole());
        return role;
    }

    public String requireUsername(HttpServletRequest request) {
        return requireUser(request).getUsername();
    }
}
