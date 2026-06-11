package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!tableExists("users")) {
            return;
        }

        if (columnExists("users", "status")) {
            jdbcTemplate.update("update users set status = 'ACTIVE' where status is null");
            jdbcTemplate.update("update users set status = 'ACTIVE' where status = 'APPROVED'");
        }

        if (columnExists("users", "created_at")) {
            jdbcTemplate.update("update users set created_at = current_timestamp(6) where created_at is null");
        }

        if (columnExists("users", "sugar_level")) {
            jdbcTemplate.update("alter table users modify sugar_level varchar(50)");
        }

        ensureColumn("users", "whatsapp_notifications_enabled", "bit not null default 1");
        ensureColumn("users", "sms_notifications_enabled", "bit not null default 1");
        ensureColumn("users", "appointment_completed", "bit not null default 0");
        ensureColumn("users", "onboarding_completed", "bit not null default 0");
        ensureColumn("users", "profile_image", "varchar(1000)");
        ensureColumn("users", "email_verified", "bit not null default 0");
        ensureColumn("users", "provider", "varchar(50) not null default 'LOCAL'");

        // Backfill legacy users to be verified standard users
        jdbcTemplate.update("update users set email_verified = 1 where email_verified is null");
        jdbcTemplate.update("update users set provider = 'LOCAL' where provider is null");

        backfillOnboardingState();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = database()
                          and table_name = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = ?
                          and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("alter table " + tableName + " add column " + columnName + " " + definition);
        }
    }

    private void backfillOnboardingState() {
        if (!tableExists("subscription_transactions") || !tableExists("appointments")) {
            return;
        }

        jdbcTemplate.update(
                """
                        update users u
                        set subscription_active =
                            case
                                when exists (
                                    select 1
                                    from subscription_transactions st
                                    where st.user_id = u.id
                                      and st.subscription_status = 'ACTIVE'
                                      and st.end_date >= current_date()
                                ) then 1
                                else 0
                            end
                        where u.role in ('USER', 'DIETICIAN')
                        """
        );

        jdbcTemplate.update(
                """
                        update users u
                        set appointment_completed =
                            case
                                when exists (
                                    select 1
                                    from appointments a
                                    where a.user_id = u.id
                                      and a.status <> 'CANCELLED'
                                ) then 1
                                else 0
                            end
                        where u.role = 'USER'
                        """
        );

        jdbcTemplate.update(
                """
                        update users u
                        set appointment_completed =
                            case
                                when exists (
                                    select 1
                                    from appointments a
                                    where a.dietician_id = u.id
                                      and a.status <> 'CANCELLED'
                                ) then 1
                                else 0
                            end
                        where u.role = 'DIETICIAN'
                        """
        );

        jdbcTemplate.update(
                """
                        update users
                        set onboarding_completed =
                            case
                                when role not in ('USER', 'DIETICIAN') then 1
                                when profile_setup_completed = 1
                                  and subscription_active = 1
                                  and appointment_completed = 1 then 1
                                else 0
                            end
                        """
        );
    }
}
