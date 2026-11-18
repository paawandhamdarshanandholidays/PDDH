package com.pddh.travel_service.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @Column(nullable = false, length = 36)
    private String id;
    @Column(nullable = false, length = 16)
    private String bookingType;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private int guests;
    @Column(name = "booking_date")
    private LocalDate bookingDate;
    @Column(nullable = false)
    private LocalDate startDate;
    private LocalDate endDate;
    private String tripId;
    private String hotelId;
    @Column(name = "customer_id", length = 120)
    private String customerId;
    @Column(name = "submission_fingerprint", length = 64)
    private String submissionFingerprint;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(nullable = false)
    private Instant createdAt;
}
