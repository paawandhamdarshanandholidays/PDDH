package com.pddh.gallery_service.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pddh.gallery_service.entity.ContentItem;
import com.pddh.gallery_service.repository.ContentItemRepository;

@Service
public class ContentService {
    private final ContentItemRepository repository;
    private final ObjectMapper objectMapper;
    private final String configuredType;

    public ContentService(ContentItemRepository repository, ObjectMapper objectMapper,
            @Value("${app.content.type}") String configuredType) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.configuredType = configuredType.toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return repository.findAllByTypeOrderByIdAsc(configuredType).stream().map(this::read).toList();
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
        item.setPayload(write(payload));
        repository.saveAndFlush(item);
        return content;
    }

    @Transactional
    public Map<String, Object> update(String id, Map<String, Object> input) {
        ContentItem item = repository.findByIdAndType(id, configuredType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content item not found"));
        Map<String, Object> content = new LinkedHashMap<>(input);
        content.put("id", id);
        ObjectNode payload = requireObject(objectMapper.valueToTree(content));
        item.setPayload(write(payload));
        repository.saveAndFlush(item);
        return content;
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

    private Map<String, Object> read(ContentItem item) {
        try {
            return objectMapper.readValue(item.getPayload(), new TypeReference<>() {});
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
