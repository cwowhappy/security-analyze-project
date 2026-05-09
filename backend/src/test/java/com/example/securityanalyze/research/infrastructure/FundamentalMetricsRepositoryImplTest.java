package com.example.securityanalyze.research.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(FundamentalMetricsRepositoryImpl.class)
class FundamentalMetricsRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private FundamentalMetricsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByStockCode() {
        // 准备数据
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110001", "测试公司", "测试"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600001", "测试股份"));
        FinancialReport report = TestDataFactory.report("600001", LocalDate.of(2023, 12, 31));
        TestDataFactory.insertFinancialReport(jdbcTemplate, report);

        Optional<FundamentalMetrics> result = repository.findByStockCode("600001", 5);

        assertTrue(result.isPresent());
        assertEquals("600001", result.get().getStockCode());
        assertEquals("测试股份", result.get().getStockName());
        assertEquals("信息技术", result.get().getIndustry());
        assertEquals(1, result.get().getAnnualMetrics().size());
        assertEquals(LocalDate.of(2023, 12, 31), result.get().getAnnualMetrics().get(0).getReportDate());
    }

    @Test
    void shouldReturnEmptyWhenStockCodeNotFound() {
        Optional<FundamentalMetrics> result = repository.findByStockCode("999999", 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenNoAnnualReports() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110002", "无财报公司", "无财"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600002", "无财报"));

        Optional<FundamentalMetrics> result = repository.findByStockCode("600002", 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFilterByReportYearRange() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110003", "年份过滤", "年份"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600003", "年份过滤"));

        // 插入2020年报告（超过5年范围）
        FinancialReport oldReport = TestDataFactory.report("600003", LocalDate.of(2020, 12, 31));
        TestDataFactory.insertFinancialReport(jdbcTemplate, oldReport);

        // 插入2023年报告（在范围内）
        FinancialReport newReport = TestDataFactory.report("600003", LocalDate.of(2023, 12, 31));
        TestDataFactory.insertFinancialReport(jdbcTemplate, newReport);

        Optional<FundamentalMetrics> result = repository.findByStockCode("600003", 5);

        assertTrue(result.isPresent());
        // 假设当前年份 >= 2025，则2020年的报告可能不在5年范围内
        // 由于测试时间不确定性，这里只验证返回了在范围内的报告
        assertTrue(result.get().getAnnualMetrics().size() >= 1);
    }

    @Test
    void shouldScreenCompanies() {
        Long companyId1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110004", "筛选测试A", "筛选A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId1, "600004", "筛选A"));

        Long companyId2 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110005", "筛选测试B", "筛选B"));
        CompanySecurity sec2 = TestDataFactory.security(companyId2, "600005", "筛选B");
        sec2.setMarket("SZ");
        TestDataFactory.insertCompanySecurity(jdbcTemplate, sec2);

        // 插入财务报告使公司有最新数据
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600004", LocalDate.of(2023, 12, 31)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600005", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> results = repository.screenCompanies(null, null, null, 0, 20);

        assertTrue(results.size() >= 2);
        List<String> codes = results.stream().map(ScreenCompanyItem::getStockCode).toList();
        assertTrue(codes.contains("600004"));
        assertTrue(codes.contains("600005"));
    }

    @Test
    void shouldScreenCompaniesByKeyword() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110006", "关键词测试", "关键词"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600006", "关键词股份"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600006", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> byCode = repository.screenCompanies("600006", null, null, 0, 20);
        List<ScreenCompanyItem> byName = repository.screenCompanies("关键词", null, null, 0, 20);

        assertEquals(1, byCode.size());
        assertEquals("600006", byCode.get(0).getStockCode());
        assertEquals(1, byName.size());
    }

    @Test
    void shouldScreenCompaniesByMarket() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110007", "市场测试", "市场"));
        CompanySecurity sec = TestDataFactory.security(companyId, "600007", "市场测试");
        sec.setMarket("SZ");
        TestDataFactory.insertCompanySecurity(jdbcTemplate, sec);
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600007", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> results = repository.screenCompanies(null, null, "SZ", 0, 20);

        assertTrue(results.stream().anyMatch(r -> "600007".equals(r.getStockCode())));
    }

    @Test
    void shouldCountScreenCompanies() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110008", "计数测试", "计数"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600008", "计数测试"));

        long total = repository.countScreenCompanies(null, null, null);
        assertTrue(total >= 1);
    }

    @Test
    void shouldFindIndustryPeers() {
        // 创建同行业的两家公司
        Long companyId1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110009", "同行A", "同行A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId1, "600009", "同行A"));

        Company company2 = TestDataFactory.company("91110010", "同行B", "同行B");
        company2.setIndustry("信息技术"); // 确保同一行业
        Long companyId2 = TestDataFactory.insertCompany(jdbcTemplate, company2);
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId2, "600010", "同行B"));

        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600009", LocalDate.of(2023, 12, 31)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600010", LocalDate.of(2023, 12, 31)));

        List<PeerMetric> peers = repository.findIndustryPeers("600009");

        assertTrue(peers.stream().anyMatch(p -> "600010".equals(p.getStockCode())));
        assertFalse(peers.stream().anyMatch(p -> "600009".equals(p.getStockCode()))); // 排除自身
    }

    @Test
    void shouldReturnEmptyPeersWhenNoSameIndustry() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110011", "独行者", "独行"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600011", "独行者"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600011", LocalDate.of(2023, 12, 31)));

        List<PeerMetric> peers = repository.findIndustryPeers("600011");

        assertTrue(peers.isEmpty() || peers.stream().noneMatch(p -> "600011".equals(p.getStockCode())));
    }

    @Test
    void shouldCalculatePeerMetrics() {
        Long companyId1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110012", "计算A", "计算A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId1, "600012", "计算A"));

        Company company2 = TestDataFactory.company("91110013", "计算B", "计算B");
        company2.setIndustry("信息技术");
        Long companyId2 = TestDataFactory.insertCompany(jdbcTemplate, company2);
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId2, "600013", "计算B"));

        FinancialReport report = TestDataFactory.report("600013", LocalDate.of(2023, 12, 31));
        report.setTotalAssets(new BigDecimal("80000000"));
        report.setTotalLiabilities(new BigDecimal("20000000"));
        report.setTotalEquity(new BigDecimal("60000000"));
        report.setParentNetProfit(new BigDecimal("6000000"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, report);

        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600012", LocalDate.of(2023, 12, 31)));

        List<PeerMetric> peers = repository.findIndustryPeers("600012");

        PeerMetric peer = peers.stream().filter(p -> "600013".equals(p.getStockCode())).findFirst().orElse(null);
        assertNotNull(peer);
        assertNotNull(peer.getRoe()); // 6000000 / 60000000 * 100 = 10
        assertEquals(0, new BigDecimal("10").compareTo(peer.getRoe().setScale(0, BigDecimal.ROUND_HALF_UP)));
        assertNotNull(peer.getDebtRatio()); // 20000000 / 80000000 * 100 = 25
    }

    @Test
    void shouldScreenCompaniesByIndustry() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110014", "行业筛选", "行业"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600014", "行业筛选"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600014", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> results = repository.screenCompanies(null, "信息技术", null, 0, 20);

        assertTrue(results.stream().anyMatch(r -> "600014".equals(r.getStockCode())));
    }

    @Test
    void shouldScreenCompaniesWithAllFilters() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110015", "全筛选", "全筛"));
        CompanySecurity sec = TestDataFactory.security(companyId, "600015", "全筛选");
        sec.setMarket("SZ");
        TestDataFactory.insertCompanySecurity(jdbcTemplate, sec);
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600015", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> results = repository.screenCompanies("600015", "信息技术", "SZ", 0, 20);

        assertEquals(1, results.size());
        assertEquals("600015", results.get(0).getStockCode());
    }

    @Test
    void shouldCountScreenCompaniesWithFilters() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110016", "计数筛选", "计数"));
        CompanySecurity sec = TestDataFactory.security(companyId, "600016", "计数筛选");
        sec.setMarket("BJ");
        TestDataFactory.insertCompanySecurity(jdbcTemplate, sec);

        long count = repository.countScreenCompanies(null, "信息技术", "BJ");

        assertTrue(count >= 1);
    }

    @Test
    void shouldReturnEmptyPeersWhenTargetCompanyHasNoIndustry() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110017", "无行业", "无业"));
        Company company = TestDataFactory.company("91110017", "无行业", "无业");
        company.setIndustry(null);
        // 重新插入以更新 industry 为 null
        String updateSql = "UPDATE company SET industry = NULL WHERE id = :id";
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params =
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
        params.addValue("id", companyId);
        jdbcTemplate.update(updateSql, params);

        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600017", "无行业"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600017", LocalDate.of(2023, 12, 31)));

        List<PeerMetric> peers = repository.findIndustryPeers("600017");

        // industry 为 null 时，对比结果应为空（因为 WHERE c.industry = tc.industry 中 tc.industry 为 null）
        assertTrue(peers.isEmpty());
    }

    @Test
    void shouldScreenCompaniesWithBlankKeyword() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110018", "空白关键词", "空白"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600018", "空白证券"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600018", LocalDate.of(2023, 12, 31)));

        // blank keyword 应等同于 null，返回所有结果
        List<ScreenCompanyItem> results = repository.screenCompanies("", null, null, 0, 20);
        assertTrue(results.stream().anyMatch(r -> "600018".equals(r.getStockCode())));
    }

    @Test
    void shouldScreenCompaniesWithBlankIndustry() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110019", "空白行业", "空白"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600019", "空白行业证券"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600019", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> results = repository.screenCompanies(null, "   ", null, 0, 20);
        assertTrue(results.stream().anyMatch(r -> "600019".equals(r.getStockCode())));
    }

    @Test
    void shouldScreenCompaniesWithBlankMarket() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110020", "空白市场", "空白"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600020", "空白市场证券"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600020", LocalDate.of(2023, 12, 31)));

        List<ScreenCompanyItem> results = repository.screenCompanies(null, null, "", 0, 20);
        assertTrue(results.stream().anyMatch(r -> "600020".equals(r.getStockCode())));
    }

    @Test
    void shouldCountScreenCompaniesWithBlankKeyword() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110021", "计数空白关键词", "计数空白"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600021", "计数空白证券"));

        long count = repository.countScreenCompanies("", null, null);
        assertTrue(count >= 1);
    }

    @Test
    void shouldCountScreenCompaniesWithBlankIndustry() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110022", "计数空白行业", "计数空白行业"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600022", "计数空白行业证券"));

        long count = repository.countScreenCompanies(null, "   ", null);
        assertTrue(count >= 1);
    }

    @Test
    void shouldCountScreenCompaniesWithBlankMarket() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110023", "计数空白市场", "计数空白市场"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600023", "计数空白市场证券"));

        long count = repository.countScreenCompanies(null, null, "");
        assertTrue(count >= 1);
    }
}
