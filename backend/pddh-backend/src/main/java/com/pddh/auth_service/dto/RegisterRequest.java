package com.pddh.auth_service.dto;

public record RegisterRequest(
		String name,
		String username,
		String password,
		String role,
		String email,
		String mobile,
		String gender
		) {

}
