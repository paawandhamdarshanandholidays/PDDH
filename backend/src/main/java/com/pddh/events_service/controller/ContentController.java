package com.pddh.events_service.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.contentvalidation.PublicImageUrlValidator;
import com.pddh.contentvalidation.DatabaseImage;
import com.pddh.events_service.security.ContentIdentityService;
import com.pddh.events_service.service.ContentService;
import com.pddh.events_service.api_common.resilience.RateLimitGuard;
import com.pddh.events_service.idempotency.IdempotencyService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/catalog/events")
public class ContentController {
    private final ContentService contentService;
    private final ContentIdentityService identityService;
    private final RateLimitGuard rateLimitGuard;
    private final IdempotencyService idempotencyService;

    public ContentController(
            ContentService contentService,
            ContentIdentityService identityService,
            RateLimitGuard rateLimitGuard,
            IdempotencyService idempotencyService) {
        this.contentService = contentService;
        this.identityService = identityService;
        this.rateLimitGuard = rateLimitGuard;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return contentService.list();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable String id) {
        DatabaseImage image = contentService.image(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .header("Cache-Control", "public, max-age=3600")
                .body(image.bytes());
    }

    @PostMapping
    public ResponseEntity<Object> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        requireManager(request);
        PublicImageUrlValidator.validate(body);
        Object response = idempotencyService.execute(
                identityService.requireSubject(request),
                "content.create:" + request.getRequestURI(),
                idempotencyKey,
                body,
                Object.class,
                () -> contentService.create(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        requireManager(request);
        PublicImageUrlValidator.validate(body);
        return contentService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
        requireManager(request);
        contentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireManager(HttpServletRequest request) {
        String role = identityService.requireRole(request);
        if (!CONTENT_MANAGERS.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only content managers can change this content");
        }
        rateLimitGuard.check("content.write");
    }

    private static final Set<String> CONTENT_MANAGERS = Set.of("CEO", "ADMIN", "MANAGER");
}
