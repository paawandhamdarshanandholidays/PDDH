package com.pddh.roleprofile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RoleProfileSchemaCompatibilityTest {
    @Test
    void backfillsExistingRowsBeforeApplyingRequiredColumnConstraints() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);

        new RoleProfileSchemaCompatibility(jdbc, "users", "admin", "manager", "ceo", "staff").migrateAll();

        verify(jdbc).execute(contains("UPDATE users AS profile"));
        verify(jdbc, times(5)).execute(contains("SET password = COALESCE(password, password_hash)"));
        verify(jdbc, times(5)).execute(contains("SET password = password_hash"));
        verify(jdbc).update("UPDATE users SET role = ?", "CUSTOMER");
        verify(jdbc).update("UPDATE admin SET role = ?", "ADMIN");
        verify(jdbc).update("UPDATE manager SET role = ?", "MANAGER");
        verify(jdbc).update("UPDATE ceo SET role = ?", "CEO");
        verify(jdbc).update("UPDATE staff SET role = ?", "STAFF");
        verify(jdbc, times(20)).execute(contains("ALTER TABLE"));
    }

    @Test
    void addsLegacyPasswordColumnWhenItIsMissing() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(true, false);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);

        new RoleProfileSchemaCompatibility(jdbc, "users").migrateAll();

        verify(jdbc).execute(contains("ADD COLUMN password VARCHAR(255)"));
        verify(jdbc).execute(contains("ADD COLUMN role VARCHAR(50)"));
        verify(jdbc).update("UPDATE users SET role = ?", "CUSTOMER");
        verify(jdbc, times(6)).execute(contains("ALTER TABLE"));
    }

    @Test
    void failsClearlyWhenExistingProfilesCannotBeMatchedToCredentials() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        assertThatThrownBy(() -> new RoleProfileSchemaCompatibility(jdbc, "users").migrateAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing password hashes could not be matched to combined_roles");

        verify(jdbc, times(4)).execute(anyString());
    }
}
