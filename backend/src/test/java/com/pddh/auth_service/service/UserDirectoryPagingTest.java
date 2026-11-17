package com.pddh.auth_service.service;

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

import com.pddh.auth_service.repository.UserDirectoryRepository;
import com.pddh.auth_service.security.UserRole;

class UserDirectoryPagingTest {
    private final UserDirectoryRepository repository = mock(UserDirectoryRepository.class);
    private final UserDirectoryService service = new UserDirectoryService(repository);

    @Test
    void managerCanLoadSortedDirectoryPagesOfAtMostFifty() {
        when(repository.findAll(any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(), invocation.getArgument(0), 51));

        var response = service.listPage(UserRole.MANAGER, 0, 50, "email", "DESC");

        assertThat(response.totalElements()).isEqualTo(51);
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort().getOrderFor("email").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rejectsDirectoryPagesAboveFiftyAndCustomerAccess() {
        assertThatThrownBy(() -> service.listPage(UserRole.MANAGER, 0, 51, "name", "ASC"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.listPage(UserRole.CUSTOMER, 0, 50, "name", "ASC"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
