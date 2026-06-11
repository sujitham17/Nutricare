package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                create table if not exists ratings (
                    id bigint not null auto_increment,
                    appointment_id bigint not null,
                    user_id bigint not null,
                    dietician_id bigint not null,
                    rating int not null,
                    review varchar(1000),
                    created_at datetime(6) not null default current_timestamp(6),
                    primary key (id)
                )
                """);

        ensureUniqueIndex("ratings", "uk_rating_appointment", "appointment_id");
        ensureIndex("ratings", "idx_ratings_user", "user_id");
        ensureIndex("ratings", "idx_ratings_dietician", "dietician_id");
    }

    private void ensureUniqueIndex(String tableName, String indexName, String columnName) {
        if (!indexExists(tableName, indexName)) {
            try {
                jdbcTemplate.execute("alter table " + tableName + " add constraint " + indexName + " unique (" + columnName + ")");
            } catch (DataAccessException ex) {
                log.warn("Unable to create unique rating index {}. New duplicate ratings remain blocked by service validation.", indexName);
            }
        }
    }

    private void ensureIndex(String tableName, String indexName, String columnName) {
        if (!indexExists(tableName, indexName)) {
            jdbcTemplate.execute("create index " + indexName + " on " + tableName + " (" + columnName + ")");
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.statistics
                        where table_schema = database()
                          and table_name = ?
                          and index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}
