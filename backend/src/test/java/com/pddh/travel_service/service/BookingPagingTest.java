package com.pddh.travel_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.travel_service.entity.Booking;
import com.pddh.travel_service.dto.BookingUpdateRequest;
import com.pddh.travel_service.repository.BookingRepository;
import com.pddh.travel_service.security.TravelIdentity;

class BookingPagingTest {
    private final BookingRepository repository = mock(BookingRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final com.pddh.trips_service.service.ContentService trips =
            mock(com.pddh.trips_service.service.ContentService.class);
    private final com.pddh.stays_service.service.ContentService stays =
            mock(com.pddh.stays_service.service.ContentService.class);
    private final BookingService service = new BookingService(repository, jdbcTemplate, trips, stays);

    @Test
    void managerCanSearchSortedBookingPagesWithinTheMaximumSize() {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setBookingType("TRIP");
        booking.setCustomerName("Customer");
        booking.setEmail("customer@example.test");
        booking.setPhone("123");
        booking.setGuests(2);
        booking.setBookingDate(LocalDate.now());
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setStatus("PENDING");
        booking.setCreatedAt(Instant.now());
        when(repository.search(eq("asha"), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(booking), invocation.getArgument(1), 10_001));

        var response = service.listPage(
                new TravelIdentity("manager", "manager@example.test", "MANAGER"),
                0, 10_000, "customerName", "ASC", " asha ");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(10_001);
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq("asha"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10_000);
        assertThat(pageable.getValue().getSort().getOrderFor("customerName").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void customerPageIsScopedToTheAuthenticatedCustomer() {
        TravelIdentity customer = new TravelIdentity("traveller", "customer@example.test", "CUSTOMER");
        when(repository.searchByCustomerId(eq(customer.username()), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 25), 0));

        var response = service.listPage(customer, 0, 25, "createdAt", "DESC", "");

        assertThat(response.content()).isEmpty();
        verify(repository).searchByCustomerId(eq(customer.username()), eq(""), any(Pageable.class));
    }

    @Test
    void rejectsPageSizesAboveTenThousand() {
        assertThatThrownBy(() -> service.listPage(
                new TravelIdentity("manager", "manager@example.test", "MANAGER"),
                0, 10_001, "createdAt", "DESC", ""))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void bookingStatusUpdateReturnsThePersistedRecord() {
        Booking booking = booking();
        when(repository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(repository.saveAndFlush(booking)).thenReturn(booking);

        var updated = service.updateStatus(
                "booking-1", " confirmed ", new TravelIdentity("manager", "manager@example.test", "MANAGER"));

        assertThat(updated.id()).isEqualTo("booking-1");
        assertThat(updated.status()).isEqualTo("CONFIRMED");
        verify(repository).saveAndFlush(booking);
    }

    @Test
    void bookingEditPreservesItsReferenceAndStatus() {
        Booking booking = booking();
        when(repository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(repository.saveAndFlush(booking)).thenReturn(booking);
        BookingUpdateRequest request = new BookingUpdateRequest(
                "Updated customer", "updated@example.test", "456", 3,
                "2026-12-10", "2026-12-15", "trip-2", null);

        var updated = service.update(
                "booking-1", request, new TravelIdentity("manager", "manager@example.test", "MANAGER"));

        assertThat(updated.id()).isEqualTo("booking-1");
        assertThat(updated.status()).isEqualTo("PENDING");
        assertThat(updated.customerName()).isEqualTo("Updated customer");
        assertThat(updated.tripId()).isEqualTo("trip-2");
    }

    @Test
    void bookingDeleteIsRestrictedToReviewRoles() {
        assertThatThrownBy(() -> service.delete("booking-1",
                new TravelIdentity("customer", "customer@example.test", "CUSTOMER")))
                .isInstanceOf(ResponseStatusException.class);
    }

    private Booking booking() {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setBookingType("TRIP");
        booking.setCustomerId("customer");
        booking.setCustomerName("Customer");
        booking.setEmail("customer@example.test");
        booking.setPhone("123");
        booking.setGuests(2);
        booking.setBookingDate(LocalDate.of(2026, 11, 14));
        booking.setStartDate(LocalDate.of(2026, 12, 1));
        booking.setEndDate(LocalDate.of(2026, 12, 5));
        booking.setTripId("trip-1");
        booking.setStatus("PENDING");
        booking.setCreatedAt(Instant.now());
        return booking;
    }
}
