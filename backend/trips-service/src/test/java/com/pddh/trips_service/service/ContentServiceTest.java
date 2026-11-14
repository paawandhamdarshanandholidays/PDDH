package com.pddh.trips_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.trips_service.entity.ContentItem;
import com.pddh.trips_service.repository.ContentItemRepository;

class ContentServiceTest {
    @Test
    void createReturnsAJsonObjectCompatibleWithTheHttpSerializer() throws Exception {
        ContentItemRepository repository = mock(ContentItemRepository.class);
        ContentService service = new ContentService(repository, new ObjectMapper(), "trips");

        Map<String, Object> response = service.create(Map.of(
                "title", "Temple journey",
                "destination", "Mathura",
                "durationDays", 3));

        assertEquals("Temple journey", response.get("title"));
        assertTrue(response.get("id") instanceof String);

        String serializedResponse = new tools.jackson.databind.ObjectMapper().writeValueAsString(response);
        assertTrue(serializedResponse.startsWith("{"));
        assertTrue(serializedResponse.contains("\"title\":\"Temple journey\""));
        assertFalse(serializedResponse.contains("\"nodeType\""));

        ArgumentCaptor<ContentItem> savedItem = ArgumentCaptor.forClass(ContentItem.class);
        verify(repository).saveAndFlush(savedItem.capture());
        assertTrue(savedItem.getValue().getPayload().contains("\"title\":\"Temple journey\""));
    }

    @Test
    void storesUploadedImageBytesSeparatelyFromThePublicContentPayload() {
        ContentItemRepository repository = mock(ContentItemRepository.class);
        ContentService service = new ContentService(repository, new ObjectMapper(), "trips");

        Map<String, Object> response = service.create(Map.of(
                "title", "Temple journey",
                "imageUrl", "data:image/png;base64,iVBORw0KGgo="));

        assertTrue(((String) response.get("imageUrl")).matches(
                "^/api/catalog/trips/[0-9a-f-]+/image$"));
        ArgumentCaptor<ContentItem> savedItem = ArgumentCaptor.forClass(ContentItem.class);
        verify(repository).saveAndFlush(savedItem.capture());
        assertTrue(savedItem.getValue().getImages().length > 0);
        assertFalse(savedItem.getValue().getPayload().contains("data:image/"));
    }
}
