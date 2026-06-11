package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentLifecycleSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer appointmentTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = 'appointments'",
                Integer.class
        );
        if (appointmentTableCount == null || appointmentTableCount == 0) {
            return;
        }

        ensureColumn("meeting_link", "varchar(1000)");
        ensureColumn("meeting_created_at", "datetime(6)");
        ensureColumn("meeting_status", "varchar(30) not null default 'NOT_CREATED'");
        ensureColumn("reminder_sent", "bit not null default 0");
        ensureColumn("sms_reminder_sent", "bit not null default 0");
        ensureColumn("whatsapp_reminder_sent", "bit not null default 0");
        ensureColumn("reminder_sent_at", "datetime(6)");
        ensureColumn("consultation_fee", "decimal(10,2)");
        ensureColumn("payment_status", "varchar(30) not null default 'PENDING'");
        ensureColumn("payment_id", "varchar(255)");
        ensureColumn("order_id", "varchar(255)");
        ensureColumn("paid_at", "datetime(6)");
        ensureColumn("refund_status", "varchar(30) not null default 'NOT_REQUIRED'");
        ensureColumn("refund_expected_by", "date");
        ensureColumn("cancelled_by", "bigint");
        ensureColumn("cancellation_reason", "varchar(1000)");
        ensureColumn("cancelled_at", "datetime(6)");

        jdbcTemplate.update("update appointments set status = 'CANCELLED' where status = 'REJECTED'");
        jdbcTemplate.update("update appointments set meeting_status = 'NOT_CREATED' where meeting_status is null");
        jdbcTemplate.update("update appointments set payment_status = 'PENDING' where payment_status is null");
        jdbcTemplate.update("update appointments set refund_status = 'NOT_REQUIRED' where refund_status is null");
        jdbcTemplate.update("""
                update appointments
                set meeting_link = concat('https://meet.jit.si/NutriCare-Appointment-', id),
                    meeting_status = 'SCHEDULED',
                    meeting_created_at = coalesce(meeting_created_at, now(6))
                where meeting_link like 'https://meet.google.com/%'
                """);
        jdbcTemplate.update("update appointments set meeting_status = 'SCHEDULED' where meeting_link is not null and meeting_link <> '' and meeting_status = 'NOT_CREATED'");

        Integer paymentTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = 'consultation_payments'",
                Integer.class
        );
        if (paymentTableCount != null && paymentTableCount > 0) {
            ensurePaymentColumn("paid_at", "datetime(6)");
        }
    }

    private void ensureColumn(String columnName, String definition) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'appointments'
                          and column_name = ?
                        """,
                Integer.class,
                columnName
        );
        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute("alter table appointments add column " + columnName + " " + definition);
        }
    }

    private void ensurePaymentColumn(String columnName, String definition) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'consultation_payments'
                          and column_name = ?
                        """,
                Integer.class,
                columnName
        );
        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute("alter table consultation_payments add column " + columnName + " " + definition);
        }
    }
}
