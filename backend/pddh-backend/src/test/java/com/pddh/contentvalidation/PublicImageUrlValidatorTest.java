package com.pddh.contentvalidation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class PublicImageUrlValidatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsHttpAndHttpsImageUrls() throws Exception {
        assertDoesNotThrow(() -> PublicImageUrlValidator.validate(
                objectMapper.readTree("""
                        {"imageUrl":"https://images.example.org/photo.jpg","src":"http://cdn.example.org/photo.png"}
                        """)));
    }

    @Test
    void rejectsDataUrisAndMalformedImageValues() throws Exception {
        assertThrows(ResponseStatusException.class, () -> PublicImageUrlValidator.validate(
                objectMapper.readTree("""
                        {"imageUrl":"data:image/png;base64,AAAA"}
                        """)));
        assertThrows(ResponseStatusException.class, () -> PublicImageUrlValidator.validate(
                objectMapper.readTree("""
                        {"imageUrl":"javascript:alert(1)"}
                        """)));
        assertThrows(ResponseStatusException.class, () -> PublicImageUrlValidator.validate(
                objectMapper.readTree("""
                        {"src":""}
                        """)));
    }

    @Test
    void acceptsValidBase64ImageUploads() throws Exception {
        assertDoesNotThrow(() -> PublicImageUrlValidator.validate(objectMapper.readTree("""
                {"imageUrl":"data:image/png;base64,iVBORw0KGgo="}
                """)));
    }

    @Test
    void validatesImageUrlsInMapRequestBodies() {
        assertDoesNotThrow(() -> PublicImageUrlValidator.validate(Map.of(
                "images", List.of("https://images.example.org/photo.jpg"),
                "details", Map.of("image_url", "http://cdn.example.org/photo.png"))));

        assertThrows(ResponseStatusException.class, () -> PublicImageUrlValidator.validate(Map.of(
                "images", List.of("data:image/png;base64,AAAA"))));
    }
}
