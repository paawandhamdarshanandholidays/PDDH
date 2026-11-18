package com.pddh.contact_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.contact_service.dto.EnquiryUpdateRequest;
import com.pddh.contact_service.entity.Enquiry;
import com.pddh.contact_service.repository.EnquiryRepository;

class EnquiryManagementTest {
    private final EnquiryRepository repository = mock(EnquiryRepository.class);
    private final EnquiryManagementService service = new EnquiryManagementService(repository);

    @Test
    void updatesEnquiryContentWithoutChangingItsReferenceOrStatus() {
        Enquiry enquiry = enquiry();
        when(repository.findById("enquiry-1")).thenReturn(Optional.of(enquiry));
        when(repository.saveAndFlush(enquiry)).thenReturn(enquiry);

        var updated = service.update("enquiry-1", new EnquiryUpdateRequest(
                "Updated name", "updated@example.test", "456", "Updated subject", "Updated message"));

        assertThat(updated.id()).isEqualTo("enquiry-1");
        assertThat(updated.createdAt()).isEqualTo(enquiry.getCreatedAt());
        assertThat(updated.status()).isEqualTo("OPEN");
        assertThat(updated.subject()).isEqualTo("Updated subject");
        verify(repository).saveAndFlush(enquiry);
    }

    @Test
    void rejectsInvalidEnquiryEmailAndDeletesByImmutableReference() {
        assertThatThrownBy(() -> service.update("enquiry-1", new EnquiryUpdateRequest(
                "Name", "not-an-email", "123", "Subject", "Message")))
                .isInstanceOf(ResponseStatusException.class);

        Enquiry enquiry = enquiry();
        when(repository.findById("enquiry-1")).thenReturn(Optional.of(enquiry));
        service.delete("enquiry-1");
        verify(repository).delete(enquiry);
    }

    private Enquiry enquiry() {
        Enquiry enquiry = new Enquiry();
        enquiry.setId("enquiry-1");
        enquiry.setName("Customer");
        enquiry.setEmail("customer@example.test");
        enquiry.setPhone("123");
        enquiry.setSubject("Question");
        enquiry.setMessage("Details");
        enquiry.setStatus("OPEN");
        enquiry.setCreatedAt(Instant.parse("2026-11-18T00:00:00Z"));
        return enquiry;
    }
}
