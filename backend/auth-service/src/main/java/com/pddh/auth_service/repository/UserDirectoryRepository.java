package com.pddh.auth_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pddh.auth_service.entities.UserDirectoryEntry;

public interface UserDirectoryRepository extends JpaRepository<UserDirectoryEntry, Long> {
    Optional<UserDirectoryEntry> findByCustomerId(Long customerId);
    Optional<UserDirectoryEntry> findByEmailIgnoreCase(String email);
    List<UserDirectoryEntry> findAllByOrderByNameAsc();
}
