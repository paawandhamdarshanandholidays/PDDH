package com.pddh.contact_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.contact_service.repository.EnquiryRepository;

class EnquiryPagingTest {
    private final EnquiryRepository repository = mock(EnquiryRepository.class);
    private final EnquiryManagementService service = new EnquiryManagementService(repository);

    @Test
    void loadsEnquiriesInSortedPagesOfUpToFifty() {
        when(repository.findAll(any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(), invocation.getArgument(0), 101));

        var response = service.listPage(1, 50, "createdAt", "DESC");

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(101);
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rejectsUnsupportedSortAndPageSizesAboveFifty() {
        assertThatThrownBy(() -> service.listPage(0, 51, "createdAt", "DESC"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.listPage(0, 50, "message", "ASC"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
