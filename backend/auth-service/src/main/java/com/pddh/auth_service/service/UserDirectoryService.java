package com.pddh.auth_service.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.UserDirectoryRequest;
import com.pddh.auth_service.dto.UserDirectoryResponse;
import com.pddh.auth_service.entities.UserDirectoryEntry;
import com.pddh.auth_service.repository.UserDirectoryRepository;
import com.pddh.auth_service.security.UserRole;
import com.pddh.roleprofile.ProfileSyncEvent;

@Service
public class UserDirectoryService {
    private static final Set<UserRole> AUTHORIZED_ROLES =
            Set.of(UserRole.ADMIN, UserRole.STAFF, UserRole.CEO, UserRole.MANAGER);

    private final UserDirectoryRepository repository;

    public UserDirectoryService(UserDirectoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserDirectoryResponse> list(UserRole actorRole) {
        requireAuthorized(actorRole);
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserDirectoryResponse create(UserRole actorRole, UserDirectoryRequest request) {
        requireAuthorized(actorRole);
        validate(request);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A directory entry already uses this email");
        }
        UserDirectoryEntry entry = new UserDirectoryEntry();
        entry.setName(request.name().trim());
        entry.setContactNumber(request.contactNumber().trim());
        entry.setEmail(email);
        entry.setAddress(request.address().trim());
        entry.setReferredBy(request.referredBy().trim());
        entry.setServiceProvided(request.serviceProvided().trim());
        return toResponse(repository.saveAndFlush(entry));
    }

    @Transactional
    public void syncCustomer(ProfileSyncEvent event) {
        if (event == null || !"UPSERT".equalsIgnoreCase(event.operation())
                || !"CUSTOMER".equalsIgnoreCase(event.role())) {
            return;
        }
        if (event.identityId() == null || blank(event.name()) || blank(event.email())) {
            throw new IllegalArgumentException("Customer profile sync event is missing required directory values");
        }
        UserDirectoryEntry entry = repository.findByCustomerId(event.identityId())
                .or(() -> repository.findByEmailIgnoreCase(event.email().trim()))
                .orElseGet(UserDirectoryEntry::new);
        entry.setCustomerId(event.identityId());
        entry.setName(event.name().trim());
        entry.setContactNumber(event.mobile() == null ? "" : event.mobile().trim());
        entry.setEmail(event.email().trim().toLowerCase(Locale.ROOT));
        if (entry.getAddress() == null) entry.setAddress("");
        if (entry.getReferredBy() == null) entry.setReferredBy("");
        if (entry.getServiceProvided() == null) entry.setServiceProvided("");
        repository.saveAndFlush(entry);
    }

    private void requireAuthorized(UserRole role) {
        if (!AUTHORIZED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access the user directory");
        }
    }

    private void validate(UserDirectoryRequest request) {
        if (request == null || blank(request.name()) || blank(request.contactNumber())
                || blank(request.email()) || blank(request.address())
                || blank(request.referredBy()) || blank(request.serviceProvided())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All user directory fields are required");
        }
        if (request.name().length() > 120 || request.contactNumber().length() > 30
                || request.email().length() > 254 || request.address().length() > 300
                || request.referredBy().length() > 120 || request.serviceProvided().length() > 160
                || !request.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more directory fields are invalid");
        }
    }

    private UserDirectoryResponse toResponse(UserDirectoryEntry entry) {
        return new UserDirectoryResponse(entry.getId(), entry.getName(), entry.getContactNumber(), entry.getEmail(),
                entry.getAddress(), entry.getReferredBy(), entry.getServiceProvided());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
