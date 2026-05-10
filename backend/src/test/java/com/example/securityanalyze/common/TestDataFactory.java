package com.example.securityanalyze.common;

import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.index.domain.EtfInfo;
import com.example.securityanalyze.index.domain.IndexEtfMapping;
import com.example.securityanalyze.index.domain.IndexHistory;
import com.example.securityanalyze.index.domain.IndexInfo;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
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
        return security(companyId, stockCode, stockName, new BigDecimal("1000000000"));
    }

    public static CompanySecurity security(Long companyId, String stockCode, String stockName, BigDecimal totalShares) {
        CompanySecurity s = new CompanySecurity();
        s.setCompanyId(companyId);
        s.setStockCode(stockCode);
        s.setStockName(stockName);
        s.setMarket("SH");
        s.setSecurityType("A股");
        s.setListingDate(LocalDate.of(2010, 6, 1));
        s.setListingStatus("上市");
        s.setTotalShares(totalShares);
        s.setCirculatingShares(totalShares);
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
        r.setTotalLiabilities(new BigDecimal("40000000"));
        r.setTotalEquity(new BigDecimal("60000000"));
        r.setTotalCurrentAssets(new BigDecimal("60000000"));
        r.setTotalNoncurrentAssets(new BigDecimal("40000000"));
        r.setTotalRevenue(new BigDecimal("50000000"));
        r.setOperateIncome(new BigDecimal("48000000"));
        r.setOperateCost(new BigDecimal("24000000"));
        r.setSaleExpense(new BigDecimal("3000000"));
        r.setManageExpense(new BigDecimal("4000000"));
        r.setResearchExpense(new BigDecimal("2000000"));
        r.setFinanceExpense(new BigDecimal("1000000"));
        r.setOperateProfit(new BigDecimal("8000000"));
        r.setTotalProfit(new BigDecimal("8500000"));
        r.setNetProfit(new BigDecimal("6000000"));
        r.setParentNetProfit(new BigDecimal("5000000"));
        r.setOperatingCashFlow(new BigDecimal("4500000"));
        r.setInvestingCashFlow(new BigDecimal("-1500000"));
        r.setFinancingCashFlow(new BigDecimal("-2000000"));
        r.setEndCce(new BigDecimal("12000000"));
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
                                              listing_date, listing_status, total_shares, circulating_shares,
                                              created_at, updated_at)
                VALUES (:companyId, :stockCode, :stockName, :market, :securityType,
                        :listingDate, :listingStatus, :totalShares, :circulatingShares,
                        :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", security.getCompanyId());
        params.addValue("stockCode", security.getStockCode());
        params.addValue("stockName", security.getStockName());
        params.addValue("market", security.getMarket());
        params.addValue("securityType", security.getSecurityType());
        params.addValue("listingDate", security.getListingDate());
        params.addValue("listingStatus", security.getListingStatus());
        params.addValue("totalShares", security.getTotalShares());
        params.addValue("circulatingShares", security.getCirculatingShares());
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

    // ------------------------------------------------------------------
    // 指数模块测试数据辅助方法
    // ------------------------------------------------------------------

    public static IndexInfo indexInfo(String indexCode, String indexName, String indexType) {
        IndexInfo idx = new IndexInfo();
        idx.setIndexCode(indexCode);
        idx.setIndexName(indexName);
        idx.setIndexType(indexType);
        idx.setMarket(indexCode.startsWith("0") || indexCode.startsWith("9") ? "SH" : "SZ");
        idx.setBaseDate(LocalDate.of(2000, 1, 1));
        idx.setBasePoint(new BigDecimal("1000.00"));
        idx.setComponentCount(100);
        idx.setPublishDate(LocalDate.of(2010, 6, 1));
        idx.setIsCore(false);
        idx.setSource("test");
        return idx;
    }

    public static IndexHistory indexHistory(String indexCode, LocalDate tradeDate, String granularity) {
        IndexHistory h = new IndexHistory();
        h.setIndexCode(indexCode);
        h.setTradeDate(tradeDate);
        h.setGranularity(granularity);
        h.setOpenPrice(new BigDecimal("3000.00"));
        h.setHighPrice(new BigDecimal("3100.00"));
        h.setLowPrice(new BigDecimal("2900.00"));
        h.setClosePrice(new BigDecimal("3050.00"));
        h.setVolume(1000000L);
        h.setAmount(new BigDecimal("500000000"));
        h.setAmplitude(new BigDecimal("3.33"));
        h.setChangePct(new BigDecimal("1.67"));
        h.setChangeAmount(new BigDecimal("50.00"));
        h.setTurnoverRate(new BigDecimal("0.50"));
        return h;
    }

    public static EtfInfo etfInfo(String etfCode, String etfName, String trackingIndexCode) {
        EtfInfo etf = new EtfInfo();
        etf.setEtfCode(etfCode);
        etf.setEtfName(etfName);
        etf.setTrackingIndexCode(trackingIndexCode);
        etf.setManagementFee(new BigDecimal("0.50"));
        etf.setFundSize(new BigDecimal("1000000000"));
        etf.setEstablishDate(LocalDate.of(2015, 1, 1));
        etf.setMarket(etfCode.startsWith("5") ? "SH" : "SZ");
        etf.setSource("test");
        return etf;
    }

    public static IndexEtfMapping indexEtfMapping(String indexCode, String etfCode) {
        IndexEtfMapping m = new IndexEtfMapping();
        m.setIndexCode(indexCode);
        m.setEtfCode(etfCode);
        m.setRelationType("track");
        return m;
    }

    public static Long insertIndexInfo(NamedParameterJdbcTemplate jdbc, IndexInfo index) {
        String sql = """
                INSERT INTO index_info (index_code, index_name, index_type, market, base_date, base_point,
                                        component_count, publish_date, is_core, source, created_at, updated_at)
                VALUES (:indexCode, :indexName, :indexType, :market, :baseDate, :basePoint,
                        :componentCount, :publishDate, :isCore, :source, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", index.getIndexCode());
        params.addValue("indexName", index.getIndexName());
        params.addValue("indexType", index.getIndexType());
        params.addValue("market", index.getMarket());
        params.addValue("baseDate", index.getBaseDate());
        params.addValue("basePoint", index.getBasePoint());
        params.addValue("componentCount", index.getComponentCount());
        params.addValue("publishDate", index.getPublishDate());
        params.addValue("isCore", index.getIsCore());
        params.addValue("source", index.getSource());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", index.getCreatedAt() != null ? index.getCreatedAt() : now);
        params.addValue("updatedAt", index.getUpdatedAt() != null ? index.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertIndexHistory(NamedParameterJdbcTemplate jdbc, IndexHistory history) {
        String sql = """
                INSERT INTO index_history (index_code, trade_date, granularity, open_price, high_price, low_price,
                                           close_price, volume, amount, amplitude, change_pct, change_amount,
                                           turnover_rate, created_at, updated_at)
                VALUES (:indexCode, :tradeDate, :granularity, :openPrice, :highPrice, :lowPrice,
                        :closePrice, :volume, :amount, :amplitude, :changePct, :changeAmount,
                        :turnoverRate, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", history.getIndexCode());
        params.addValue("tradeDate", history.getTradeDate());
        params.addValue("granularity", history.getGranularity());
        params.addValue("openPrice", history.getOpenPrice());
        params.addValue("highPrice", history.getHighPrice());
        params.addValue("lowPrice", history.getLowPrice());
        params.addValue("closePrice", history.getClosePrice());
        params.addValue("volume", history.getVolume());
        params.addValue("amount", history.getAmount());
        params.addValue("amplitude", history.getAmplitude());
        params.addValue("changePct", history.getChangePct());
        params.addValue("changeAmount", history.getChangeAmount());
        params.addValue("turnoverRate", history.getTurnoverRate());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", history.getCreatedAt() != null ? history.getCreatedAt() : now);
        params.addValue("updatedAt", history.getUpdatedAt() != null ? history.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertEtfInfo(NamedParameterJdbcTemplate jdbc, EtfInfo etf) {
        String sql = """
                INSERT INTO etf_info (etf_code, etf_name, tracking_index_code, management_fee, fund_size,
                                      establish_date, market, source, created_at, updated_at)
                VALUES (:etfCode, :etfName, :trackingIndexCode, :managementFee, :fundSize,
                        :establishDate, :market, :source, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("etfCode", etf.getEtfCode());
        params.addValue("etfName", etf.getEtfName());
        params.addValue("trackingIndexCode", etf.getTrackingIndexCode());
        params.addValue("managementFee", etf.getManagementFee());
        params.addValue("fundSize", etf.getFundSize());
        params.addValue("establishDate", etf.getEstablishDate());
        params.addValue("market", etf.getMarket());
        params.addValue("source", etf.getSource());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", etf.getCreatedAt() != null ? etf.getCreatedAt() : now);
        params.addValue("updatedAt", etf.getUpdatedAt() != null ? etf.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertIndexEtfMapping(NamedParameterJdbcTemplate jdbc, IndexEtfMapping mapping) {
        String sql = """
                INSERT INTO index_etf_mapping (index_code, etf_code, relation_type, created_at)
                VALUES (:indexCode, :etfCode, :relationType, :createdAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", mapping.getIndexCode());
        params.addValue("etfCode", mapping.getEtfCode());
        params.addValue("relationType", mapping.getRelationType());
        params.addValue("createdAt", mapping.getCreatedAt() != null ? mapping.getCreatedAt() : LocalDateTime.now());

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


    // ------------------------------------------------------------------
    // 持仓管理模块测试数据辅助方法
    // ------------------------------------------------------------------

    public static Long insertUser(NamedParameterJdbcTemplate jdbc, User user) {
        String sql = """
                INSERT INTO sys_user (username, password_hash, real_name, status, role, created_at, updated_at)
                VALUES (:username, :passwordHash, :realName, :status::user_status, :role::user_role, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", user.getUsername());
        params.addValue("passwordHash", user.getPasswordHash());
        params.addValue("realName", user.getRealName());
        params.addValue("status", user.getStatus().name());
        params.addValue("role", user.getRole().name());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", user.getCreatedAt() != null ? user.getCreatedAt() : now);
        params.addValue("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Portfolio portfolio(Long userId, String name, PortfolioType type) {
        Portfolio p = new Portfolio();
        p.setUserId(userId);
        p.setName(name);
        p.setType(type);
        p.setBroker("华泰证券");
        p.setDescription("测试组合");
        p.setIsDeleted(false);
        return p;
    }

    public static TransactionRecord transaction(Long portfolioId, String stockCode, TradeType tradeType,
                                                   java.math.BigDecimal price, java.math.BigDecimal quantity) {
        TransactionRecord t = new TransactionRecord();
        t.setPortfolioId(portfolioId);
        t.setStockCode(stockCode);
        t.setTradeDate(LocalDate.now());
        t.setTradeType(tradeType);
        t.setPrice(price);
        t.setQuantity(quantity);
        t.setFee(java.math.BigDecimal.ZERO);
        t.setTax(java.math.BigDecimal.ZERO);
        if (price != null && quantity != null) {
            t.setAmount(price.multiply(quantity));
        }
        t.setRealizedPnl(java.math.BigDecimal.ZERO);
        t.setIsDeleted(false);
        return t;
    }

    public static Position position(Long portfolioId, String stockCode, java.math.BigDecimal currentQuantity,
                                     java.math.BigDecimal totalCost, java.math.BigDecimal avgCost) {
        Position pos = new Position();
        pos.setPortfolioId(portfolioId);
        pos.setStockCode(stockCode);
        pos.setCurrentQuantity(currentQuantity);
        pos.setTotalCost(totalCost);
        pos.setAvgCost(avgCost);
        pos.setRealizedPnl(java.math.BigDecimal.ZERO);
        pos.setIsDeleted(false);
        return pos;
    }

    public static Long insertPortfolio(NamedParameterJdbcTemplate jdbc, Portfolio portfolio) {
        String sql = """
                INSERT INTO portfolio (user_id, name, type, broker, description, is_deleted, deleted_at, created_at, updated_at)
                VALUES (:userId, :name, :type::portfolio_type, :broker, :description, :isDeleted, :deletedAt, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", portfolio.getUserId());
        params.addValue("name", portfolio.getName());
        params.addValue("type", portfolio.getType().name());
        params.addValue("broker", portfolio.getBroker());
        params.addValue("description", portfolio.getDescription());
        params.addValue("isDeleted", portfolio.getIsDeleted() != null ? portfolio.getIsDeleted() : false);
        params.addValue("deletedAt", portfolio.getDeletedAt());
        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", portfolio.getCreatedAt() != null ? portfolio.getCreatedAt() : now);
        params.addValue("updatedAt", portfolio.getUpdatedAt() != null ? portfolio.getUpdatedAt() : now);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertTransaction(NamedParameterJdbcTemplate jdbc, TransactionRecord tx) {
        String sql = """
                INSERT INTO transaction_record (portfolio_id, stock_code, trade_date, trade_type, price, quantity,
                                                fee, tax, amount, realized_pnl, remark, is_deleted, deleted_at, created_at)
                VALUES (:portfolioId, :stockCode, :tradeDate, :tradeType::trade_type, :price, :quantity,
                        :fee, :tax, :amount, :realizedPnl, :remark, :isDeleted, :deletedAt, :createdAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", tx.getPortfolioId());
        params.addValue("stockCode", tx.getStockCode());
        params.addValue("tradeDate", tx.getTradeDate());
        params.addValue("tradeType", tx.getTradeType().name());
        params.addValue("price", tx.getPrice());
        params.addValue("quantity", tx.getQuantity());
        params.addValue("fee", tx.getFee());
        params.addValue("tax", tx.getTax());
        params.addValue("amount", tx.getAmount());
        params.addValue("realizedPnl", tx.getRealizedPnl());
        params.addValue("remark", tx.getRemark());
        params.addValue("isDeleted", tx.getIsDeleted() != null ? tx.getIsDeleted() : false);
        params.addValue("deletedAt", tx.getDeletedAt());
        params.addValue("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertPosition(NamedParameterJdbcTemplate jdbc, Position pos) {
        String sql = """
                INSERT INTO position (portfolio_id, stock_code, current_quantity, total_cost, avg_cost,
                                      realized_pnl, first_buy_date, last_trade_date, is_deleted, deleted_at, updated_at)
                VALUES (:portfolioId, :stockCode, :currentQuantity, :totalCost, :avgCost,
                        :realizedPnl, :firstBuyDate, :lastTradeDate, :isDeleted, :deletedAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", pos.getPortfolioId());
        params.addValue("stockCode", pos.getStockCode());
        params.addValue("currentQuantity", pos.getCurrentQuantity());
        params.addValue("totalCost", pos.getTotalCost());
        params.addValue("avgCost", pos.getAvgCost());
        params.addValue("realizedPnl", pos.getRealizedPnl());
        params.addValue("firstBuyDate", pos.getFirstBuyDate());
        params.addValue("lastTradeDate", pos.getLastTradeDate());
        params.addValue("isDeleted", pos.getIsDeleted() != null ? pos.getIsDeleted() : false);
        params.addValue("deletedAt", pos.getDeletedAt());
        params.addValue("updatedAt", pos.getUpdatedAt() != null ? pos.getUpdatedAt() : LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public static Long insertDailyQuote(NamedParameterJdbcTemplate jdbc, String stockCode, LocalDate tradeDate,
                                         BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
                                         BigDecimal closePrice, Long volume, BigDecimal amount) {
        String sql = """
                INSERT INTO daily_quote (stock_code, trade_date, open_price, high_price, low_price,
                                         close_price, volume, amount, created_at)
                VALUES (:stockCode, :tradeDate, :openPrice, :highPrice, :lowPrice,
                        :closePrice, :volume, :amount, :createdAt)
                ON CONFLICT (stock_code, trade_date) DO UPDATE
                SET open_price = EXCLUDED.open_price, high_price = EXCLUDED.high_price,
                    low_price = EXCLUDED.low_price, close_price = EXCLUDED.close_price,
                    volume = EXCLUDED.volume, amount = EXCLUDED.amount
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("tradeDate", tradeDate);
        params.addValue("openPrice", openPrice);
        params.addValue("highPrice", highPrice);
        params.addValue("lowPrice", lowPrice);
        params.addValue("closePrice", closePrice);
        params.addValue("volume", volume);
        params.addValue("amount", amount);
        params.addValue("createdAt", LocalDateTime.now());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"stock_code"});
        return 1L;
    }

    public static Long insertStockValuationMetrics(NamedParameterJdbcTemplate jdbc, String stockCode,
                                                    java.time.LocalDate tradeDate, BigDecimal closePrice,
                                                    BigDecimal peTtm, BigDecimal peLyr, BigDecimal pb,
                                                    BigDecimal psTtm, BigDecimal pePercentile,
                                                    BigDecimal pbPercentile, BigDecimal psPercentile) {
        String sql = """
                INSERT INTO stock_valuation_metrics
                (stock_code, trade_date, close_price, pe_ttm, pe_lyr, pb, ps_ttm,
                 pe_ttm_percentile, pb_percentile, ps_ttm_percentile, created_at, updated_at)
                VALUES (:stockCode, :tradeDate, :closePrice, :peTtm, :peLyr, :pb, :psTtm,
                        :peTtmPercentile, :pbPercentile, :psTtmPercentile, :createdAt, :updatedAt)
                ON CONFLICT (stock_code, trade_date) DO UPDATE
                SET close_price = EXCLUDED.close_price, pe_ttm = EXCLUDED.pe_ttm,
                    pe_lyr = EXCLUDED.pe_lyr, pb = EXCLUDED.pb, ps_ttm = EXCLUDED.ps_ttm,
                    pe_ttm_percentile = EXCLUDED.pe_ttm_percentile,
                    pb_percentile = EXCLUDED.pb_percentile,
                    ps_ttm_percentile = EXCLUDED.ps_ttm_percentile
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("tradeDate", tradeDate);
        params.addValue("closePrice", closePrice);
        params.addValue("peTtm", peTtm);
        params.addValue("peLyr", peLyr);
        params.addValue("pb", pb);
        params.addValue("psTtm", psTtm);
        params.addValue("peTtmPercentile", pePercentile);
        params.addValue("pbPercentile", pbPercentile);
        params.addValue("psTtmPercentile", psPercentile);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        params.addValue("createdAt", now);
        params.addValue("updatedAt", now);

        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"stock_code"});
        return 1L;
    }

    public static Long insertStockFundamentalMetrics(NamedParameterJdbcTemplate jdbc, String stockCode,
                                                      int reportYear, BigDecimal roe, BigDecimal revenueYoy,
                                                      BigDecimal profitYoy, BigDecimal cashflowProfitRatio,
                                                      BigDecimal periodExpenseRate) {
        String sql = """
                INSERT INTO stock_fundamental_metrics
                (stock_code, report_year, revenue_yoy, profit_yoy, asset_growth_rate,
                 roe, roa, asset_turnover, equity_multiplier, current_ratio, quick_ratio,
                 cashflow_profit_ratio, period_expense_rate, is_deleted, created_at, updated_at)
                VALUES (:stockCode, :reportYear, :revenueYoy, :profitYoy, :assetGrowthRate,
                        :roe, :roa, :assetTurnover, :equityMultiplier, :currentRatio, :quickRatio,
                        :cashflowProfitRatio, :periodExpenseRate, FALSE, :createdAt, :updatedAt)
                ON CONFLICT (stock_code, report_year) DO UPDATE
                SET revenue_yoy = EXCLUDED.revenue_yoy, profit_yoy = EXCLUDED.profit_yoy,
                    roe = EXCLUDED.roe, cashflow_profit_ratio = EXCLUDED.cashflow_profit_ratio,
                    period_expense_rate = EXCLUDED.period_expense_rate
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("reportYear", reportYear);
        params.addValue("revenueYoy", revenueYoy);
        params.addValue("profitYoy", profitYoy);
        params.addValue("assetGrowthRate", BigDecimal.ZERO);
        params.addValue("roe", roe);
        params.addValue("roa", BigDecimal.ZERO);
        params.addValue("assetTurnover", BigDecimal.ZERO);
        params.addValue("equityMultiplier", BigDecimal.ZERO);
        params.addValue("currentRatio", BigDecimal.ZERO);
        params.addValue("quickRatio", BigDecimal.ZERO);
        params.addValue("cashflowProfitRatio", cashflowProfitRatio);
        params.addValue("periodExpenseRate", periodExpenseRate);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        params.addValue("createdAt", now);
        params.addValue("updatedAt", now);

        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"stock_code"});
        return 1L;
    }
}
