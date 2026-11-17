package com.pddh.auth_service.service;

import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.LoginRequest;
import com.pddh.auth_service.dto.LoginResponse;
import com.pddh.auth_service.dto.UsernameAvailabilityResponse;
import com.pddh.auth_service.security.UserRole;
import java.util.List;

public interface AuthService {
	
	 public String register(RegisterRequest request);

	 UsernameAvailabilityResponse checkUsernameAvailability(
			 String email, UserRole role, String username, String mobile);

	 List<UserRole> availableRoles();

	 LoginResponse login(LoginRequest request);

}
