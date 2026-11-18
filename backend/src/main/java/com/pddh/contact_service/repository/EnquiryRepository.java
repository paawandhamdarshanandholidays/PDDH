package com.pddh.contact_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pddh.contact_service.entity.Enquiry;

public interface EnquiryRepository extends JpaRepository<Enquiry, String> {
    List<Enquiry> findAllByOrderByCreatedAtDesc();
    Page<Enquiry> findAll(Pageable pageable);
    @Query("""
            select enquiry from Enquiry enquiry
            where lower(enquiry.id) like lower(concat('%', :query, '%'))
                or lower(enquiry.name) like lower(concat('%', :query, '%'))
                or lower(enquiry.email) like lower(concat('%', :query, '%'))
                or lower(enquiry.phone) like lower(concat('%', :query, '%'))
                or lower(enquiry.subject) like lower(concat('%', :query, '%'))
                or lower(enquiry.message) like lower(concat('%', :query, '%'))
                or lower(enquiry.status) like lower(concat('%', :query, '%'))
            """)
    Page<Enquiry> search(@Param("query") String query, Pageable pageable);
}
