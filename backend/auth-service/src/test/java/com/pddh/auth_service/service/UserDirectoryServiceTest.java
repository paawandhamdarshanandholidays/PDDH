package com.pddh.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.UserDirectoryRequest;
import com.pddh.auth_service.entities.UserDirectoryEntry;
import com.pddh.auth_service.repository.UserDirectoryRepository;
import com.pddh.auth_service.security.UserRole;
import com.pddh.roleprofile.ProfileSyncEvent;

class UserDirectoryServiceTest {
    private final UserDirectoryRepository repository = org.mockito.Mockito.mock(UserDirectoryRepository.class);
    private final UserDirectoryService service = new UserDirectoryService(repository);

    @Test
    void createsDirectoryRowsForAuthorizedStaff() {
        when(repository.findByEmailIgnoreCase("traveller@example.test")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserDirectoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UserDirectoryRequest request = new UserDirectoryRequest(
                " Traveller ", " +91 1234567890 ", "Traveller@example.test", " Vrindavan ",
                "Walk-in", "Darshan booking");

        service.create(UserRole.STAFF, request);

        var saved = org.mockito.ArgumentCaptor.forClass(UserDirectoryEntry.class);
        verify(repository).saveAndFlush(saved.capture());
        assertEquals("Traveller", saved.getValue().getName());
        assertEquals("+91 1234567890", saved.getValue().getContactNumber());
        assertEquals("traveller@example.test", saved.getValue().getEmail());
        assertEquals("Vrindavan", saved.getValue().getAddress());
    }

    @Test
    void deniesDirectoryWritesForCustomers() {
        UserDirectoryRequest request = new UserDirectoryRequest(
                "Traveller", "123", "traveller@example.test", "Address", "Referral", "Service");

        assertThrows(ResponseStatusException.class, () -> service.create(UserRole.CUSTOMER, request));
    }

    @Test
    void profileSyncAddsCustomersAndPreservesManuallyEnteredDetails() {
        UserDirectoryEntry entry = new UserDirectoryEntry();
        entry.setAddress("Mathura");
        entry.setReferredBy("Temple event");
        entry.setServiceProvided("Accommodation");
        when(repository.findByCustomerId(42L)).thenReturn(Optional.of(entry));
        when(repository.saveAndFlush(any(UserDirectoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProfileSyncEvent event = new ProfileSyncEvent(
                "event-id", "UPSERT", 42L, "CUSTOMER", "Updated Name", "traveller",
                "traveller@example.test", "ignored-hash", "+91 1111111111", null, true);

        service.syncCustomer(event);

        verify(repository).saveAndFlush(entry);
        assertEquals("Updated Name", entry.getName());
        assertEquals("traveller@example.test", entry.getEmail());
        assertEquals("+91 1111111111", entry.getContactNumber());
        assertEquals("Mathura", entry.getAddress());
        assertEquals("Temple event", entry.getReferredBy());
        assertEquals("Accommodation", entry.getServiceProvided());
    }
}
