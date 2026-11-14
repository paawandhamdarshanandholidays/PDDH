package com.pddh.travel_service.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class BookingSchemaCompatibilityTest {
    @Test
    void addsMissingCustomerAndFingerprintColumnsAndUniqueIndex() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(true, false, false);

        new BookingSchemaCompatibility(jdbc).migrate();

        verify(jdbc).execute("ALTER TABLE bookings ADD COLUMN customer_id VARCHAR(120)");
        verify(jdbc).execute("ALTER TABLE bookings ADD COLUMN submission_fingerprint VARCHAR(64)");
        verify(jdbc).execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_bookings_customer_daily_submission "
                + "ON bookings (customer_id, booking_date, submission_fingerprint) "
                + "WHERE customer_id IS NOT NULL AND submission_fingerprint IS NOT NULL");
    }

    @Test
    void leavesDatabaseUntouchedWhenBookingsTableDoesNotExist() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(false);

        new BookingSchemaCompatibility(jdbc).migrate();

        verify(jdbc, never()).execute(anyString());
    }
}
