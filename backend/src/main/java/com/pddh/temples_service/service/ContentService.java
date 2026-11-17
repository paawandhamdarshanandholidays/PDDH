package com.pddh.temples_service.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.contentvalidation.DatabaseImage;
import com.pddh.contentvalidation.ImagePayloadSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pddh.temples_service.entity.ContentItem;
import com.pddh.temples_service.repository.ContentItemRepository;

@Service
public class ContentService {
    private final ContentItemRepository repository;
    private final ObjectMapper objectMapper;
    private final String configuredType;

    public ContentService(ContentItemRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.configuredType = "temples";
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return repository.findSummariesByType(configuredType).stream().map(this::read).toList();
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> input) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> content = new LinkedHashMap<>(input);
        content.put("id", id);
        ObjectNode payload = requireObject(objectMapper.valueToTree(content));
        ContentItem item = new ContentItem();
        item.setId(id);
        item.setType(configuredType);
        item.setImages(ImagePayloadSupport.prepareForStorage(payload, configuredType, id, null));
        item.setPayload(write(payload));
        repository.saveAndFlush(item);
        return ImagePayloadSupport.response(read(item), configuredType, id, item.getImages() != null);
    }

    @Transactional
    public Map<String, Object> update(String id, Map<String, Object> input) {
        ContentItem item = repository.findByIdAndType(id, configuredType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content item not found"));
        Map<String, Object> content = new LinkedHashMap<>(input);
        content.put("id", id);
        ObjectNode payload = requireObject(objectMapper.valueToTree(content));
        item.setImages(ImagePayloadSupport.prepareForStorage(payload, configuredType, id, item.getImages()));
        item.setPayload(write(payload));
        repository.saveAndFlush(item);
        return ImagePayloadSupport.response(read(item), configuredType, id, item.getImages() != null);
    }

    @Transactional(readOnly = true)
    public DatabaseImage image(String id) {
        ContentItem item = repository.findByIdAndType(id, configuredType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content item not found"));
        byte[] image = item.getImages();
        String mediaType = DatabaseImage.detectMediaType(image);
        if (mediaType == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No uploaded image found");
        }
        return new DatabaseImage(mediaType, image);
    }

    @Transactional
    public void delete(String id) {
        ContentItem item = repository.findByIdAndType(id, configuredType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content item not found"));
        repository.delete(item);
    }

    private ObjectNode requireObject(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must be a JSON object");
        }
        return (ObjectNode) input.deepCopy();
    }

    private Map<String, Object> read(ContentItemRepository.ContentItemSummary item) {
        return read(item.getPayload(), item.getId(), false);
    }

    private Map<String, Object> read(ContentItem item) {
        return read(item.getPayload(), item.getId(), item.getImages() != null);
    }

    private Map<String, Object> read(String payload, String id, boolean hasImage) {
        try {
            Map<String, Object> content = objectMapper.readValue(payload, new TypeReference<>() {});
            return ImagePayloadSupport.response(content, configuredType, id, hasImage);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored content is invalid", exception);
        }
    }

    private String write(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be serialized", exception);
        }
    }
}
