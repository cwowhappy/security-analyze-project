package com.example.securityanalyze.finance.infrastructure;

import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.finance.domain.FinancialReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FinancialReportRepositoryImpl implements FinancialReportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String SELECT_SQL = """
            SELECT id, stock_code, report_date, report_type, report_year, notice_date, currency,
                   total_assets, total_liabilities, total_equity, monetary_funds, accounts_receivable,
                   inventory, total_current_assets, total_noncurrent_assets, total_current_liabilities,
                   total_noncurrent_liabilities, total_revenue, operate_income, operate_cost,
                   sale_expense, manage_expense, research_expense, finance_expense, operate_profit,
                   total_profit, net_profit, parent_net_profit, operating_cash_flow, investing_cash_flow,
                   financing_cash_flow, cce_add, end_cce, balance_sheet, profit_sheet, cash_flow_sheet,
                   created_at, updated_at
            FROM financial_report
            """;

    private final RowMapper<FinancialReport> rowMapper = (rs, rowNum) -> {
        FinancialReport report = new FinancialReport();
        report.setId(rs.getLong("id"));
        report.setStockCode(rs.getString("stock_code"));

        java.sql.Date reportDate = rs.getDate("report_date");
        if (reportDate != null) {
            report.setReportDate(reportDate.toLocalDate());
        }

        report.setReportType(rs.getString("report_type"));
        report.setReportYear(rs.getInt("report_year"));

        java.sql.Date noticeDate = rs.getDate("notice_date");
        if (noticeDate != null) {
            report.setNoticeDate(noticeDate.toLocalDate());
        }

        report.setCurrency(rs.getString("currency"));

        // 资产负债表
        report.setTotalAssets(getBigDecimal(rs, "total_assets"));
        report.setTotalLiabilities(getBigDecimal(rs, "total_liabilities"));
        report.setTotalEquity(getBigDecimal(rs, "total_equity"));
        report.setMonetaryFunds(getBigDecimal(rs, "monetary_funds"));
        report.setAccountsReceivable(getBigDecimal(rs, "accounts_receivable"));
        report.setInventory(getBigDecimal(rs, "inventory"));
        report.setTotalCurrentAssets(getBigDecimal(rs, "total_current_assets"));
        report.setTotalNoncurrentAssets(getBigDecimal(rs, "total_noncurrent_assets"));
        report.setTotalCurrentLiabilities(getBigDecimal(rs, "total_current_liabilities"));
        report.setTotalNoncurrentLiabilities(getBigDecimal(rs, "total_noncurrent_liabilities"));

        // 利润表
        report.setTotalRevenue(getBigDecimal(rs, "total_revenue"));
        report.setOperateIncome(getBigDecimal(rs, "operate_income"));
        report.setOperateCost(getBigDecimal(rs, "operate_cost"));
        report.setSaleExpense(getBigDecimal(rs, "sale_expense"));
        report.setManageExpense(getBigDecimal(rs, "manage_expense"));
        report.setResearchExpense(getBigDecimal(rs, "research_expense"));
        report.setFinanceExpense(getBigDecimal(rs, "finance_expense"));
        report.setOperateProfit(getBigDecimal(rs, "operate_profit"));
        report.setTotalProfit(getBigDecimal(rs, "total_profit"));
        report.setNetProfit(getBigDecimal(rs, "net_profit"));
        report.setParentNetProfit(getBigDecimal(rs, "parent_net_profit"));

        // 现金流量表
        report.setOperatingCashFlow(getBigDecimal(rs, "operating_cash_flow"));
        report.setInvestingCashFlow(getBigDecimal(rs, "investing_cash_flow"));
        report.setFinancingCashFlow(getBigDecimal(rs, "financing_cash_flow"));
        report.setCceAdd(getBigDecimal(rs, "cce_add"));
        report.setEndCce(getBigDecimal(rs, "end_cce"));

        // JSONB
        report.setBalanceSheet(readJsonb(rs, "balance_sheet"));
        report.setProfitSheet(readJsonb(rs, "profit_sheet"));
        report.setCashFlowSheet(readJsonb(rs, "cash_flow_sheet"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            report.setCreatedAt(createdAt.toLocalDateTime());
        }

        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            report.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return report;
    };

    private static BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? null : value;
    }

    private Map<String, Object> readJsonb(ResultSet rs, String column) throws SQLException {
        String json = rs.getString(column);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public List<FinancialReport> findByStockCode(String stockCode) {
        String sql = SELECT_SQL + " WHERE stock_code = :stockCode ORDER BY report_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    public List<FinancialReport> findByStockCodeAndYear(String stockCode, int year) {
        String sql = SELECT_SQL + " WHERE stock_code = :stockCode AND report_year = :year ORDER BY report_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("year", year);
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    public List<FinancialReport> findByStockCodeAndDateRange(String stockCode, LocalDate startDate, LocalDate endDate) {
        String sql = SELECT_SQL + " WHERE stock_code = :stockCode AND report_date BETWEEN :startDate AND :endDate ORDER BY report_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("startDate", startDate);
        params.addValue("endDate", endDate);
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    @Override
    public Optional<FinancialReport> findById(Long id) {
        String sql = SELECT_SQL + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        List<FinancialReport> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<FinancialReport> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate) {
        String sql = SELECT_SQL + " WHERE stock_code = :stockCode AND report_date = :reportDate";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("reportDate", reportDate);
        List<FinancialReport> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void save(FinancialReport report) {
        if (report.getId() != null && existsById(report.getId())) {
            update(report);
        } else {
            insert(report);
        }
    }

    @Override
    public void saveAll(List<FinancialReport> reports) {
        for (FinancialReport report : reports) {
            save(report);
        }
    }

    @Override
    public boolean existsByStockCodeAndReportDate(String stockCode, LocalDate reportDate) {
        String sql = "SELECT COUNT(*) FROM financial_report WHERE stock_code = :stockCode AND report_date = :reportDate";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("reportDate", reportDate);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    private boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM financial_report WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    private void insert(FinancialReport report) {
        String sql = """
                INSERT INTO financial_report (
                    stock_code, report_date, report_type, report_year, notice_date, currency,
                    total_assets, total_liabilities, total_equity, monetary_funds, accounts_receivable,
                    inventory, total_current_assets, total_noncurrent_assets, total_current_liabilities,
                    total_noncurrent_liabilities, total_revenue, operate_income, operate_cost,
                    sale_expense, manage_expense, research_expense, finance_expense, operate_profit,
                    total_profit, net_profit, parent_net_profit, operating_cash_flow, investing_cash_flow,
                    financing_cash_flow, cce_add, end_cce, balance_sheet, profit_sheet, cash_flow_sheet,
                    created_at, updated_at
                ) VALUES (
                    :stockCode, :reportDate, :reportType, :reportYear, :noticeDate, :currency,
                    :totalAssets, :totalLiabilities, :totalEquity, :monetaryFunds, :accountsReceivable,
                    :inventory, :totalCurrentAssets, :totalNoncurrentAssets, :totalCurrentLiabilities,
                    :totalNoncurrentLiabilities, :totalRevenue, :operateIncome, :operateCost,
                    :saleExpense, :manageExpense, :researchExpense, :financeExpense, :operateProfit,
                    :totalProfit, :netProfit, :parentNetProfit, :operatingCashFlow, :investingCashFlow,
                    :financingCashFlow, :cceAdd, :endCce, :balanceSheet::jsonb, :profitSheet::jsonb, :cashFlowSheet::jsonb,
                    NOW(), NOW()
                )
                """;
        jdbcTemplate.update(sql, toParams(report));
    }

    private void update(FinancialReport report) {
        String sql = """
                UPDATE financial_report SET
                    report_type = :reportType,
                    report_year = :reportYear,
                    notice_date = :noticeDate,
                    currency = :currency,
                    total_assets = :totalAssets,
                    total_liabilities = :totalLiabilities,
                    total_equity = :totalEquity,
                    monetary_funds = :monetaryFunds,
                    accounts_receivable = :accountsReceivable,
                    inventory = :inventory,
                    total_current_assets = :totalCurrentAssets,
                    total_noncurrent_assets = :totalNoncurrentAssets,
                    total_current_liabilities = :totalCurrentLiabilities,
                    total_noncurrent_liabilities = :totalNoncurrentLiabilities,
                    total_revenue = :totalRevenue,
                    operate_income = :operateIncome,
                    operate_cost = :operateCost,
                    sale_expense = :saleExpense,
                    manage_expense = :manageExpense,
                    research_expense = :researchExpense,
                    finance_expense = :financeExpense,
                    operate_profit = :operateProfit,
                    total_profit = :totalProfit,
                    net_profit = :netProfit,
                    parent_net_profit = :parentNetProfit,
                    operating_cash_flow = :operatingCashFlow,
                    investing_cash_flow = :investingCashFlow,
                    financing_cash_flow = :financingCashFlow,
                    cce_add = :cceAdd,
                    end_cce = :endCce,
                    balance_sheet = :balanceSheet::jsonb,
                    profit_sheet = :profitSheet::jsonb,
                    cash_flow_sheet = :cashFlowSheet::jsonb,
                    updated_at = NOW()
                WHERE id = :id
                """;
        MapSqlParameterSource params = toParams(report);
        params.addValue("id", report.getId());
        jdbcTemplate.update(sql, params);
    }

    private MapSqlParameterSource toParams(FinancialReport report) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", report.getStockCode());
        params.addValue("reportDate", report.getReportDate());
        params.addValue("reportType", report.getReportType());
        params.addValue("reportYear", report.getReportYear());
        params.addValue("noticeDate", report.getNoticeDate());
        params.addValue("currency", report.getCurrency());

        params.addValue("totalAssets", report.getTotalAssets());
        params.addValue("totalLiabilities", report.getTotalLiabilities());
        params.addValue("totalEquity", report.getTotalEquity());
        params.addValue("monetaryFunds", report.getMonetaryFunds());
        params.addValue("accountsReceivable", report.getAccountsReceivable());
        params.addValue("inventory", report.getInventory());
        params.addValue("totalCurrentAssets", report.getTotalCurrentAssets());
        params.addValue("totalNoncurrentAssets", report.getTotalNoncurrentAssets());
        params.addValue("totalCurrentLiabilities", report.getTotalCurrentLiabilities());
        params.addValue("totalNoncurrentLiabilities", report.getTotalNoncurrentLiabilities());

        params.addValue("totalRevenue", report.getTotalRevenue());
        params.addValue("operateIncome", report.getOperateIncome());
        params.addValue("operateCost", report.getOperateCost());
        params.addValue("saleExpense", report.getSaleExpense());
        params.addValue("manageExpense", report.getManageExpense());
        params.addValue("researchExpense", report.getResearchExpense());
        params.addValue("financeExpense", report.getFinanceExpense());
        params.addValue("operateProfit", report.getOperateProfit());
        params.addValue("totalProfit", report.getTotalProfit());
        params.addValue("netProfit", report.getNetProfit());
        params.addValue("parentNetProfit", report.getParentNetProfit());

        params.addValue("operatingCashFlow", report.getOperatingCashFlow());
        params.addValue("investingCashFlow", report.getInvestingCashFlow());
        params.addValue("financingCashFlow", report.getFinancingCashFlow());
        params.addValue("cceAdd", report.getCceAdd());
        params.addValue("endCce", report.getEndCce());

        params.addValue("balanceSheet", writeJsonb(report.getBalanceSheet()));
        params.addValue("profitSheet", writeJsonb(report.getProfitSheet()));
        params.addValue("cashFlowSheet", writeJsonb(report.getCashFlowSheet()));

        return params;
    }

    private String writeJsonb(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
