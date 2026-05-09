package com.example.securityanalyze.research.infrastructure;

import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.StockFundamentalMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class StockFundamentalMetricsRepositoryImpl implements StockFundamentalMetricsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<StockFundamentalMetrics> ROW_MAPPER = new RowMapper<>() {
        @Override
        public StockFundamentalMetrics mapRow(ResultSet rs, int rowNum) throws SQLException {
            StockFundamentalMetrics m = new StockFundamentalMetrics();
            m.setId(rs.getLong("id"));
            m.setStockCode(rs.getString("stock_code"));
            m.setReportYear(rs.getInt("report_year"));
            m.setRevenueYoy(rs.getBigDecimal("revenue_yoy"));
            m.setProfitYoy(rs.getBigDecimal("profit_yoy"));
            m.setAssetGrowthRate(rs.getBigDecimal("asset_growth_rate"));
            m.setRoe(rs.getBigDecimal("roe"));
            m.setRoa(rs.getBigDecimal("roa"));
            m.setAssetTurnover(rs.getBigDecimal("asset_turnover"));
            m.setEquityMultiplier(rs.getBigDecimal("equity_multiplier"));
            m.setCurrentRatio(rs.getBigDecimal("current_ratio"));
            m.setQuickRatio(rs.getBigDecimal("quick_ratio"));
            m.setCashflowProfitRatio(rs.getBigDecimal("cashflow_profit_ratio"));
            m.setPeriodExpenseRate(rs.getBigDecimal("period_expense_rate"));
            m.setIsDeleted(rs.getBoolean("is_deleted"));
            m.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
            m.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            m.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return m;
        }

        private LocalDateTime toLocalDateTime(Timestamp ts) {
            return ts != null ? ts.toLocalDateTime() : null;
        }
    };

    @Override
    public Optional<StockFundamentalMetrics> findByStockCodeAndYear(String stockCode, int reportYear) {
        String sql = """
                SELECT * FROM stock_fundamental_metrics
                WHERE stock_code = :stockCode
                  AND report_year = :reportYear
                  AND is_deleted = FALSE
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("reportYear", reportYear);

        List<StockFundamentalMetrics> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<StockFundamentalMetrics> findByStockCode(String stockCode, int limit) {
        String sql = """
                SELECT * FROM stock_fundamental_metrics
                WHERE stock_code = :stockCode
                  AND is_deleted = FALSE
                ORDER BY report_year DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("limit", limit);

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public void batchUpsert(List<StockFundamentalMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO stock_fundamental_metrics (
                    stock_code, report_year,
                    revenue_yoy, profit_yoy, asset_growth_rate,
                    roe, roa, asset_turnover, equity_multiplier,
                    current_ratio, quick_ratio,
                    cashflow_profit_ratio, period_expense_rate,
                    is_deleted, deleted_at, created_at, updated_at
                ) VALUES (
                    :stockCode, :reportYear,
                    :revenueYoy, :profitYoy, :assetGrowthRate,
                    :roe, :roa, :assetTurnover, :equityMultiplier,
                    :currentRatio, :quickRatio,
                    :cashflowProfitRatio, :periodExpenseRate,
                    FALSE, NULL, NOW(), NOW()
                )
                ON CONFLICT (stock_code, report_year) DO UPDATE SET
                    revenue_yoy = EXCLUDED.revenue_yoy,
                    profit_yoy = EXCLUDED.profit_yoy,
                    asset_growth_rate = EXCLUDED.asset_growth_rate,
                    roe = EXCLUDED.roe,
                    roa = EXCLUDED.roa,
                    asset_turnover = EXCLUDED.asset_turnover,
                    equity_multiplier = EXCLUDED.equity_multiplier,
                    current_ratio = EXCLUDED.current_ratio,
                    quick_ratio = EXCLUDED.quick_ratio,
                    cashflow_profit_ratio = EXCLUDED.cashflow_profit_ratio,
                    period_expense_rate = EXCLUDED.period_expense_rate,
                    is_deleted = FALSE,
                    deleted_at = NULL,
                    updated_at = NOW()
                """;

        MapSqlParameterSource[] batch = metrics.stream()
                .map(this::toParams)
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
        log.info("批量 upsert 衍生指标完成, stockCode={}, 条数={}",
                metrics.get(0).getStockCode(), metrics.size());
    }

    @Override
    public void deleteByStockCodeAndYear(String stockCode, int reportYear) {
        String sql = """
                UPDATE stock_fundamental_metrics
                SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW()
                WHERE stock_code = :stockCode AND report_year = :reportYear
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("reportYear", reportYear);
        jdbcTemplate.update(sql, params);
    }

    private MapSqlParameterSource toParams(StockFundamentalMetrics m) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("stockCode", m.getStockCode());
        p.addValue("reportYear", m.getReportYear());
        p.addValue("revenueYoy", m.getRevenueYoy());
        p.addValue("profitYoy", m.getProfitYoy());
        p.addValue("assetGrowthRate", m.getAssetGrowthRate());
        p.addValue("roe", m.getRoe());
        p.addValue("roa", m.getRoa());
        p.addValue("assetTurnover", m.getAssetTurnover());
        p.addValue("equityMultiplier", m.getEquityMultiplier());
        p.addValue("currentRatio", m.getCurrentRatio());
        p.addValue("quickRatio", m.getQuickRatio());
        p.addValue("cashflowProfitRatio", m.getCashflowProfitRatio());
        p.addValue("periodExpenseRate", m.getPeriodExpenseRate());
        return p;
    }
}
