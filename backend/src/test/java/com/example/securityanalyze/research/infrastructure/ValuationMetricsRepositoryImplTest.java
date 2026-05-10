package com.example.securityanalyze.research.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.research.domain.CompanyBasicInfo;
import com.example.securityanalyze.research.domain.MetricStats;
import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.ValuationMetrics;
import com.example.securityanalyze.research.domain.ValuationMetricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(ValuationMetricsRepositoryImpl.class)
class ValuationMetricsRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private ValuationMetricsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindLatestByStockCode() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate,
                TestDataFactory.company("91110010", "估值测试公司", "估值测试"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate,
                TestDataFactory.security(companyId, "600010", "估值测试"));

        TestDataFactory.insertStockValuationMetrics(jdbcTemplate, "600010",
                LocalDate.of(2024, 1, 2), new BigDecimal("100"),
                new BigDecimal("28.5"), new BigDecimal("30"), new BigDecimal("8.3"), new BigDecimal("12.1"),
                new BigDecimal("0.72"), new BigDecimal("0.65"), new BigDecimal("0.58"));
        TestDataFactory.insertStockValuationMetrics(jdbcTemplate, "600010",
                LocalDate.of(2024, 1, 3), new BigDecimal("101"),
                new BigDecimal("29"), new BigDecimal("31"), new BigDecimal("8.5"), new BigDecimal("12.5"),
                new BigDecimal("0.73"), new BigDecimal("0.66"), new BigDecimal("0.59"));

        Optional<ValuationMetrics> result = repository.findLatestByStockCode("600010");

        assertTrue(result.isPresent());
        assertEquals("600010", result.get().getStockCode());
        assertEquals(LocalDate.of(2024, 1, 3), result.get().getTradeDate());
        assertEquals(0, new BigDecimal("101").compareTo(result.get().getClosePrice()));
        assertEquals(0, new BigDecimal("29").compareTo(result.get().getPeTtm()));
    }

    @Test
    void shouldReturnEmptyWhenNoValuationData() {
        Optional<ValuationMetrics> result = repository.findLatestByStockCode("999999");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindHistoryByStockCode() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate,
                TestDataFactory.company("91110011", "历史测试公司", "历史测试"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate,
                TestDataFactory.security(companyId, "600011", "历史测试"));

        for (int i = 0; i < 5; i++) {
            TestDataFactory.insertStockValuationMetrics(jdbcTemplate, "600011",
                    LocalDate.of(2024, 1, i + 1), new BigDecimal("100" + i),
                    new BigDecimal("20"), new BigDecimal("25"), new BigDecimal("5"), new BigDecimal("10"),
                    new BigDecimal("0.5"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        }

        List<ValuationMetrics> result = repository.findHistoryByStockCode("600011",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5));

        assertEquals(5, result.size());
        assertEquals(LocalDate.of(2024, 1, 1), result.get(0).getTradeDate());
        assertEquals(LocalDate.of(2024, 1, 5), result.get(4).getTradeDate());
    }

    @Test
    void shouldFindMetricStats() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate,
                TestDataFactory.company("91110012", "统计测试公司", "统计测试"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate,
                TestDataFactory.security(companyId, "600012", "统计测试"));

        for (int i = 0; i < 10; i++) {
            TestDataFactory.insertStockValuationMetrics(jdbcTemplate, "600012",
                    LocalDate.now().minusDays(i), new BigDecimal("100"),
                    new BigDecimal(i + 10), null, null, null,
                    null, null, null);
        }

        MetricStats stats = repository.findMetricStats("600012", "pe_ttm", 5);

        assertNotNull(stats);
        assertNotNull(stats.getMin());
        assertNotNull(stats.getMax());
        assertNotNull(stats.getMedian());
        assertNotNull(stats.getP30());
        assertNotNull(stats.getP70());
    }

    @Test
    void shouldThrowExceptionForUnknownMetric() {
        assertThrows(IllegalArgumentException.class, () ->
                repository.findMetricStats("600012", "unknown_metric", 5));
    }

    @Test
    void shouldFindCompanyBasicInfo() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate,
                TestDataFactory.company("91110013", "基本信息公司", "基本信息"));
        CompanySecurity sec = TestDataFactory.security(companyId, "600013", "基本信息", new BigDecimal("500000000"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, sec);

        CompanyBasicInfo info = repository.findCompanyBasicInfo("600013");

        assertNotNull(info);
        assertEquals("600013", info.getStockCode());
        assertEquals("基本信息", info.getStockName());
        assertEquals("信息技术", info.getIndustry());
        assertEquals("SH", info.getMarket());
        assertEquals(0, new BigDecimal("500000000").compareTo(info.getTotalShares()));
    }

    @Test
    void shouldReturnNullWhenCompanyNotFound() {
        CompanyBasicInfo info = repository.findCompanyBasicInfo("999999");
        assertNull(info);
    }

    @Test
    void shouldFindLatestOperatingCashFlow() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate,
                TestDataFactory.company("91110014", "现金流公司", "现金流"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate,
                TestDataFactory.security(companyId, "600014", "现金流"));

        TestDataFactory.insertFinancialReport(jdbcTemplate,
                TestDataFactory.report("600014", LocalDate.of(2023, 12, 31)));

        BigDecimal ocf = repository.findLatestOperatingCashFlow("600014");

        assertNotNull(ocf);
        assertEquals(0, new BigDecimal("4500000").compareTo(ocf));
    }

    @Test
    void shouldReturnNullWhenNoOperatingCashFlow() {
        BigDecimal ocf = repository.findLatestOperatingCashFlow("999999");
        assertNull(ocf);
    }

    @Test
    void shouldFindLatestFundamentalMetrics() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate,
                TestDataFactory.company("91110015", "基本面公司", "基本面"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate,
                TestDataFactory.security(companyId, "600015", "基本面"));

        TestDataFactory.insertStockFundamentalMetrics(jdbcTemplate, "600015", 2023,
                new BigDecimal("15.5"), new BigDecimal("0.10"), new BigDecimal("0.20"),
                new BigDecimal("120"), new BigDecimal("25"));

        Optional<StockFundamentalMetrics> result = repository.findLatestFundamentalMetrics("600015");

        assertTrue(result.isPresent());
        assertEquals("600015", result.get().getStockCode());
        assertEquals(2023, result.get().getReportYear());
        assertEquals(0, new BigDecimal("15.5").compareTo(result.get().getRoe()));
    }

    @Test
    void shouldReturnEmptyWhenNoFundamentalMetrics() {
        Optional<StockFundamentalMetrics> result = repository.findLatestFundamentalMetrics("999999");
        assertTrue(result.isEmpty());
    }
}
