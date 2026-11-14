package com.pddh.roleprofile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ProfileSyncListenerTest {
    @Test
    void deserializesKafkaMessageAndDelegatesToProfileService() throws Exception {
        RoleProfileService service = mock(RoleProfileService.class);
        ProfileSyncListener listener = new ProfileSyncListener(service, new ObjectMapper());

        listener.receive("""
                {"eventId":"evt-1","operation":"DELETE","identityId":9,"role":"STAFF","active":false}
                """);

        verify(service).sync(new ProfileSyncEvent("evt-1", "DELETE", 9L, "STAFF", null, null, null,
                null, null, null, false));
    }

    @Test
    void reportsMalformedPayloadAsDeserializationError() {
        ProfileSyncListener listener = new ProfileSyncListener(mock(RoleProfileService.class), new ObjectMapper());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.receive("{"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
}
