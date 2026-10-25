package com.financialsurveillance.activitymonitor.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class AdvisorDailyActivityRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO activity_monitor.advisor_daily_activity
                (advisor_id, trade_date, trade_count, total_value, updated_at)
            VALUES (?, ?, ?, ?, NOW())
            ON CONFLICT (advisor_id, trade_date) DO UPDATE
            SET trade_count = advisor_daily_activity.trade_count + EXCLUDED.trade_count,
                total_value = advisor_daily_activity.total_value + EXCLUDED.total_value,
                updated_at  = NOW()
            """;

    private final JdbcTemplate jdbcTemplate;

    public void recordTrades(String advisorId, LocalDate tradeDate, int tradeCount, BigDecimal totalValue) {
        jdbcTemplate.update(UPSERT_SQL, advisorId, tradeDate, tradeCount, totalValue);
    }
}