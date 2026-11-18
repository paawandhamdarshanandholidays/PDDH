package com.pddh.travel_service.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingSchemaCompatibility implements SmartInitializingSingleton {
    private final JdbcTemplate jdbc;

    public BookingSchemaCompatibility(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterSingletonsInstantiated() {
        migrate();
    }

    public void migrate() {
        if (!tableExists()) {
            return;
        }
        if (!columnExists("customer_id")) {
            jdbc.execute("ALTER TABLE bookings ADD COLUMN customer_id VARCHAR(120)");
        }
        if (!columnExists("submission_fingerprint")) {
            jdbc.execute("ALTER TABLE bookings ADD COLUMN submission_fingerprint VARCHAR(64)");
        }
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_bookings_customer_daily_submission "
                + "ON bookings (customer_id, booking_date, submission_fingerprint) "
                + "WHERE customer_id IS NOT NULL AND submission_fingerprint IS NOT NULL");
    }

    private boolean tableExists() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, "bookings"));
    }

    private boolean columnExists(String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)",
                Boolean.class, "bookings", column));
    }
}
