package com.pddh.contact_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.contact_service.dto.EnquiryRequest;
import com.pddh.contact_service.dto.EnquiryUpdateRequest;
import com.pddh.contact_service.dto.EnquiryPageResponse;
import com.pddh.contact_service.entity.Enquiry;
import com.pddh.contact_service.repository.EnquiryRepository;
import com.pddh.contact_service.dto.EnquiryResponse;

@Service
public class EnquiryManagementService {
    private static final Set<String> STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");
    private final EnquiryRepository repository;

    public EnquiryManagementService(EnquiryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EnquiryResponse submit(EnquiryRequest request) {
        if (request == null || blank(request.name()) || blank(request.email()) || blank(request.phone())
                || blank(request.subject()) || blank(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All enquiry fields are required");
        }
        if (request.name().length() > 120 || request.email().length() > 254
                || request.phone().length() > 30 || request.subject().length() > 160
                || request.message().length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more enquiry fields exceed their limits");
        }
        Enquiry enquiry = new Enquiry();
        enquiry.setId(UUID.randomUUID().toString());
        enquiry.setName(request.name().trim());
        enquiry.setEmail(request.email().trim());
        enquiry.setPhone(request.phone().trim());
        enquiry.setSubject(request.subject().trim());
        enquiry.setMessage(request.message().trim());
        enquiry.setStatus("OPEN");
        enquiry.setCreatedAt(Instant.now());
        return toResponse(repository.saveAndFlush(enquiry));
    }

    @Transactional(readOnly = true)
    public List<EnquiryResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EnquiryPageResponse listPage(int page, int size, String sort, String direction, String search) {
        if (page < 0 || size < 1 || size > 10_000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be 1–10000");
        }
        String property = switch (sort) {
            case "createdAt", "name", "subject", "status", "id" -> sort;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported enquiry sort field");
        };
        Sort.Direction order = parseDirection(direction);
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Page<Enquiry> result = repository.search(query, PageRequest.of(page, size, Sort.by(order, property)));
        return new EnquiryPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public EnquiryResponse updateStatus(String id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported enquiry status");
        }
        Enquiry enquiry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found"));
        enquiry.setStatus(normalized);
        return toResponse(repository.saveAndFlush(enquiry));
    }

    @Transactional
    public EnquiryResponse update(String id, EnquiryUpdateRequest request) {
        validateContent(request);
        Enquiry enquiry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found"));
        enquiry.setName(request.name().trim());
        enquiry.setEmail(request.email().trim());
        enquiry.setPhone(request.phone().trim());
        enquiry.setSubject(request.subject().trim());
        enquiry.setMessage(request.message().trim());
        return toResponse(repository.saveAndFlush(enquiry));
    }

    @Transactional
    public void delete(String id) {
        Enquiry enquiry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found"));
        repository.delete(enquiry);
    }

    private void validateContent(EnquiryUpdateRequest request) {
        if (request == null || blank(request.name()) || blank(request.email()) || blank(request.phone())
                || blank(request.subject()) || blank(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All enquiry fields are required");
        }
        if (request.name().length() > 120 || request.email().length() > 254
                || request.phone().length() > 30 || request.subject().length() > 160
                || request.message().length() > 5000
                || !request.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more enquiry fields are invalid");
        }
    }

    private EnquiryResponse toResponse(Enquiry enquiry) {
        return new EnquiryResponse(enquiry.getId(), enquiry.getName(), enquiry.getEmail(), enquiry.getPhone(),
                enquiry.getSubject(), enquiry.getMessage(), enquiry.getStatus(), enquiry.getCreatedAt());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private Sort.Direction parseDirection(String direction) {
        if ("ASC".equalsIgnoreCase(direction)) return Sort.Direction.ASC;
        if ("DESC".equalsIgnoreCase(direction)) return Sort.Direction.DESC;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be ASC or DESC");
    }
}
