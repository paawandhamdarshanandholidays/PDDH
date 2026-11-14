package com.pddh.auth_service.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ProfileRegistrationValues {
    private ProfileRegistrationValues() { }

    static String usernameFromEmail(String email) {
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email address is required");
        }
        String normalizedEmail = email.trim();
        int separator = normalizedEmail.indexOf('@');
        if (separator < 1 || separator == normalizedEmail.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email address is required");
        }
        return normalizedEmail.substring(0, separator);
    }

    static String normalizeGender(String gender) {
        if (gender == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select Male, Female, or Others");
        }
        return switch (gender.trim().toLowerCase(Locale.ROOT)) {
            case "male" -> "Male";
            case "female" -> "Female";
            case "others" -> "Others";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select Male, Female, or Others");
        };
    }
}
