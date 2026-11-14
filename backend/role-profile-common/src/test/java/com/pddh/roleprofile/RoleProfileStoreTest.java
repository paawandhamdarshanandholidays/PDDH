package com.pddh.roleprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.pddh.roleprofile.entities.AdminRoleProfile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

class RoleProfileStoreTest {
    @Test
    @SuppressWarnings("unchecked")
    void persistsCanonicalRoleFromProfileSyncEvent() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<AdminRoleProfile> existingProfiles = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(AdminRoleProfile.class))).thenReturn(existingProfiles);
        when(existingProfiles.setParameter(anyString(), eq(12L))).thenReturn(existingProfiles);
        when(existingProfiles.getResultStream()).thenReturn(Stream.empty());
        RoleProfileStore store = new RoleProfileStore(entityManager);
        ProfileSyncEvent event = new ProfileSyncEvent("event-1", "UPSERT", 12L, " admin ",
                "Admin", "admin12", "admin@test.com", "hash", "555", "Other", true);

        store.upsert("admin", event);

        ArgumentCaptor<AdminRoleProfile> profile = ArgumentCaptor.forClass(AdminRoleProfile.class);
        verify(entityManager).persist(profile.capture());
        assertThat(profile.getValue().getRole()).isEqualTo("ADMIN");
    }
}
