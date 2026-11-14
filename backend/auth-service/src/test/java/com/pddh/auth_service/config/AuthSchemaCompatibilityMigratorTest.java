package com.pddh.auth_service.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthSchemaCompatibilityMigratorTest {
    @Test
    void installsRoleScopedIdentityConstraintsWhenNoLegacyTablesExist() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(false);

        new AuthSchemaCompatibilityMigrator(jdbc).run(mock(ApplicationArguments.class));

        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("DROP CONSTRAINT"));
        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("uq_combined_roles_role_username_ci"));
        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("uq_combined_roles_role_email_ci"));
        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("uq_combined_roles_role_mobile_ci"));
        verify(jdbc).execute(
                org.mockito.ArgumentMatchers.contains("DROP INDEX IF EXISTS public.uq_combined_roles_single_ceo"));
        verify(jdbc, never()).execute(
                org.mockito.ArgumentMatchers.contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_combined_roles_single_ceo"));
    }

    @Test
    void copiesLegacyAggregateAndRoleTablesWhenPresent() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);

        new AuthSchemaCompatibilityMigrator(jdbc).run(mock(ApplicationArguments.class));

        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("INSERT INTO combined_roles"));
        verify(jdbc, times(2)).execute(org.mockito.ArgumentMatchers.contains("INSERT INTO users (identity_id"));
        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("FROM \"user\""));
        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("FROM user_table"));
        verify(jdbc).execute(org.mockito.ArgumentMatchers.contains("INSERT INTO staff (identity_id, role"));
        verify(jdbc, times(2)).execute(org.mockito.ArgumentMatchers.contains("'CUSTOMER'"));
    }
}
