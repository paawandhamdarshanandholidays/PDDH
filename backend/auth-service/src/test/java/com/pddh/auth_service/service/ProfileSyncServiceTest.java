package com.pddh.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.auth_service.entities.ProfileSyncOutbox;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.ProfileSyncOutboxRepository;
import com.pddh.roleprofile.RoleProfileStore;

class ProfileSyncServiceTest {
    private ProfileSyncOutboxRepository outbox;
    private ObjectMapper mapper;
    private RoleProfileStore profiles;
    private ProfileSyncService service;

    @BeforeEach
    void setUp() {
        outbox = mock(ProfileSyncOutboxRepository.class);
        mapper = mock(ObjectMapper.class);
        profiles = mock(RoleProfileStore.class);
        service = new ProfileSyncService(outbox, mapper, profiles);
    }

    @Test
    void writesRoleRowAndEnqueuesKafkaUpsertEvent() throws Exception {
        User user = user("ADMIN");
        when(mapper.writeValueAsString(any())).thenReturn("{\"operation\":\"UPSERT\"}");

        service.upsert(user);

        verify(profiles).upsert(eq("admin"), any(com.pddh.roleprofile.ProfileSyncEvent.class));
        ArgumentCaptor<ProfileSyncOutbox> event = ArgumentCaptor.forClass(ProfileSyncOutbox.class);
        verify(outbox).save(event.capture());
        assertThat(event.getValue().getRoutingKey()).isEqualTo("pddh.profile.admin");
        assertThat(event.getValue().getPayload()).contains("UPSERT");
        assertThat(event.getValue().getAttempts()).isZero();
        assertThat(event.getValue().getNextAttemptAt()).isNotNull();
    }

    @Test
    void deletesRoleRowAndEnqueuesDeleteEvent() throws Exception {
        User user = user("CEO");
        when(mapper.writeValueAsString(any())).thenReturn("{\"operation\":\"DELETE\"}");

        service.delete(user);

        verify(profiles).delete("ceo", 10L);
        ArgumentCaptor<ProfileSyncOutbox> event = ArgumentCaptor.forClass(ProfileSyncOutbox.class);
        verify(outbox).save(event.capture());
        assertThat(event.getValue().getRoutingKey()).isEqualTo("pddh.profile.ceo");
        assertThat(event.getValue().getPayload()).contains("DELETE");
    }

    @Test
    void rejectsUnknownRolesAndConvertsSerializationErrors() throws Exception {
        assertThatThrownBy(() -> service.upsert(user("OTHER"))).isInstanceOf(IllegalArgumentException.class);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("bad payload") { });

        assertThatThrownBy(() -> service.delete(user("STAFF")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }

    private User user(String role) {
        User user = new User();
        user.setId(10L);
        user.setName("Person");
        user.setUsername("person10");
        user.setEmail("person@test.com");
        user.setPassword("hash");
        user.setRole(role);
        user.setMobile("555");
        user.setGender("Others");
        user.setActive(true);
        return user;
    }
}
