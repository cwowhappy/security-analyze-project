package com.example.securityanalyze.common;

import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 测试数据构造工厂，提供各类领域对象的便捷创建方法，以及直接插入数据库的辅助方法。
 */
public final class TestDataFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TestDataFactory() {
    }

    public static Company company(String unifiedCode, String name, String shortName) {
        Company c = new Company();
        c.setUnifiedCode(unifiedCode);
        c.setCompanyName(name);
        c.setShortName(shortName);
        c.setIndustry("信息技术");
        c.setRegion("北京市");
        c.setEstablishDate(LocalDate.of(2000, 1, 1));
        c.setRegisteredCapital(new BigDecimal("10000"));
        return c;
    }

    public static CompanySecurity security(Long companyId, String stockCode, String stockName) {
        CompanySecurity s = new CompanySecurity();
        s.setCompanyId(companyId);
        s.setStockCode(stockCode);
        s.setStockName(stockName);
        s.setMarket("SH");
        s.setSecurityType("A股");
        s.setListingDate(LocalDate.of(2010, 6, 1));
        s.setListingStatus("上市");
        return s;
    }

    public static FinancialReport report(String stockCode, LocalDate reportDate) {
        FinancialReport r = new FinancialReport();
        r.setStockCode(stockCode);
        r.setReportDate(reportDate);
        r.setReportType("年报");
        r.setReportYear(reportDate.getYear());
        r.setNoticeDate(reportDate.plusMonths(1));
        r.setCurrency("CNY");
        r.setTotalAssets(new BigDecimal("100000000"));
        r.setTotalRevenue(new BigDecimal("50000000"));
        r.setNetProfit(new BigDecimal("5000000"));
        r.setBalanceSheet(Map.of("key", "value"));
        return r;
    }

    public static User user(String username, UserStatus status) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("hash");
        u.setRealName("测试用户");
        u.setStatus(status);
        u.setRole(Role.USER);
        return u;
    }

    // ------------------------------------------------------------------
    // 直接插入数据库的辅助方法（用于没有 save() 方法的 Repository 测试）
    // ------------------------------------------------------------------

    public static Long insertCompany(NamedParameterJdbcTemplate jdbc, Company company) {
        String sql = """
                INSERT INTO company (unified_code, company_name, short_name, industry, region,
                                     establish_date, registered_capital, created_at, updated_at)
                VALUES (:unifiedCode, :companyName, :shortName, :industry, :region,
                        :establishDate, :registeredCapital, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("unifiedCode", company.getUnifiedCode());
        params.addValue("companyName", company.getCompanyName());
        params.addValue("shortName", company.getShortName());
        params.addValue("industry", company.getIndustry());
        params.addValue("region", company.getRegion());
        params.addValue("establishDate", company.getEstablishDate());
        params.addValue("registeredCapital", company.getRegisteredCapital());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", company.getCreatedAt() != null ? company.getCreatedAt() : now);
        params.addValue("updatedAt", company.getUpdatedAt() != null ? company.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertCompanySecurity(NamedParameterJdbcTemplate jdbc, CompanySecurity security) {
        String sql = """
                INSERT INTO company_security (company_id, stock_code, stock_name, market, security_type,
                                              listing_date, listing_status, created_at, updated_at)
                VALUES (:companyId, :stockCode, :stockName, :market, :securityType,
                        :listingDate, :listingStatus, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", security.getCompanyId());
        params.addValue("stockCode", security.getStockCode());
        params.addValue("stockName", security.getStockName());
        params.addValue("market", security.getMarket());
        params.addValue("securityType", security.getSecurityType());
        params.addValue("listingDate", security.getListingDate());
        params.addValue("listingStatus", security.getListingStatus());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", security.getCreatedAt() != null ? security.getCreatedAt() : now);
        params.addValue("updatedAt", security.getUpdatedAt() != null ? security.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertFinancialReport(NamedParameterJdbcTemplate jdbc, FinancialReport report) {
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
                    :createdAt, :updatedAt
                )
                """;
        MapSqlParameterSource params = financialReportParams(report);
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", report.getCreatedAt() != null ? report.getCreatedAt() : now);
        params.addValue("updatedAt", report.getUpdatedAt() != null ? report.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertCollectorTaskLog(NamedParameterJdbcTemplate jdbc, String taskName, String taskType,
                                               LocalDateTime startedAt, LocalDateTime endedAt,
                                               String status, Integer rowsAffected) {
        String sql = """
                INSERT INTO collector_task_log (task_name, task_type, started_at, ended_at, status, rows_affected, created_at)
                VALUES (:taskName, :taskType, :startedAt, :endedAt, :status, :rowsAffected, :createdAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("taskName", taskName);
        params.addValue("taskType", taskType);
        params.addValue("startedAt", Timestamp.valueOf(startedAt));
        params.addValue("endedAt", endedAt != null ? Timestamp.valueOf(endedAt) : null);
        params.addValue("status", status);
        params.addValue("rowsAffected", rowsAffected);
        params.addValue("createdAt", Timestamp.valueOf(LocalDateTime.now()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    // ------------------------------------------------------------------
    // 内部辅助
    // ------------------------------------------------------------------

    static MapSqlParameterSource financialReportParams(FinancialReport report) {
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

    private static String writeJsonb(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSONB 序列化失败", e);
        }
    }
}
