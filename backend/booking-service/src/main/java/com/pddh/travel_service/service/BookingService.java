package com.pddh.travel_service.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.travel_service.dto.BookingRecordResponse;
import com.pddh.travel_service.dto.BookingRequest;
import com.pddh.travel_service.dto.BookingResponse;
import com.pddh.travel_service.entity.Booking;
import com.pddh.travel_service.repository.BookingRepository;
import com.pddh.travel_service.security.TravelIdentity;
import com.pddh.travel_service.service.OutboxService;
import com.pddh.saga_events.BookingCreatedEvent;

@Service
public class BookingService {
    private static final Set<String> BOOKING_TYPES = Set.of("TRIP", "STAY");
    private static final Set<String> BOOKING_STATUSES = Set.of("PENDING", "CONFIRMED", "REJECTED", "CANCELLED");
    private final BookingRepository bookingRepository;
    private final OutboxService outboxService;
    private final JdbcTemplate jdbcTemplate;

    public BookingService(BookingRepository bookingRepository, OutboxService outboxService, JdbcTemplate jdbcTemplate) {
        this.bookingRepository = bookingRepository;
        this.outboxService = outboxService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public BookingResponse create(BookingRequest request, TravelIdentity identity) {
        if (identity == null || !"CUSTOMER".equals(identity.role())
                || blank(identity.username()) || blank(identity.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a signed-in customer can create a booking");
        }
        if (request == null || blank(request.bookingType()) || blank(request.customerName())
                || blank(request.phone()) || request.guests() == null
                || request.guests() < 1 || request.guests() > 30 || blank(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete and valid booking details are required");
        }
        String type = request.bookingType().trim().toUpperCase(Locale.ROOT);
        if (!BOOKING_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported booking type");
        }
        if (("TRIP".equals(type) && blank(request.tripId()))
                || ("STAY".equals(type) && blank(request.hotelId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A trip or stay must be selected");
        }
        LocalDate startDate = parseDate(request.startDate(), "startDate");
        LocalDate endDate = blank(request.endDate()) ? null : parseDate(request.endDate(), "endDate");
        if ("TRIP".equals(type) && endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A trip end date is required");
        }
        if (startDate.isBefore(LocalDate.now()) || endDate != null && !endDate.isAfter(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking dates are invalid");
        }
        LocalDate bookingDate = LocalDate.now();
        String customerId = identity.username();
        String tripId = blankToNull(request.tripId());
        String hotelId = blankToNull(request.hotelId());
        String fingerprint = fingerprint(type, request.customerName().trim(), request.phone().trim(),
                request.guests().toString(), startDate.toString(), endDate == null ? "" : endDate.toString(),
                tripId == null ? "" : tripId, hotelId == null ? "" : hotelId);
        acquireDuplicateSubmissionLock(customerId, bookingDate, fingerprint);
        if (bookingRepository.existsByCustomerIdAndBookingDateAndSubmissionFingerprint(
                customerId, bookingDate, fingerprint)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An identical booking has already been submitted today");
        }
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString());
        booking.setBookingType(type);
        booking.setCustomerName(request.customerName().trim());
        booking.setEmail(identity.email().trim());
        booking.setPhone(request.phone().trim());
        booking.setGuests(request.guests());
        booking.setBookingDate(bookingDate);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setTripId(tripId);
        booking.setHotelId(hotelId);
        booking.setCustomerId(customerId);
        booking.setSubmissionFingerprint(fingerprint);
        booking.setStatus("PENDING");
        booking.setCreatedAt(java.time.Instant.now());
        bookingRepository.saveAndFlush(booking);
        outboxService.enqueue(booking.getId(), "booking.created", new BookingCreatedEvent(
                booking.getId(), booking.getBookingType(), booking.getTripId(), booking.getHotelId()));
        return new BookingResponse(booking.getId(), "Booking submitted and is being validated", booking.getBookingDate());
    }

    private void acquireDuplicateSubmissionLock(String customerId, LocalDate bookingDate, String fingerprint) {
        String lockIdentity = customerId + '\n' + bookingDate + '\n' + fingerprint;
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                statement.setString(1, lockIdentity);
                statement.execute();
            }
            return null;
        });
    }

    private String fingerprint(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] value = field.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
                digest.update(value);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<BookingRecordResponse> list(TravelIdentity identity) {
        if ("CUSTOMER".equals(identity.role())) {
            if (blank(identity.username())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer identity has no account identifier");
            }
            return bookingRepository.findAllByCustomerIdOrderByCreatedAtDesc(identity.username())
                    .stream().map(this::toResponse).toList();
        }
        if (!identity.canReviewBookings()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view bookings");
        }
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void updateStatus(String id, String requestedStatus, TravelIdentity identity) {
        if (!identity.canReviewBookings()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review bookings");
        }
        if (requestedStatus == null
                || !BOOKING_STATUSES.contains(requestedStatus.trim().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported booking status");
        }
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        booking.setStatus(requestedStatus.trim().toUpperCase(Locale.ROOT));
        bookingRepository.saveAndFlush(booking);
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must use YYYY-MM-DD format");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private BookingRecordResponse toResponse(Booking booking) {
        return new BookingRecordResponse(booking.getId(), booking.getCustomerId(), booking.getBookingType(),
                booking.getCustomerName(), booking.getEmail(), booking.getPhone(), booking.getGuests(),
                booking.getBookingDate(), booking.getStartDate(), booking.getEndDate(), booking.getTripId(),
                booking.getHotelId(), booking.getStatus());
    }
}
