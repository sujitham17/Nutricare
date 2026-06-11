package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionTransactionSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!tableExists("subscription_transactions")) {
            return;
        }

        ensureColumn("subscription_transactions", "razorpay_order_id", "varchar(255)");
        ensureColumn("subscription_transactions", "razorpay_signature", "varchar(500)");
        ensureColumn("subscription_transactions", "deleted", "bit not null default 0");
        ensureColumn("subscription_transactions", "deleted_at", "datetime(6)");

        if (tableExists("payment_transactions")) {
            migratePaymentTransactions();
        }
        if (tableExists("user_subscriptions")) {
            migrateUserSubscriptions();
        }
        if (tableExists("dietician_subscriptions")) {
            migrateDieticianSubscriptions();
        }
    }

    private void migratePaymentTransactions() {
        jdbcTemplate.update("""
                insert into subscription_transactions (
                    user_id, plan_id, role_type, amount, status, subscription_status,
                    payment_provider, provider_payment_id, start_date, end_date, created_at
                )
                select
                    pt.user_id,
                    pt.plan_id,
                    u.role,
                    pt.amount,
                    pt.payment_status,
                    case when pt.payment_status = 'SUCCESS' then 'ACTIVE' else 'INACTIVE' end,
                    pt.payment_provider,
                    pt.provider_payment_id,
                    null,
                    null,
                    pt.created_at
                from payment_transactions pt
                join users u on u.id = pt.user_id
                where not exists (
                    select 1
                    from subscription_transactions st
                    where st.user_id = pt.user_id
                      and st.plan_id = pt.plan_id
                      and st.created_at = pt.created_at
                      and st.amount = pt.amount
                )
                """);
    }

    private void migrateUserSubscriptions() {
        jdbcTemplate.update("""
                insert into subscription_transactions (
                    user_id, plan_id, role_type, amount, status, subscription_status,
                    payment_provider, provider_payment_id, start_date, end_date, created_at
                )
                select
                    us.user_id,
                    us.plan_id,
                    'USER',
                    sp.price,
                    'SUCCESS',
                    us.status,
                    'LEGACY',
                    concat('legacy_user_subscription_', us.id),
                    us.start_date,
                    us.end_date,
                    us.created_at
                from user_subscriptions us
                join subscription_plans sp on sp.id = us.plan_id
                where not exists (
                    select 1
                    from subscription_transactions st
                    where st.provider_payment_id = concat('legacy_user_subscription_', us.id)
                )
                """);
    }

    private void migrateDieticianSubscriptions() {
        jdbcTemplate.update("""
                insert into subscription_transactions (
                    user_id, plan_id, role_type, amount, status, subscription_status,
                    payment_provider, provider_payment_id, start_date, end_date, created_at
                )
                select
                    ds.dietician_id,
                    ds.plan_id,
                    'DIETICIAN',
                    sp.price,
                    'SUCCESS',
                    ds.status,
                    'LEGACY',
                    concat('legacy_dietician_subscription_', ds.id),
                    ds.start_date,
                    ds.end_date,
                    ds.created_at
                from dietician_subscriptions ds
                join subscription_plans sp on sp.id = ds.plan_id
                where not exists (
                    select 1
                    from subscription_transactions st
                    where st.provider_payment_id = concat('legacy_dietician_subscription_', ds.id)
                )
                """);
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

    private void ensureColumn(String tableName, String columnName, String definition) {
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
        if (count == null || count == 0) {
            jdbcTemplate.execute("alter table " + tableName + " add column " + columnName + " " + definition);
        }
    }
}
