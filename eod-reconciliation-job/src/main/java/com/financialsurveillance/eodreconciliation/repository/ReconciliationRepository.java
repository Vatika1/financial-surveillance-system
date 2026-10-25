package com.financialsurveillance.eodreconciliation.repository;

import com.financialsurveillance.eodreconciliation.dto.AdvisorDailyTotal;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Reconciliation is activity-monitor's own end-of-day process, deployed separately,
// so it reads and writes activity_monitor.advisor_daily_activity directly.
@Repository
@RequiredArgsConstructor
public class ReconciliationRepository {
    private static final String SELECT_FOR_UPDATE = """
            SELECT advisor_id, trade_count, total_value
            FROM activity_monitor.advisor_daily_activity
            WHERE advisor_id = ? AND trade_date = ?
            FOR UPDATE
            """;

    private static final String MARK_RECONCILED = """
            UPDATE activity_monitor.advisor_daily_activity
            SET reconciled = TRUE, updated_at = NOW()
            WHERE advisor_id = ? AND trade_date = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public Optional<AdvisorDailyTotal> findForUpdate(String advisorId, LocalDate tradeDate) {
        List<AdvisorDailyTotal> rows = jdbcTemplate.query(SELECT_FOR_UPDATE,
                (rs, i) -> new AdvisorDailyTotal(
                        rs.getString("advisor_id"),
                        rs.getInt("trade_count"),
                        rs.getBigDecimal("total_value")),
                advisorId, Date.valueOf(tradeDate));
        return rows.stream().findFirst();
    }

    public void markReconciled(List<String> advisorIds, LocalDate tradeDate) {
        Date date = Date.valueOf(tradeDate);
        jdbcTemplate.batchUpdate(MARK_RECONCILED, advisorIds, advisorIds.size(),
                (ps, advisorId) -> {
                    ps.setString(1, advisorId);
                    ps.setDate(2, date);
                });
    }
}
