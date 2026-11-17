package com.pddh.roleprofile;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class RoleProfileSchemaCompatibility implements SmartInitializingSingleton {
    private static final Map<String, String> ROLE_TABLES = Map.of(
            "users", "CUSTOMER",
            "admin", "ADMIN",
            "manager", "MANAGER",
            "ceo", "CEO",
            "staff", "STAFF");
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final JdbcTemplate jdbc;
    private final List<String> roleTables;

    public RoleProfileSchemaCompatibility(JdbcTemplate jdbc, String... roleTables) {
        this.jdbc = jdbc;
        this.roleTables = List.of(roleTables);
        if (this.roleTables.isEmpty() || this.roleTables.stream().anyMatch(table -> !ROLE_TABLES.containsKey(table))) {
            throw new IllegalArgumentException("Unsupported role profile table configuration");
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        migrateAll();
    }

    public void migrateAll() {
        for (String table : roleTables) {
            if (tableExists(table)) {
                migrate(table);
            }
        }
    }

    private void migrate(String table) {
        if (tableExists("combined_roles")) {
            jdbc.execute("UPDATE " + table + " AS profile "
                    + "SET identity_id = COALESCE(profile.identity_id, account.id), "
                    + "password_hash = COALESCE(profile.password_hash, account.password) "
                    + "FROM combined_roles AS account "
                    + "WHERE (profile.identity_id IS NULL OR profile.password_hash IS NULL) "
                    + "AND (profile.id = account.id "
                    + "OR lower(profile.username) = lower(account.username) "
                    + "OR lower(profile.email) = lower(account.email))");
        }

        if (!columnExists(table, "password")) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN password VARCHAR(255)");
        }
        if (!columnExists(table, "role")) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN role VARCHAR(50)");
        }
        jdbc.update("UPDATE " + table + " SET role = ?", ROLE_TABLES.get(table));
        jdbc.execute("UPDATE " + table + " SET password = COALESCE(password, password_hash), "
                + "password_hash = COALESCE(password_hash, password)");
        jdbc.execute("UPDATE " + table + " SET identity_id = id WHERE identity_id IS NULL");
        normalizeLegacyPasswords(table);
        jdbc.execute("UPDATE " + table + " SET password = password_hash "
                + "WHERE password IS DISTINCT FROM password_hash");

        Long incompleteProfiles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE identity_id IS NULL OR password_hash IS NULL",
                Long.class);
        if (incompleteProfiles != null && incompleteProfiles > 0) {
            throw new IllegalStateException("Cannot migrate " + incompleteProfiles
                    + " existing rows in role table " + table
                    + ": missing password hashes could not be matched to combined_roles");
        }

        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN identity_id SET NOT NULL");
        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN password_hash SET NOT NULL");
        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN password SET NOT NULL");
        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN role SET NOT NULL");
    }

    private void normalizeLegacyPasswords(String table) {
        List<LegacyPassword> passwords = jdbc.query(
                "SELECT id, password_hash FROM " + table
                        + " WHERE password_hash IS NOT NULL AND password_hash NOT LIKE '$2%'",
                (RowMapper<LegacyPassword>) (result, rowNumber) ->
                        new LegacyPassword(result.getLong("id"), result.getString("password_hash")));
        for (LegacyPassword password : passwords) {
            jdbc.update("UPDATE " + table + " SET password_hash = ? WHERE id = ?",
                    PASSWORD_ENCODER.encode(password.value), password.id);
        }
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, table));
    }

    private boolean columnExists(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)",
                Boolean.class, table, column));
    }

    private record LegacyPassword(long id, String value) { }
}
