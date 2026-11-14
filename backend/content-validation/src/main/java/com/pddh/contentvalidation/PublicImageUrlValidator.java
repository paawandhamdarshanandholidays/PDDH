package com.pddh.contentvalidation;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

public final class PublicImageUrlValidator {
    private PublicImageUrlValidator() {
    }

    public static void validate(JsonNode content) {
        validateNode(content, "");
    }

    public static void validate(Map<String, ?> content) {
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must be a JSON object");
        }
        validateValue(content, "");
    }

    private static void validateValue(Object value, String fieldName) {
        if (isImageField(fieldName)) {
            if (value instanceof List<?> values) {
                values.forEach(PublicImageUrlValidator::validateImageValue);
            } else {
                validateImageValue(value);
            }
            return;
        }
        if (value instanceof Map<?, ?> fields) {
            fields.forEach((name, fieldValue) -> validateValue(fieldValue, String.valueOf(name)));
        } else if (value instanceof List<?> values) {
            values.forEach(item -> validateValue(item, ""));
        }
    }

    private static void validateNode(JsonNode node, String fieldName) {
        if (isImageField(fieldName)) {
            if (node.isArray()) {
                node.forEach(PublicImageUrlValidator::validateImageValue);
            } else {
                validateImageValue(node);
            }
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                validateNode(field.getValue(), field.getKey());
            }
        } else if (node.isArray()) {
            node.forEach(value -> validateNode(value, ""));
        }
    }

    private static boolean isImageField(String name) {
        String normalized = name.replace("_", "").replace("-", "").toLowerCase();
        return normalized.equals("src") || normalized.contains("image");
    }

    private static void validateImageValue(JsonNode value) {
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > 4096) {
            throw invalidImageUrl();
        }
        validateImageUrl(value.asText());
    }

    private static void validateImageValue(Object value) {
        if (!(value instanceof String imageUrl) || imageUrl.isBlank() || imageUrl.length() > 4096) {
            throw invalidImageUrl();
        }
        validateImageUrl(imageUrl);
    }

    private static void validateImageUrl(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
                throw invalidImageUrl();
            }
        } catch (IllegalArgumentException exception) {
            throw invalidImageUrl();
        }
    }

    private static ResponseStatusException invalidImageUrl() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Image values must be public HTTP or HTTPS URLs.");
    }
}
