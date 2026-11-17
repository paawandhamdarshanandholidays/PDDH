package com.pddh.travel_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.pddh.travel_service.dto.BookingRecordResponse;
import com.pddh.travel_service.dto.BookingPageResponse;
import com.pddh.travel_service.dto.BookingRequest;
import com.pddh.travel_service.dto.BookingResponse;
import com.pddh.travel_service.security.JwtIdentityService;
import com.pddh.travel_service.security.TravelIdentity;
import com.pddh.travel_service.service.BookingService;
import com.pddh.api_common.resilience.RateLimitGuard;
import com.pddh.travel_service.idempotency.IdempotencyService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final JwtIdentityService identityService;
    private final RateLimitGuard rateLimitGuard;
    private final IdempotencyService idempotencyService;

    public BookingController(
            BookingService bookingService,
            JwtIdentityService identityService,
            RateLimitGuard rateLimitGuard,
            IdempotencyService idempotencyService) {
        this.bookingService = bookingService;
        this.identityService = identityService;
        this.rateLimitGuard = rateLimitGuard;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BookingRequest request,
            HttpServletRequest servletRequest) {
        rateLimitGuard.check("booking.create");
        TravelIdentity identity = identityService.requireIdentity(servletRequest);
        if (!"CUSTOMER".equals(identity.role())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only a signed-in customer can create a booking");
        }
        BookingResponse response = idempotencyService.execute(
                identity.username(), "booking.create", idempotencyKey, request, BookingResponse.class,
                () -> bookingService.create(request, identity));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<BookingRecordResponse> list(HttpServletRequest request) {
        TravelIdentity identity = identityService.requireIdentity(request);
        return bookingService.list(identity);
    }

    @GetMapping("/page")
    public BookingPageResponse listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            HttpServletRequest request) {
        TravelIdentity identity = identityService.requireIdentity(request);
        return bookingService.listPage(identity, page, size, sort, direction);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String id, @RequestBody StatusRequest status, HttpServletRequest request) {
        TravelIdentity identity = identityService.requireIdentity(request);
        bookingService.updateStatus(id, status == null ? null : status.status(), identity);
        return ResponseEntity.noContent().build();
    }

    public record StatusRequest(String status) {
    }
}
