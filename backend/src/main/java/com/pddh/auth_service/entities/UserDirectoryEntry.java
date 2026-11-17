package com.pddh.auth_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_directory", indexes = {
        @Index(name = "idx_user_directory_customer_id", columnList = "customer_id"),
        @Index(name = "idx_user_directory_name", columnList = "name"),
        @Index(name = "idx_user_directory_contact", columnList = "contact_number") })
public class UserDirectoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", unique = true)
    private Long customerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(name = "referred_by", nullable = false, length = 120)
    private String referredBy;

    @Column(name = "service_provided", nullable = false, length = 160)
    private String serviceProvided;
}
