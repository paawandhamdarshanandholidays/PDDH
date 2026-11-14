package com.pddh.roleprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

class RoleProfileConfigurationTest {
    @Test
    void createsRoleBeansAndKafkaTopicFromConfiguration() {
        RoleProfileConfiguration configuration = new RoleProfileConfiguration();
        RoleProfileProperties properties = configuration.roleProfileProperties(
                "staff", "STAFF", "/api/roles/staff", "pddh.profile.staff");
        RoleProfileStore store = configuration.roleProfileStore(mock(EntityManager.class));
        RoleProfileRepository repository = configuration.roleProfileRepository(store, properties);
        RoleProfileService service = configuration.roleProfileService(repository, properties);
        RoleIdentityService identities = mock(RoleIdentityService.class);

        assertThat(configuration.roleProfileController(service, identities)).isNotNull();
        assertThat(configuration.profileSyncListener(service, new ObjectMapper())).isNotNull();
        assertThat(configuration.roleProfileSchemaCompatibility(mock(JdbcTemplate.class), properties)).isNotNull();
        assertThat(configuration.roleProfileTopic(properties).name()).isEqualTo("pddh.profile.staff");
    }
}
