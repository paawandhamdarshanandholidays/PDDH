package com.pddh.roleprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class RoleProfileServiceTest {
    private RoleProfileRepository repository;
    private RoleProfileProperties properties;
    private RoleProfileService service;

    @BeforeEach
    void setUp() {
        repository = mock(RoleProfileRepository.class);
        properties = new RoleProfileProperties("staff", "STAFF", "/api/roles/staff", "pddh.profile.staff");
        service = new RoleProfileService(repository, properties);
    }

    @Test
    void listsProfilesForAuthorizedActorAndCapsPageSize() {
        var profile = new RoleProfileService.UserProfile("4", "Staff Member", "staff4", "staff@test.com",
                "123", "STAFF", true, null);
        when(repository.count()).thenReturn(51L);
        when(repository.page(50, 50L)).thenReturn(List.of(profile));

        var result = service.list("CEO", 1, 100);

        assertThat(result.content()).containsExactly(profile);
        assertThat(result.size()).isEqualTo(50);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(repository).page(50, 50L);
    }

    @Test
    void deniesLowerRankActorAndInvalidPagination() {
        assertThatThrownBy(() -> service.list("CUSTOMER", 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> service.list("CEO", -1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.list("CEO", 0, 0))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).count();
    }

    @Test
    void authenticatesActiveProfileAndUpdatesLastLogin() {
        String hash = new BCryptPasswordEncoder().encode("secret");
        var stored = new RoleProfileStore.StoredProfile(12L, "staff12", "staff@test.com", hash, true);
        when(repository.findForAuthentication("staff12")).thenReturn(java.util.Optional.of(stored));

        var response = service.authenticate(" staff12 ", "secret");

        assertThat(response).isEqualTo(new RoleProfileService.AuthenticationResponse("staff12", "staff@test.com", "STAFF"));
        verify(repository).updateLastLogin(12L);
    }

    @Test
    void rejectsBlankUnknownInactiveAndWrongPasswordCredentials() {
        assertUnauthorized(() -> service.authenticate(" ", "secret"));
        when(repository.findForAuthentication("missing")).thenReturn(java.util.Optional.empty());
        assertUnauthorized(() -> service.authenticate("missing", "secret"));
        when(repository.findForAuthentication("inactive")).thenReturn(java.util.Optional.of(
                new RoleProfileStore.StoredProfile(1L, "inactive", "i@test.com",
                        new BCryptPasswordEncoder().encode("secret"), false)));
        assertUnauthorized(() -> service.authenticate("inactive", "secret"));
        when(repository.findForAuthentication("staff")).thenReturn(java.util.Optional.of(
                new RoleProfileStore.StoredProfile(2L, "staff", "s@test.com",
                        new BCryptPasswordEncoder().encode("secret"), true)));
        assertUnauthorized(() -> service.authenticate("staff", "wrong"));
        verify(repository, never()).updateLastLogin(anyLong());
    }

    @Test
    void synchronizesUpsertAndDeleteAndRejectsInvalidEvents() {
        var event = new ProfileSyncEvent("event-1", "UPSERT", 9L, "STAFF", "Staff", "staff9", "s9@test.com",
                "hash", "555", "Others", true);
        service.sync(event);
        verify(repository).upsert(event);

        var deletion = new ProfileSyncEvent("event-2", "DELETE", 9L, "STAFF", null, null, null, null, null, null, false);
        service.sync(deletion);
        verify(repository).delete(9L);

        var wrongRole = new ProfileSyncEvent("event-3", "DELETE", 9L, "ADMIN", null, null, null, null, null, null, false);
        assertThatThrownBy(() -> service.sync(wrongRole)).isInstanceOf(IllegalArgumentException.class);
        var unsupported = new ProfileSyncEvent("event-4", "PATCH", 9L, "STAFF", null, null, null, null, null, null, false);
        assertThatThrownBy(() -> service.sync(unsupported)).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertUnauthorized(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
