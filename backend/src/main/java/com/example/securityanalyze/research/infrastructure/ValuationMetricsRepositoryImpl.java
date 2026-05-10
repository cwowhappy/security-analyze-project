package com.example.securityanalyze.research.infrastructure;

import com.example.securityanalyze.research.domain.CompanyBasicInfo;
import com.example.securityanalyze.research.domain.MetricStats;
import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.ValuationMetrics;
import com.example.securityanalyze.research.domain.ValuationMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ValuationMetricsRepositoryImpl implements ValuationMetricsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String LATEST_SQL = """
            SELECT
                vm.stock_code,
                vm.trade_date,
                vm.close_price,
                vm.pe_ttm,
                vm.pe_lyr,
                vm.pb,
                vm.ps_ttm,
                vm.pe_ttm_percentile,
                vm.pb_percentile,
                vm.ps_ttm_percentile,
                vm.dcf_fair_price
            FROM stock_valuation_metrics vm
            WHERE vm.stock_code = :stockCode
            ORDER BY vm.trade_date DESC
            LIMIT 1
            """;

    private static final String HISTORY_SQL = """
            SELECT
                vm.stock_code,
                vm.trade_date,
                vm.close_price,
                vm.pe_ttm,
                vm.pe_lyr,
                vm.pb,
                vm.ps_ttm,
                vm.pe_ttm_percentile,
                vm.pb_percentile,
                vm.ps_ttm_percentile,
                vm.dcf_fair_price
            FROM stock_valuation_metrics vm
            WHERE vm.stock_code = :stockCode
              AND vm.trade_date >= :startDate
              AND vm.trade_date <= :endDate
            ORDER BY vm.trade_date ASC
            """;

    private static final String METRIC_STATS_SQL = """
            SELECT
                PERCENTILE_CONT(0.0) WITHIN GROUP (ORDER BY %s) AS min_val,
                PERCENTILE_CONT(0.3) WITHIN GROUP (ORDER BY %s) AS p30,
                PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY %s) AS median,
                PERCENTILE_CONT(0.7) WITHIN GROUP (ORDER BY %s) AS p70,
                PERCENTILE_CONT(1.0) WITHIN GROUP (ORDER BY %s) AS max_val
            FROM stock_valuation_metrics
            WHERE stock_code = :stockCode
              AND trade_date >= :startDate
              AND %s IS NOT NULL
            """;

    private static final String COMPANY_INFO_SQL = """
            SELECT
                cs.stock_code,
                cs.stock_name,
                c.industry,
                cs.market,
                cs.total_shares
            FROM company_security cs
            JOIN company c ON cs.company_id = c.id
            WHERE cs.stock_code = :stockCode
              AND cs.is_deleted = FALSE
              AND c.is_deleted = FALSE
            LIMIT 1
            """;

    private static final String LATEST_OCF_SQL = """
            SELECT operating_cash_flow
            FROM financial_report
            WHERE stock_code = :stockCode
              AND report_type = '年报'
              AND is_deleted = FALSE
            ORDER BY report_date DESC
            LIMIT 1
            """;

    private static final String LATEST_FUNDAMENTAL_SQL = """
            SELECT
                stock_code, report_year,
                revenue_yoy, profit_yoy, asset_growth_rate,
                roe, roa, asset_turnover, equity_multiplier,
                current_ratio, quick_ratio,
                cashflow_profit_ratio, period_expense_rate
            FROM stock_fundamental_metrics
            WHERE stock_code = :stockCode
              AND is_deleted = FALSE
            ORDER BY report_year DESC
            LIMIT 1
            """;

    private static final RowMapper<ValuationMetrics> VALUATION_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ValuationMetrics mapRow(ResultSet rs, int rowNum) throws SQLException {
            ValuationMetrics m = new ValuationMetrics();
            m.setStockCode(rs.getString("stock_code"));
            java.sql.Date tradeDate = rs.getDate("trade_date");
            if (tradeDate != null) {
                m.setTradeDate(tradeDate.toLocalDate());
            }
            m.setClosePrice(rs.getBigDecimal("close_price"));
            m.setPeTtm(rs.getBigDecimal("pe_ttm"));
            m.setPeLyr(rs.getBigDecimal("pe_lyr"));
            m.setPb(rs.getBigDecimal("pb"));
            m.setPsTtm(rs.getBigDecimal("ps_ttm"));
            m.setPeTtmPercentile(rs.getBigDecimal("pe_ttm_percentile"));
            m.setPbPercentile(rs.getBigDecimal("pb_percentile"));
            m.setPsTtmPercentile(rs.getBigDecimal("ps_ttm_percentile"));
            m.setDcfFairPrice(rs.getBigDecimal("dcf_fair_price"));
            return m;
        }
    };

    private static final RowMapper<CompanyBasicInfo> COMPANY_INFO_ROW_MAPPER = (rs, rowNum) -> {
        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockCode(rs.getString("stock_code"));
        info.setStockName(rs.getString("stock_name"));
        info.setIndustry(rs.getString("industry"));
        info.setMarket(rs.getString("market"));
        info.setTotalShares(rs.getBigDecimal("total_shares"));
        return info;
    };

    private static final RowMapper<StockFundamentalMetrics> FUNDAMENTAL_ROW_MAPPER = (rs, rowNum) -> {
        StockFundamentalMetrics m = new StockFundamentalMetrics();
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
        return m;
    };

    @Override
    public Optional<ValuationMetrics> findLatestByStockCode(String stockCode) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        List<ValuationMetrics> list = jdbcTemplate.query(LATEST_SQL, params, VALUATION_ROW_MAPPER);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<ValuationMetrics> findHistoryByStockCode(String stockCode, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("startDate", startDate);
        params.addValue("endDate", endDate);
        return jdbcTemplate.query(HISTORY_SQL, params, VALUATION_ROW_MAPPER);
    }

    @Override
    public MetricStats findMetricStats(String stockCode, String metricName, int years) {
        LocalDate startDate = LocalDate.now().minusYears(years);
        // 简单校验 metricName，防止 SQL 注入
        String col = switch (metricName) {
            case "pe_ttm" -> "pe_ttm";
            case "pb" -> "pb";
            case "ps_ttm" -> "ps_ttm";
            default -> throw new IllegalArgumentException("Unknown metric: " + metricName);
        };

        String sql = String.format(METRIC_STATS_SQL, col, col, col, col, col, col);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("startDate", startDate);

        List<MetricStats> list = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            MetricStats stats = new MetricStats();
            stats.setMin(rs.getBigDecimal("min_val"));
            stats.setP30(rs.getBigDecimal("p30"));
            stats.setMedian(rs.getBigDecimal("median"));
            stats.setP70(rs.getBigDecimal("p70"));
            stats.setMax(rs.getBigDecimal("max_val"));
            return stats;
        });

        return list.isEmpty() ? new MetricStats() : list.get(0);
    }

    @Override
    public CompanyBasicInfo findCompanyBasicInfo(String stockCode) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        List<CompanyBasicInfo> list = jdbcTemplate.query(COMPANY_INFO_SQL, params, COMPANY_INFO_ROW_MAPPER);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public BigDecimal findLatestOperatingCashFlow(String stockCode) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        List<BigDecimal> list = jdbcTemplate.query(LATEST_OCF_SQL, params,
                (rs, rowNum) -> rs.getBigDecimal("operating_cash_flow"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Optional<StockFundamentalMetrics> findLatestFundamentalMetrics(String stockCode) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        List<StockFundamentalMetrics> list = jdbcTemplate.query(LATEST_FUNDAMENTAL_SQL, params, FUNDAMENTAL_ROW_MAPPER);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
