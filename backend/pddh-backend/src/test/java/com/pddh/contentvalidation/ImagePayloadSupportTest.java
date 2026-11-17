package com.pddh.contentvalidation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ImagePayloadSupportTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesUploadedImagesAndReturnsAnApiImagePath() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("imageUrl", "data:image/png;base64,iVBORw0KGgo=");

        byte[] image = ImagePayloadSupport.prepareForStorage(payload, "trips", "trip-id", null);

        assertArrayEquals(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}, image);
        assertEquals("/api/catalog/trips/trip-id/image", payload.get("imageUrl").asText());
        assertEquals("/api/catalog/trips/trip-id/image", ImagePayloadSupport.response(
                new LinkedHashMap<>(Map.of("imageUrl", "")), "trips", "trip-id", true).get("imageUrl"));
    }

    @Test
    void preservesAnExistingDatabaseImageWhenItsUrlIsSubmittedAgain() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("imageUrl", "https://api.example.test/api/catalog/hotels/stay-id/image");
        byte[] existingImage = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

        byte[] image = ImagePayloadSupport.prepareForStorage(payload, "stays", "stay-id", existingImage);

        assertArrayEquals(existingImage, image);
        assertEquals("/api/catalog/hotels/stay-id/image", payload.get("imageUrl").asText());
    }

    @Test
    void preservesAnExistingDatabaseImageWhenAnUpdateOmitsTheImageField() {
        ObjectNode payload = objectMapper.createObjectNode();
        byte[] existingImage = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

        byte[] image = ImagePayloadSupport.prepareForStorage(payload, "trips", "trip-id", existingImage);

        assertArrayEquals(existingImage, image);
        assertEquals("/api/catalog/trips/trip-id/image", payload.get("imageUrl").asText());
    }

    @Test
    void usesTheGallerySourcePropertyForUploadedImages() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("src", "data:image/png;base64,iVBORw0KGgo=");

        ImagePayloadSupport.prepareForStorage(payload, "gallery", "image-id", null);

        assertEquals("/api/catalog/gallery/image-id/image", payload.get("src").asText());
    }
}
