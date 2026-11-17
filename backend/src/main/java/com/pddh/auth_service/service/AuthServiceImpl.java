package com.pddh.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.List;

import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.UsernameAvailabilityResponse;
import com.pddh.auth_service.dto.LoginRequest;
import com.pddh.auth_service.dto.LoginResponse;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.JwtService;
import com.pddh.auth_service.security.UserRole;

@Service
public class AuthServiceImpl implements AuthService {
	
	private UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final ProfileSyncService profileSyncService;
	private final RoleAuthenticationClient roleAuthenticationClient;
	private final RegistrationIdentityService registrationIdentityService;
	@Autowired
	public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			ProfileSyncService profileSyncService, RoleAuthenticationClient roleAuthenticationClient,
			RegistrationIdentityService registrationIdentityService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.profileSyncService = profileSyncService;
		this.roleAuthenticationClient = roleAuthenticationClient;
		this.registrationIdentityService = registrationIdentityService;
	}
	
	@Override
	@Transactional
	public String register(RegisterRequest request) {

	    if (request != null && request.role() != null && !request.role().isBlank()
	            && !"CUSTOMER".equalsIgnoreCase(request.role().trim())) {
	        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
	                "Public registration is only for customer accounts. Admin and staff accounts must be created "
	                        + "through the authorized user-management endpoint.");
	    }
	    if (request == null || isBlank(request.name())
	            || isBlank(request.email()) || isBlank(request.password())) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
	                "Name, email, and password are required");
	    }
	    String email = request.email().trim();
	    String mobile = request.mobile() == null || request.mobile().isBlank() ? null : request.mobile().trim();
	    String username = registrationIdentityService.usernameFor(
	            email, request.username(), UserRole.CUSTOMER, null);
	    String gender = ProfileRegistrationValues.normalizeGender(request.gender());

	    registrationIdentityService.validateUnique(
	            UserRole.CUSTOMER, email, username, mobile, null);

	    User user = new User();
	    user.setName(request.name().trim());
	    user.setUsername(username);
	    user.setPassword(passwordEncoder.encode(request.password()));
	    user.setRole("CUSTOMER");
	    user.setEmail(email);
	    user.setMobile(mobile);
	    user.setGender(gender);
	    user.setActive(true);
	    userRepository.saveAndFlush(user);
	    profileSyncService.upsert(user);

	    return "User registered successfully";
	}

	@Override
	public UsernameAvailabilityResponse checkUsernameAvailability(
			String email, UserRole role, String username, String mobile) {
		return registrationIdentityService.checkAvailability(email, role, username, mobile);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserRole> availableRoles() {
		List<String> rolesInUse = userRepository.findDistinctActiveRoles();
		return Arrays.stream(UserRole.values())
				.filter(role -> rolesInUse.stream().anyMatch(role.name()::equalsIgnoreCase))
				.toList();
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		String identifier = request == null || request.username() == null ? "" : request.username().trim();
		String password = request == null || request.password() == null ? "" : request.password();
		String role = request == null || request.role() == null ? "" : request.role();
		if (identifier.isBlank() || password.isBlank() || role.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role or credentials");
		}
		try {
			role = UserRole.parse(role).name();
		} catch (ResponseStatusException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role or credentials");
		}
		var profile = roleAuthenticationClient.authenticate(role, identifier, password);
		userRepository.updateLastLoginAt(profile.username(), profile.role(), java.time.Instant.now());
		return new LoginResponse(
				jwtService.generateToken(profile.username(), profile.role(), profile.email()),
				"Logged in successfully");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
