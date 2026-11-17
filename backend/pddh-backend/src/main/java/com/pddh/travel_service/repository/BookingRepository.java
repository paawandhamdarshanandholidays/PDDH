package com.pddh.travel_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pddh.travel_service.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
    Page<Booking> findAllByCustomerId(String customerId, Pageable pageable);
    boolean existsByCustomerIdAndBookingDateAndSubmissionFingerprint(
            String customerId, LocalDate bookingDate, String submissionFingerprint);
}
