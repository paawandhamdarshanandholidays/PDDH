package com.pddh.auth_service.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.pddh.roleprofile.RoleProfileSchemaCompatibility;

/** Copies data from legacy table names after Hibernate has created the current entity schema. */
@Component
public class AuthSchemaCompatibilityMigrator implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthSchemaCompatibilityMigrator.class);
    private static final List<String> ROLES = List.of("user", "admin", "manager", "ceo", "staff");
    private final JdbcTemplate jdbc;

    public AuthSchemaCompatibilityMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        removeGlobalCombinedRoleConstraints();
        migrateLegacyUsers();
        normalizeLegacyCustomerRoles();
        ROLES.forEach(this::migrateLegacyRole);
        addRoleScopedCombinedRoleConstraints();
        new RoleProfileSchemaCompatibility(jdbc, "users", "admin", "manager", "ceo", "staff").migrateAll();
    }

    void normalizeLegacyCustomerRoles() {
        int normalized = jdbc.update("""
                UPDATE combined_roles legacy
                SET role = 'CUSTOMER'
                WHERE upper(trim(legacy.role)) = 'USER'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM combined_roles customer
                      WHERE customer.id <> legacy.id
                        AND upper(trim(customer.role)) = 'CUSTOMER'
                        AND (
                            lower(customer.username) = lower(legacy.username)
                            OR lower(customer.email) = lower(legacy.email)
                            OR (customer.mobile IS NOT NULL AND legacy.mobile IS NOT NULL
                                AND lower(customer.mobile) = lower(legacy.mobile))
                        )
                  )
                """);
        if (normalized > 0) {
            LOGGER.info("Normalized {} legacy USER account(s) to the CUSTOMER role", normalized);
        }

        Integer unresolved = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM combined_roles legacy
                WHERE upper(trim(legacy.role)) = 'USER'
                  AND EXISTS (
                      SELECT 1
                      FROM combined_roles customer
                      WHERE customer.id <> legacy.id
                        AND upper(trim(customer.role)) = 'CUSTOMER'
                        AND (
                            lower(customer.username) = lower(legacy.username)
                            OR lower(customer.email) = lower(legacy.email)
                            OR (customer.mobile IS NOT NULL AND legacy.mobile IS NOT NULL
                                AND lower(customer.mobile) = lower(legacy.mobile))
                        )
                  )
                """, Integer.class);
        if (unresolved != null && unresolved > 0) {
            LOGGER.warn("Left {} legacy USER account(s) unchanged because a CUSTOMER account already uses "
                    + "the same username, email, or mobile", unresolved);
        }
    }

    private void removeGlobalCombinedRoleConstraints() {
        jdbc.execute("""
                DO $$
                DECLARE constraint_row RECORD;
                BEGIN
                    FOR constraint_row IN
                        SELECT tc.constraint_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.constraint_column_usage ccu
                          ON tc.constraint_catalog = ccu.constraint_catalog
                         AND tc.constraint_schema = ccu.constraint_schema
                         AND tc.constraint_name = ccu.constraint_name
                        WHERE tc.table_schema = 'public'
                          AND tc.table_name = 'combined_roles'
                          AND tc.constraint_type = 'UNIQUE'
                        GROUP BY tc.constraint_name
                        HAVING COUNT(*) = 1
                           AND BOOL_OR(ccu.column_name IN ('username', 'email'))
                    LOOP
                        EXECUTE format('ALTER TABLE public.combined_roles DROP CONSTRAINT %I',
                                       constraint_row.constraint_name);
                    END LOOP;
                END $$""");
    }

    private void addRoleScopedCombinedRoleConstraints() {
        jdbc.execute("DROP INDEX IF EXISTS public.uq_combined_roles_single_ceo");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_combined_roles_role_username_ci "
                + "ON public.combined_roles (lower(role), lower(username))");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_combined_roles_role_email_ci "
                + "ON public.combined_roles (lower(role), lower(email))");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_combined_roles_role_mobile_ci "
                + "ON public.combined_roles (lower(role), lower(mobile)) WHERE mobile IS NOT NULL");
    }

    private void migrateLegacyUsers() {
        if (tableExists("users_data")) {
            jdbc.execute("INSERT INTO combined_roles (id, name, username, email, password, role, mobile, gender, active, last_login_at) "
                    + "SELECT id, name, username, email, password, role, mobile, gender, active, last_login_at "
                    + "FROM users_data ON CONFLICT DO NOTHING");
            jdbc.execute("SELECT setval(pg_get_serial_sequence('combined_roles','id'), "
                    + "COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM combined_roles");
            LOGGER.info("Copied legacy users_data rows into combined_roles where no matching account existed");
        }
        migrateLegacyRoleTable("user", "users");
    }

    private void migrateLegacyRole(String role) {
        String legacyTable = role + "_table";
        if (!tableExists(legacyTable)) return;
        String targetTable = role.equals("user") ? "users" : role;
        String name = sourceColumn(legacyTable, "name", "''");
        String username = sourceColumn(legacyTable, "username", "''");
        String email = sourceColumn(legacyTable, "email", "''");
        String password = sourceColumn(legacyTable, "password_hash", "''");
        String mobile = sourceColumn(legacyTable, "mobile", "NULL");
        String gender = sourceColumn(legacyTable, "gender", "NULL");
        String active = sourceColumn(legacyTable, "active", "TRUE");
        String lastLogin = sourceColumn(legacyTable, "last_login_at", "NULL");
        String created = sourceColumn(legacyTable, "created_at", "CURRENT_TIMESTAMP");
        String updated = sourceColumn(legacyTable, "updated_at", "CURRENT_TIMESTAMP");
        jdbc.execute("INSERT INTO " + targetTable + " (identity_id, role, name, username, email, password_hash, mobile, gender, active, last_login_at, created_at, updated_at) "
                + "SELECT identity_id, '" + roleForTable(targetTable) + "', " + name + ", " + username + ", " + email + ", " + password + ", "
                + mobile + ", " + gender + ", " + active + ", " + lastLogin + ", " + created + ", " + updated
                + " FROM " + legacyTable + " ON CONFLICT DO NOTHING");
        LOGGER.info("Copied legacy {}_table rows into {} role table where no matching identity existed", role, targetTable);
    }

    private void migrateLegacyRoleTable(String sourceTable, String targetTable) {
        if (!tableExists(sourceTable)) return;
        String quotedSource = sourceTable.equals("user") ? "\"user\"" : sourceTable;
        jdbc.execute("INSERT INTO " + targetTable + " (identity_id, role, name, username, email, password_hash, mobile, gender, active, last_login_at, created_at, updated_at) "
                + "SELECT identity_id, '" + roleForTable(targetTable) + "', name, username, email, password_hash, mobile, gender, active, last_login_at, created_at, updated_at "
                + "FROM " + quotedSource + " ON CONFLICT DO NOTHING");
        LOGGER.info("Copied legacy {} role rows into {} where no matching identity existed", sourceTable, targetTable);
    }

    private String roleForTable(String table) {
        return switch (table) {
            case "users" -> "CUSTOMER";
            case "admin" -> "ADMIN";
            case "manager" -> "MANAGER";
            case "ceo" -> "CEO";
            case "staff" -> "STAFF";
            default -> throw new IllegalArgumentException("Unsupported role profile table");
        };
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, table));
    }

    private String sourceColumn(String table, String column, String fallback) {
        boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)",
                Boolean.class, table, column));
        return exists ? column : fallback;
    }
}
