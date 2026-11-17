package com.pddh.auth_service.dto;

public record LoginResponse(
		String accessToken, 
		String refreshToken
		) {}
