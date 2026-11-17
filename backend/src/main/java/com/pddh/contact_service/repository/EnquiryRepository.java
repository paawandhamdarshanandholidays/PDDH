package com.pddh.contact_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pddh.contact_service.entity.Enquiry;

public interface EnquiryRepository extends JpaRepository<Enquiry, String> {
    List<Enquiry> findAllByOrderByCreatedAtDesc();
    Page<Enquiry> findAll(Pageable pageable);
}
