package com.example.securityanalyze.research.infrastructure;

import com.example.securityanalyze.research.domain.AnnualMetric;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.IndustryRankItem;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
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
public class FundamentalMetricsRepositoryImpl implements FundamentalMetricsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String OVERVIEW_SQL = """
            SELECT
                fr.report_date,
                fr.report_year,
                fr.total_revenue,
                fr.operate_income,
                fr.operate_cost,
                fr.parent_net_profit,
                fr.total_assets,
                fr.total_liabilities,
                fr.total_equity,
                fr.total_current_assets,
                fr.total_noncurrent_assets,
                fr.total_current_liabilities,
                fr.total_noncurrent_liabilities,
                fr.operating_cash_flow,
                fr.investing_cash_flow,
                fr.financing_cash_flow,
                fr.end_cce,
                fr.sale_expense,
                fr.manage_expense,
                fr.research_expense,
                fr.finance_expense,
                cs.stock_name,
                c.industry,
                cs.market
            FROM financial_report fr
            JOIN company_security cs ON fr.stock_code = cs.stock_code
            JOIN company c ON cs.company_id = c.id
            WHERE fr.stock_code = :stockCode
              AND fr.report_type = '年报'
              AND fr.report_date >= :startDate
              AND fr.is_deleted = FALSE
              AND cs.is_deleted = FALSE
              AND c.is_deleted = FALSE
            ORDER BY fr.report_date ASC
            """;

    private static final RowMapper<AnnualMetric> ANNUAL_METRIC_ROW_MAPPER = new RowMapper<>() {
        @Override
        public AnnualMetric mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnnualMetric metric = new AnnualMetric();
            java.sql.Date reportDate = rs.getDate("report_date");
            if (reportDate != null) {
                metric.setReportDate(reportDate.toLocalDate());
            }
            metric.setReportYear(rs.getInt("report_year"));
            metric.setTotalRevenue(rs.getBigDecimal("total_revenue"));
            metric.setOperateIncome(rs.getBigDecimal("operate_income"));
            metric.setOperateCost(rs.getBigDecimal("operate_cost"));
            metric.setParentNetProfit(rs.getBigDecimal("parent_net_profit"));
            metric.setTotalAssets(rs.getBigDecimal("total_assets"));
            metric.setTotalLiabilities(rs.getBigDecimal("total_liabilities"));
            metric.setTotalEquity(rs.getBigDecimal("total_equity"));
            metric.setTotalCurrentAssets(rs.getBigDecimal("total_current_assets"));
            metric.setTotalNoncurrentAssets(rs.getBigDecimal("total_noncurrent_assets"));
            metric.setTotalCurrentLiabilities(rs.getBigDecimal("total_current_liabilities"));
            metric.setTotalNoncurrentLiabilities(rs.getBigDecimal("total_noncurrent_liabilities"));
            metric.setOperatingCashFlow(rs.getBigDecimal("operating_cash_flow"));
            metric.setInvestingCashFlow(rs.getBigDecimal("investing_cash_flow"));
            metric.setFinancingCashFlow(rs.getBigDecimal("financing_cash_flow"));
            metric.setEndCce(rs.getBigDecimal("end_cce"));
            metric.setSaleExpense(rs.getBigDecimal("sale_expense"));
            metric.setManageExpense(rs.getBigDecimal("manage_expense"));
            metric.setResearchExpense(rs.getBigDecimal("research_expense"));
            metric.setFinanceExpense(rs.getBigDecimal("finance_expense"));
            return metric;
        }
    };

    @Override
    public Optional<FundamentalMetrics> findByStockCode(String stockCode, int years) {
        log.debug("查询基本面指标, stockCode={}, years={}", stockCode, years);
        LocalDate startDate = LocalDate.now().minusYears(years).withMonth(1).withDayOfMonth(1);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("startDate", startDate);

        List<AnnualMetric> metrics = jdbcTemplate.query(OVERVIEW_SQL, params, ANNUAL_METRIC_ROW_MAPPER);
        if (metrics.isEmpty()) {
            log.warn("未找到基本面数据, stockCode={}", stockCode);
            return Optional.empty();
        }

        // 提取公司基本信息（取第一条记录即可，同一公司相同）
        String stockName = jdbcTemplate.queryForObject(
                "SELECT stock_name FROM company_security WHERE stock_code = :stockCode AND is_deleted = FALSE",
                new MapSqlParameterSource("stockCode", stockCode),
                String.class
        );

        String industry = jdbcTemplate.queryForObject(
                """
                SELECT c.industry FROM company c
                JOIN company_security cs ON c.id = cs.company_id
                WHERE cs.stock_code = :stockCode AND cs.is_deleted = FALSE AND c.is_deleted = FALSE
                """,
                new MapSqlParameterSource("stockCode", stockCode),
                String.class
        );

        String market = jdbcTemplate.queryForObject(
                "SELECT market FROM company_security WHERE stock_code = :stockCode AND is_deleted = FALSE",
                new MapSqlParameterSource("stockCode", stockCode),
                String.class
        );

        FundamentalMetrics result = new FundamentalMetrics();
        result.setStockCode(stockCode);
        result.setStockName(stockName);
        result.setIndustry(industry);
        result.setMarket(market);
        result.setAnnualMetrics(metrics);

        log.info("查询基本面指标成功, stockCode={}, 返回{}条记录", stockCode, metrics.size());
        return Optional.of(result);
    }

    @Override
    public List<ScreenCompanyItem> screenCompanies(String keyword, String industry, String market, int offset, int limit) {
        log.debug("筛选公司, keyword={}, industry={}, market={}, offset={}, limit={}",
                keyword, industry, market, offset, limit);

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT ON (cs.stock_code)
                    cs.stock_code,
                    cs.stock_name,
                    c.industry,
                    cs.market,
                    fr.total_revenue,
                    fr.parent_net_profit
                FROM company_security cs
                JOIN company c ON cs.company_id = c.id
                LEFT JOIN LATERAL (
                    SELECT total_revenue, parent_net_profit
                    FROM financial_report
                    WHERE stock_code = cs.stock_code
                      AND report_type = '年报'
                      AND is_deleted = FALSE
                    ORDER BY report_date DESC
                    LIMIT 1
                ) fr ON TRUE
                WHERE cs.is_deleted = FALSE
                  AND c.is_deleted = FALSE
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (cs.stock_code = :keyword OR cs.stock_name ILIKE :prefix)");
            params.addValue("keyword", keyword.trim());
            params.addValue("prefix", keyword.trim() + "%");
        }
        if (industry != null && !industry.isBlank()) {
            sql.append(" AND c.industry = :industry");
            params.addValue("industry", industry);
        }
        if (market != null && !market.isBlank()) {
            sql.append(" AND cs.market = :market");
            params.addValue("market", market);
        }

        sql.append(" ORDER BY cs.stock_code ASC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            ScreenCompanyItem item = new ScreenCompanyItem();
            item.setStockCode(rs.getString("stock_code"));
            item.setStockName(rs.getString("stock_name"));
            item.setIndustry(rs.getString("industry"));
            item.setMarket(rs.getString("market"));
            item.setLatestRevenue(rs.getBigDecimal("total_revenue"));
            item.setLatestProfit(rs.getBigDecimal("parent_net_profit"));
            return item;
        });
    }

    @Override
    public long countScreenCompanies(String keyword, String industry, String market) {
        log.debug("统计筛选公司数量, keyword={}, industry={}, market={}", keyword, industry, market);

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT cs.stock_code)
                FROM company_security cs
                JOIN company c ON cs.company_id = c.id
                WHERE cs.is_deleted = FALSE
                  AND c.is_deleted = FALSE
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (cs.stock_code = :keyword OR cs.stock_name ILIKE :prefix)");
            params.addValue("keyword", keyword.trim());
            params.addValue("prefix", keyword.trim() + "%");
        }
        if (industry != null && !industry.isBlank()) {
            sql.append(" AND c.industry = :industry");
            params.addValue("industry", industry);
        }
        if (market != null && !market.isBlank()) {
            sql.append(" AND cs.market = :market");
            params.addValue("market", market);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<PeerMetric> findIndustryPeers(String stockCode) {
        log.debug("查询同行业对比, stockCode={}", stockCode);

        String sql = """
                WITH target_company AS (
                    SELECT c.industry
                    FROM company_security cs
                    JOIN company c ON cs.company_id = c.id
                    WHERE cs.stock_code = :stockCode
                      AND cs.is_deleted = FALSE
                      AND c.is_deleted = FALSE
                )
                SELECT DISTINCT ON (cs.stock_code)
                    cs.stock_code,
                    cs.stock_name,
                    c.industry,
                    fr.total_revenue,
                    fr.parent_net_profit,
                    CASE WHEN fr.total_equity IS NOT NULL AND fr.total_equity > 0
                         THEN fr.parent_net_profit / fr.total_equity * 100
                         ELSE NULL
                    END as roe,
                    CASE WHEN fr.total_assets IS NOT NULL AND fr.total_assets > 0
                         THEN fr.total_liabilities / fr.total_assets * 100
                         ELSE NULL
                    END as debt_ratio
                FROM company_security cs
                JOIN company c ON cs.company_id = c.id
                CROSS JOIN target_company tc
                WHERE c.industry = tc.industry
                  AND cs.stock_code != :stockCode
                  AND cs.is_deleted = FALSE
                  AND c.is_deleted = FALSE
                ORDER BY cs.stock_code, fr.report_date DESC NULLS LAST
                """;

        // 注意：上述 ORDER BY 在 DISTINCT ON 中只保证按 stock_code 去重，
        // 但 fr 字段可能来自任意一条记录。改用 LATERAL 子查询取最新一条。

        String optimizedSql = """
                WITH target_company AS (
                    SELECT c.industry
                    FROM company_security cs
                    JOIN company c ON cs.company_id = c.id
                    WHERE cs.stock_code = :stockCode
                      AND cs.is_deleted = FALSE
                      AND c.is_deleted = FALSE
                )
                SELECT
                    cs.stock_code,
                    cs.stock_name,
                    c.industry,
                    fr.total_revenue,
                    fr.parent_net_profit,
                    CASE WHEN fr.total_equity IS NOT NULL AND fr.total_equity > 0
                         THEN fr.parent_net_profit / fr.total_equity * 100
                         ELSE NULL
                    END as roe,
                    CASE WHEN fr.total_assets IS NOT NULL AND fr.total_assets > 0
                         THEN fr.total_liabilities / fr.total_assets * 100
                         ELSE NULL
                    END as debt_ratio
                FROM company_security cs
                JOIN company c ON cs.company_id = c.id
                CROSS JOIN target_company tc
                LEFT JOIN LATERAL (
                    SELECT total_revenue, parent_net_profit, total_equity, total_liabilities, total_assets
                    FROM financial_report
                    WHERE stock_code = cs.stock_code
                      AND report_type = '年报'
                      AND is_deleted = FALSE
                    ORDER BY report_date DESC
                    LIMIT 1
                ) fr ON TRUE
                WHERE c.industry = tc.industry
                  AND cs.stock_code != :stockCode
                  AND cs.is_deleted = FALSE
                  AND c.is_deleted = FALSE
                ORDER BY cs.stock_code ASC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);

        return jdbcTemplate.query(optimizedSql, params, (rs, rowNum) -> {
            PeerMetric peer = new PeerMetric();
            peer.setStockCode(rs.getString("stock_code"));
            peer.setStockName(rs.getString("stock_name"));
            peer.setIndustry(rs.getString("industry"));
            peer.setTotalRevenue(rs.getBigDecimal("total_revenue"));
            peer.setParentNetProfit(rs.getBigDecimal("parent_net_profit"));
            peer.setRoe(rs.getBigDecimal("roe"));
            peer.setDebtRatio(rs.getBigDecimal("debt_ratio"));
            return peer;
        });
    }

    @Override
    public String findIndustryByStockCode(String stockCode) {
        String sql = """
                SELECT c.industry FROM company c
                JOIN company_security cs ON c.id = cs.company_id
                WHERE cs.stock_code = :stockCode AND cs.is_deleted = FALSE AND c.is_deleted = FALSE
                """;
        try {
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("stockCode", stockCode), String.class);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<IndustryRankItem> findIndustryRankItems(String industry) {
        String sql = """
                SELECT
                    cs.stock_code,
                    cs.stock_name,
                    c.industry,
                    fr.total_revenue,
                    fr.parent_net_profit,
                    CASE WHEN fr.operate_income IS NOT NULL AND fr.operate_income > 0
                         THEN (fr.operate_income - fr.operate_cost) / fr.operate_income * 100
                         ELSE NULL
                    END as gross_margin,
                    sfm.roe,
                    CASE WHEN fr.total_assets IS NOT NULL AND fr.total_assets > 0
                         THEN fr.total_liabilities / fr.total_assets * 100
                         ELSE NULL
                    END as debt_ratio
                FROM company_security cs
                JOIN company c ON cs.company_id = c.id
                LEFT JOIN LATERAL (
                    SELECT total_revenue, parent_net_profit, operate_income, operate_cost, total_assets, total_liabilities
                    FROM financial_report
                    WHERE stock_code = cs.stock_code
                      AND report_type = '年报'
                      AND is_deleted = FALSE
                    ORDER BY report_date DESC
                    LIMIT 1
                ) fr ON TRUE
                LEFT JOIN LATERAL (
                    SELECT roe
                    FROM stock_fundamental_metrics
                    WHERE stock_code = cs.stock_code
                      AND is_deleted = FALSE
                    ORDER BY report_year DESC
                    LIMIT 1
                ) sfm ON TRUE
                WHERE c.industry = :industry
                  AND cs.is_deleted = FALSE
                  AND c.is_deleted = FALSE
                ORDER BY cs.stock_code ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("industry", industry);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            IndustryRankItem item = new IndustryRankItem();
            item.setStockCode(rs.getString("stock_code"));
            item.setStockName(rs.getString("stock_name"));
            item.setIndustry(rs.getString("industry"));
            item.setTotalRevenue(rs.getBigDecimal("total_revenue"));
            item.setParentNetProfit(rs.getBigDecimal("parent_net_profit"));
            item.setGrossMargin(rs.getBigDecimal("gross_margin"));
            item.setRoe(rs.getBigDecimal("roe"));
            item.setDebtRatio(rs.getBigDecimal("debt_ratio"));
            return item;
        });
    }
}
