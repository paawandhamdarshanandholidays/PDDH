package com.pddh.gallery_service.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GalleryMediaTypeValidationTest {
    @Test
    void acceptsVideoLinksAndLegacyImagePayloads() {
        assertDoesNotThrow(() -> ContentController.validateGalleryMediaType(Map.of(
                "mediaType", "VIDEO",
                "src", "https://youtu.be/aqz-KE-bpKQ")));
        assertDoesNotThrow(() -> ContentController.validateGalleryMediaType(Map.of(
                "src", "https://images.example.test/photo.jpg")));
    }

    @Test
    void rejectsUnsupportedTypesAndInlineVideoData() {
        assertThrows(ResponseStatusException.class, () -> ContentController.validateGalleryMediaType(Map.of(
                "mediaType", "AUDIO",
                "src", "https://example.test/clip.mp4")));
        assertThrows(ResponseStatusException.class, () -> ContentController.validateGalleryMediaType(Map.of(
                "mediaType", "VIDEO",
                "src", "data:image/png;base64,iVBORw0KGgo=")));
    }
}
