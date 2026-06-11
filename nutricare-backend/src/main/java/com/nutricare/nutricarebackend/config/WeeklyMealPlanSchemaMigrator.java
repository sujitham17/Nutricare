package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyMealPlanSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer legacyIndexCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.statistics where table_schema = database() and table_name = 'diet_plan_meals' and index_name = 'uk_diet_plan_meal_type'",
                Integer.class
        );

        if (legacyIndexCount != null && legacyIndexCount > 0) {
            jdbcTemplate.execute("alter table diet_plan_meals drop index uk_diet_plan_meal_type");
        }
    }
}
