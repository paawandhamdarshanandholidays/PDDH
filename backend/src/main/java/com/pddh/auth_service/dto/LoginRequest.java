package com.pddh.auth_service.dto;

public record LoginRequest(
		String username,
		String password,
		String role
		) {}
