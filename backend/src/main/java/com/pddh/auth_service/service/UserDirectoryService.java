package com.pddh.auth_service.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.UserDirectoryRequest;
import com.pddh.auth_service.dto.UserDirectoryPageResponse;
import com.pddh.auth_service.dto.UserDirectoryResponse;
import com.pddh.auth_service.entities.UserDirectoryEntry;
import com.pddh.auth_service.repository.UserDirectoryRepository;
import com.pddh.auth_service.security.UserRole;
import com.pddh.roleprofile.ProfileSyncEvent;

@Service
public class UserDirectoryService {
    private static final Set<UserRole> AUTHORIZED_ROLES =
            Set.of(UserRole.ADMIN, UserRole.STAFF, UserRole.CEO, UserRole.MANAGER);
    private static final String DIRECTORY_ROWS_SQL = """
            SELECT id, source, name, contact_number, email, address, referred_by, service_provided
            FROM (
                SELECT 'MANUAL-' || CAST(ud.id AS VARCHAR) AS id,
                       'MANUAL' AS source, ud.name, ud.contact_number, ud.email,
                       ud.address, ud.referred_by, ud.service_provided
                FROM user_directory ud
                WHERE ud.customer_id IS NULL
                UNION ALL
                SELECT 'ACCOUNT-' || CAST(u.id AS VARCHAR) AS id,
                       'ACCOUNT' AS source, u.name, COALESCE(u.mobile, '') AS contact_number,
                       u.email, COALESCE(ud.address, '') AS address,
                       COALESCE(ud.referred_by, '') AS referred_by,
                       COALESCE(ud.service_provided, '') AS service_provided
                FROM combined_roles u
                LEFT JOIN user_directory ud ON ud.customer_id = u.id
                WHERE upper(trim(u.role)) IN ('CUSTOMER', 'USER')
            ) directory_rows
            """;

    private final UserDirectoryRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public UserDirectoryService(UserDirectoryRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<UserDirectoryResponse> list(UserRole actorRole) {
        requireAuthorized(actorRole);
        return jdbcTemplate.query(DIRECTORY_ROWS_SQL + " ORDER BY lower(name), lower(email), id",
                (result, row) -> toResponse(result));
    }

    @Transactional(readOnly = true)
    public UserDirectoryPageResponse listPage(
            UserRole actorRole, int page, int size, String sort, String direction) {
        requireAuthorized(actorRole);
        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be 1–50");
        }
        String property = switch (sort) {
            case "name", "email", "id" -> sort;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported directory sort field");
        };
        Sort.Direction order = parseDirection(direction);
        String orderBy = switch (property) {
            case "name" -> "lower(name)";
            case "email" -> "lower(email)";
            default -> "id";
        };
        String sqlDirection = order.isAscending() ? "ASC" : "DESC";
        long total = jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM user_directory WHERE customer_id IS NULL)
                    + (SELECT COUNT(*) FROM combined_roles WHERE upper(trim(role)) IN ('CUSTOMER', 'USER'))
                """, Long.class);
        List<UserDirectoryResponse> content = jdbcTemplate.query(
                DIRECTORY_ROWS_SQL + " ORDER BY " + orderBy + " " + sqlDirection + ", id ASC LIMIT ? OFFSET ?",
                (result, row) -> toResponse(result), size, (long) page * size);
        return new UserDirectoryPageResponse(
                content, page, size, total, total == 0 ? 0 : (int) Math.ceil((double) total / size));
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
        return toManualResponse(repository.saveAndFlush(entry));
    }

    @Transactional
    public UserDirectoryResponse update(UserRole actorRole, String recordId, UserDirectoryRequest request) {
        requireAuthorized(actorRole);
        validate(request);
        Long id = manualId(recordId);
        UserDirectoryEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual directory entry not found"));
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A directory entry already uses this email");
        }
        apply(entry, request, email);
        return toManualResponse(repository.saveAndFlush(entry));
    }

    @Transactional
    public void delete(UserRole actorRole, String recordId) {
        requireAuthorized(actorRole);
        Long id = manualId(recordId);
        UserDirectoryEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual directory entry not found"));
        repository.delete(entry);
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
        var existing = repository.findByCustomerId(event.identityId());
        if (existing.isEmpty() && repository.findByEmailIgnoreCase(event.email().trim()).isPresent()) {
            return;
        }
        UserDirectoryEntry entry = existing.orElseGet(UserDirectoryEntry::new);
        entry.setCustomerId(event.identityId());
        entry.setName(event.name().trim());
        entry.setContactNumber(event.mobile() == null ? "" : event.mobile().trim());
        entry.setEmail(event.email().trim().toLowerCase(Locale.ROOT));
        if (entry.getAddress() == null) entry.setAddress("");
        if (entry.getReferredBy() == null) entry.setReferredBy("");
        if (entry.getServiceProvided() == null) entry.setServiceProvided("");
        repository.saveAndFlush(entry);
    }

    @Transactional
    public void deleteCustomer(Long customerId) {
        repository.findByCustomerId(customerId).ifPresent(repository::delete);
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

    private void apply(UserDirectoryEntry entry, UserDirectoryRequest request, String email) {
        entry.setName(request.name().trim());
        entry.setContactNumber(request.contactNumber().trim());
        entry.setEmail(email);
        entry.setAddress(request.address().trim());
        entry.setReferredBy(request.referredBy().trim());
        entry.setServiceProvided(request.serviceProvided().trim());
    }

    private Long manualId(String recordId) {
        if (recordId == null || !recordId.startsWith("MANUAL-")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Registered customer accounts are read-only in the user directory");
        }
        try {
            return Long.parseLong(recordId.substring("MANUAL-".length()));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid manual directory entry ID");
        }
    }

    private UserDirectoryResponse toResponse(java.sql.ResultSet result) throws java.sql.SQLException {
        return new UserDirectoryResponse(result.getString("id"), result.getString("source"),
                result.getString("name"), result.getString("contact_number"), result.getString("email"),
                result.getString("address"), result.getString("referred_by"), result.getString("service_provided"));
    }

    private UserDirectoryResponse toManualResponse(UserDirectoryEntry entry) {
        return new UserDirectoryResponse("MANUAL-" + entry.getId(), "MANUAL", entry.getName(),
                entry.getContactNumber(), entry.getEmail(), entry.getAddress(), entry.getReferredBy(),
                entry.getServiceProvided());
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
