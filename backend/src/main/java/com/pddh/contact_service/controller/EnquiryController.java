package com.pddh.contact_service.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.pddh.contact_service.dto.EnquiryRequest;
import com.pddh.contact_service.dto.EnquiryUpdateRequest;
import com.pddh.contact_service.dto.EnquiryPageResponse;
import com.pddh.contact_service.dto.EnquiryResponse;
import com.pddh.contact_service.security.EnquiryIdentityService;
import com.pddh.contact_service.service.EnquiryManagementService;
import com.pddh.contact_service.api_common.resilience.RateLimitGuard;
import com.pddh.contact_service.idempotency.IdempotencyService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {
    private final EnquiryManagementService enquiryService;
    private final EnquiryIdentityService identityService;
    private final RateLimitGuard rateLimitGuard;
    private final IdempotencyService idempotencyService;

    public EnquiryController(
            EnquiryManagementService enquiryService,
            EnquiryIdentityService identityService,
            RateLimitGuard rateLimitGuard,
            IdempotencyService idempotencyService) {
        this.enquiryService = enquiryService;
        this.identityService = identityService;
        this.rateLimitGuard = rateLimitGuard;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<EnquiryResponse> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EnquiryRequest request) {
        rateLimitGuard.check("enquiry.submit");
        String scope = request.email() == null ? "public" : request.email().trim().toLowerCase(Locale.ROOT);
        EnquiryResponse response = idempotencyService.execute(
                scope, "enquiry.submit", idempotencyKey, request, EnquiryResponse.class,
                () -> enquiryService.submit(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<EnquiryResponse> list(HttpServletRequest request) {
        identityService.requireStaff(request);
        return enquiryService.list();
    }

    @GetMapping("/page")
    public EnquiryPageResponse listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "") String search,
            HttpServletRequest request) {
        identityService.requireStaff(request);
        return enquiryService.listPage(page, size, sort, direction, search);
    }

    @PatchMapping("/{id}/status")
    public EnquiryResponse updateStatus(
            @PathVariable String id, @RequestBody StatusRequest request, HttpServletRequest servletRequest) {
        identityService.requireStaff(servletRequest);
        return enquiryService.updateStatus(id, request == null ? null : request.status());
    }

    @PatchMapping("/{id}")
    public EnquiryResponse update(
            @PathVariable String id, @RequestBody EnquiryUpdateRequest update, HttpServletRequest request) {
        identityService.requireStaff(request);
        return enquiryService.update(id, update);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
        identityService.requireStaff(request);
        enquiryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record StatusRequest(String status) {
    }
}
