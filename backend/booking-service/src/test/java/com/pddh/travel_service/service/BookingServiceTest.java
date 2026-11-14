package com.pddh.travel_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.saga_events.BookingCreatedEvent;
import com.pddh.travel_service.dto.BookingRequest;
import com.pddh.travel_service.entity.Booking;
import com.pddh.travel_service.repository.BookingRepository;
import com.pddh.travel_service.security.TravelIdentity;

class BookingServiceTest {
    private BookingRepository repository;
    private OutboxService outboxService;
    private JdbcTemplate jdbcTemplate;
    private BookingService service;

    @BeforeEach
    void setUp() {
        repository = mock(BookingRepository.class);
        outboxService = mock(OutboxService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new BookingService(repository, outboxService, jdbcTemplate);
        when(repository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.existsByCustomerIdAndBookingDateAndSubmissionFingerprint(
                any(), any(), any())).thenReturn(false);
    }

    @Test
    void createPersistsPendingBookingAndEnqueuesSagaEvent() {
        BookingRequest request = new BookingRequest(
                "TRIP", "Customer", "9876543210", 2,
                LocalDate.now().plusDays(10).toString(), LocalDate.now().plusDays(12).toString(), "trip-1", null);
        TravelIdentity customer = new TravelIdentity("customer-123", "customer@example.com", "CUSTOMER");

        var response = service.create(request, customer);

        assertThat(response.reference()).isNotBlank();
        assertThat(response.message()).contains("being validated");
        assertThat(response.bookingDate()).isEqualTo(LocalDate.now());
        var savedBooking = forClass(Booking.class);
        verify(repository).saveAndFlush(savedBooking.capture());
        assertThat(savedBooking.getValue().getCustomerId()).isEqualTo(customer.username());
        assertThat(savedBooking.getValue().getSubmissionFingerprint()).hasSize(64);
        assertThat(savedBooking.getValue().getEmail()).isEqualTo(customer.email());
        assertThat(savedBooking.getValue().getBookingDate()).isEqualTo(LocalDate.now());
        verify(outboxService).enqueue(
                eq(response.reference()),
                eq("booking.created"),
                any(BookingCreatedEvent.class));
    }

    @Test
    void createRejectsIdenticalBookingSubmittedBySameCustomerOnSameDay() {
        BookingRequest request = new BookingRequest(
                "TRIP", "Customer", "9876543210", 2,
                LocalDate.now().plusDays(10).toString(), LocalDate.now().plusDays(12).toString(), "trip-1", null);
        TravelIdentity customer = new TravelIdentity("customer-123", "customer@example.com", "CUSTOMER");
        when(repository.existsByCustomerIdAndBookingDateAndSubmissionFingerprint(
                eq(customer.username()), eq(LocalDate.now()), any())).thenReturn(false, true);

        service.create(request, customer);

        assertThatThrownBy(() -> service.create(request, customer))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        var fingerprints = forClass(String.class);
        verify(repository, times(2)).existsByCustomerIdAndBookingDateAndSubmissionFingerprint(
                eq(customer.username()), eq(LocalDate.now()), fingerprints.capture());
        assertThat(fingerprints.getAllValues().get(0)).isEqualTo(fingerprints.getAllValues().get(1));
        verify(repository).saveAndFlush(any(Booking.class));
        verify(outboxService).enqueue(any(), any(), any(BookingCreatedEvent.class));
    }

    @Test
    void createRejectsPastStartDate() {
        BookingRequest request = new BookingRequest(
                "TRIP", "Customer", "9876543210", 2,
                LocalDate.now().minusDays(1).toString(), LocalDate.now().plusDays(2).toString(), "trip-1", null);

        assertThatThrownBy(() -> service.create(request,
                new TravelIdentity("customer-123", "customer@example.com", "CUSTOMER")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tripBookingRequiresEndDateAfterStartDate() {
        BookingRequest request = new BookingRequest(
                "TRIP", "Customer", "9876543210", 2,
                LocalDate.now().plusDays(10).toString(), null, "trip-1", null);

        assertThatThrownBy(() -> service.create(request,
                new TravelIdentity("customer-123", "customer@example.com", "CUSTOMER")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("end date");
    }

    @Test
    void rejectsBookingWhenUserIsNotACustomer() {
        BookingRequest request = new BookingRequest(
                "TRIP", "Customer", "9876543210", 2,
                LocalDate.now().plusDays(10).toString(), LocalDate.now().plusDays(11).toString(), "trip-1", null);

        assertThatThrownBy(() -> service.create(request,
                new TravelIdentity("admin", "admin@example.com", "ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void customerCanOnlyListBookingsMatchingTheirTokenEmail() {
        TravelIdentity customer = new TravelIdentity("traveller", "customer@example.com", "CUSTOMER");
        when(repository.findAllByCustomerIdOrderByCreatedAtDesc(customer.username()))
                .thenReturn(java.util.List.of());

        assertThat(service.list(customer)).isEmpty();
        verify(repository).findAllByCustomerIdOrderByCreatedAtDesc(customer.username());
    }

    @Test
    void staffCanUpdateBookingStatus() {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setStatus("PENDING");
        when(repository.findById("booking-1")).thenReturn(Optional.of(booking));

        service.updateStatus("booking-1", "CONFIRMED",
                new TravelIdentity("staff", "staff@example.com", "STAFF"));

        assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        verify(repository).saveAndFlush(booking);
    }
}
