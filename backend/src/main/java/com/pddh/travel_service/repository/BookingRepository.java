package com.pddh.travel_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pddh.travel_service.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
    Page<Booking> findAllByCustomerId(String customerId, Pageable pageable);
    @Query("""
            select booking from Booking booking
            where lower(booking.id) like lower(concat('%', :query, '%'))
                or lower(booking.customerId) like lower(concat('%', :query, '%'))
                or lower(booking.customerName) like lower(concat('%', :query, '%'))
                or lower(booking.email) like lower(concat('%', :query, '%'))
                or lower(booking.phone) like lower(concat('%', :query, '%'))
                or lower(booking.bookingType) like lower(concat('%', :query, '%'))
                or lower(booking.status) like lower(concat('%', :query, '%'))
                or lower(booking.tripId) like lower(concat('%', :query, '%'))
                or lower(booking.hotelId) like lower(concat('%', :query, '%'))
            """)
    Page<Booking> search(@Param("query") String query, Pageable pageable);
    @Query("""
            select booking from Booking booking
            where booking.customerId = :customerId
                and (
                    lower(booking.id) like lower(concat('%', :query, '%'))
                    or lower(booking.customerId) like lower(concat('%', :query, '%'))
                    or lower(booking.customerName) like lower(concat('%', :query, '%'))
                    or lower(booking.email) like lower(concat('%', :query, '%'))
                    or lower(booking.phone) like lower(concat('%', :query, '%'))
                    or lower(booking.bookingType) like lower(concat('%', :query, '%'))
                    or lower(booking.status) like lower(concat('%', :query, '%'))
                    or lower(booking.tripId) like lower(concat('%', :query, '%'))
                    or lower(booking.hotelId) like lower(concat('%', :query, '%'))
                )
            """)
    Page<Booking> searchByCustomerId(
            @Param("customerId") String customerId, @Param("query") String query, Pageable pageable);
    boolean existsByCustomerIdAndBookingDateAndSubmissionFingerprint(
            String customerId, LocalDate bookingDate, String submissionFingerprint);
}
