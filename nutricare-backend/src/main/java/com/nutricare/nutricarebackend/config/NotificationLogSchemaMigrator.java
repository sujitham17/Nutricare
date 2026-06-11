package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationLogSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                create table if not exists notification_logs (
                    id bigint not null auto_increment primary key,
                    user_id bigint not null,
                    receiver_role varchar(30) not null,
                    channel varchar(20) not null,
                    title varchar(255) not null,
                    message varchar(2000) not null,
                    status varchar(30) not null,
                    twilio_sid varchar(100),
                    created_at datetime(6) not null
                )
                """);
        addColumnIfMissing("receiver_role", "alter table notification_logs add column receiver_role varchar(30) not null default 'USER'");
        addColumnIfMissing("title", "alter table notification_logs add column title varchar(255) not null default 'NutriCare Notification'");
        addColumnIfMissing("twilio_sid", "alter table notification_logs add column twilio_sid varchar(100)");
        addColumnIfMissing("created_at", "alter table notification_logs add column created_at datetime(6) null");
        if (columnExists("sent_at")) {
            jdbcTemplate.execute("update notification_logs set created_at = coalesce(created_at, sent_at, current_timestamp(6)) where created_at is null");
            jdbcTemplate.execute("alter table notification_logs modify sent_at datetime(6) null");
        } else {
            jdbcTemplate.execute("update notification_logs set created_at = coalesce(created_at, current_timestamp(6)) where created_at is null");
        }
        jdbcTemplate.execute("alter table notification_logs modify created_at datetime(6) not null");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        if (!columnExists(columnName)) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'notification_logs'
                          and column_name = ?
                        """,
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }
}
