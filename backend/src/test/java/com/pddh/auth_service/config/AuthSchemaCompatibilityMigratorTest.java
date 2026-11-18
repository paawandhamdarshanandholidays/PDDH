package com.pddh.auth_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AuthSchemaCompatibilityMigratorTest {
    @Test
    void normalizesLegacyUserRoleWhenNoCustomerIdentityConflicts() {
        JdbcTemplate jdbc = createJdbcTemplate("normalize_legacy_customer");
        insertAccount(jdbc, 1, "customer-name", "customer@example.com", "111", "user");

        new AuthSchemaCompatibilityMigrator(jdbc).normalizeLegacyCustomerRoles();

        assertEquals("CUSTOMER", findRole(jdbc, 1));
    }

    @Test
    void preservesLegacyUserRoleWhenCustomerIdentityWouldConflict() {
        JdbcTemplate jdbc = createJdbcTemplate("preserve_customer_conflict");
        insertAccount(jdbc, 1, "legacy-name", "shared@example.com", "111", "USER");
        insertAccount(jdbc, 2, "customer-name", "SHARED@example.com", "222", "CUSTOMER");

        new AuthSchemaCompatibilityMigrator(jdbc).normalizeLegacyCustomerRoles();

        assertEquals("USER", findRole(jdbc, 1));
        assertEquals("CUSTOMER", findRole(jdbc, 2));
    }

    private JdbcTemplate createJdbcTemplate(String databaseName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE combined_roles (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(80) NOT NULL,
                    email VARCHAR(120) NOT NULL,
                    mobile VARCHAR(30),
                    role VARCHAR(30) NOT NULL
                )
                """);
        return jdbc;
    }

    private void insertAccount(JdbcTemplate jdbc, long id, String username, String email,
            String mobile, String role) {
        jdbc.update("INSERT INTO combined_roles (id, username, email, mobile, role) VALUES (?, ?, ?, ?, ?)",
                id, username, email, mobile, role);
    }

    private String findRole(JdbcTemplate jdbc, long id) {
        return jdbc.queryForObject("SELECT role FROM combined_roles WHERE id = ?", String.class, id);
    }
}
