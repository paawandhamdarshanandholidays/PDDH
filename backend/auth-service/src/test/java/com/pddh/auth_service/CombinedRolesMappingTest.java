package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.pddh.auth_service.entities.User;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class CombinedRolesMappingTest {
    @Test
    void mapsTheAccountMasterEntityToCombinedRoles() {
        assertThat(User.class.getAnnotation(Table.class).name()).isEqualTo("combined_roles");
    }

    @Test
    void scopesIdentityConstraintsToTheRole() {
        UniqueConstraint[] constraints = User.class.getAnnotation(Table.class).uniqueConstraints();

        assertThat(constraints).hasSize(3);
        assertThat(constraints).extracting(UniqueConstraint::columnNames)
                .containsExactlyInAnyOrder(
                        new String[] { "role", "username" },
                        new String[] { "role", "email" },
                        new String[] { "role", "mobile" });
    }
}
