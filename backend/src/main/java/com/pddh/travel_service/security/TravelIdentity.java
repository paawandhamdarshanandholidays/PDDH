package com.pddh.travel_service.security;

public record TravelIdentity(String username, String email, String role) {
    public boolean canReviewBookings() {
        return "CEO".equals(role) || "ADMIN".equals(role) || "MANAGER".equals(role) || "STAFF".equals(role);
    }
}
