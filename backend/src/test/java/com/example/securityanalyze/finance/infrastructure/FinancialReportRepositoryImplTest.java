package com.example.securityanalyze.finance.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.finance.domain.FinancialReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(FinancialReportRepositoryImpl.class)
class FinancialReportRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private FinancialReportRepository financialReportRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertNewReport() {
        FinancialReport report = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        report.setTotalAssets(new BigDecimal("200000000"));
        report.setBalanceSheet(Map.of("assets", 200));

        financialReportRepository.save(report);

        Optional<FinancialReport> found = financialReportRepository.findByStockCodeAndReportDate("600519", LocalDate.of(2023, 12, 31));
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("200000000").compareTo(found.get().getTotalAssets()));
        assertNotNull(found.get().getId());
    }

    @Test
    void shouldUpdateExistingReport() {
        FinancialReport report = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        report.setTotalAssets(new BigDecimal("100000000"));
        Long id = TestDataFactory.insertFinancialReport(jdbcTemplate, report);

        FinancialReport toUpdate = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        toUpdate.setId(id);
        toUpdate.setTotalAssets(new BigDecimal("300000000"));
        toUpdate.setBalanceSheet(Map.of("assets", 300));
        financialReportRepository.save(toUpdate);

        Optional<FinancialReport> found = financialReportRepository.findById(id);
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("300000000").compareTo(found.get().getTotalAssets()));
    }

    @Test
    void shouldSaveAllWithMixedOps() {
        // insert
        FinancialReport r1 = TestDataFactory.report("600001", LocalDate.of(2023, 6, 30));
        r1.setTotalAssets(new BigDecimal("100"));
        // update (先插入)
        FinancialReport r2 = TestDataFactory.report("600002", LocalDate.of(2023, 6, 30));
        r2.setTotalAssets(new BigDecimal("200"));
        Long id2 = TestDataFactory.insertFinancialReport(jdbcTemplate, r2);
        r2.setId(id2);
        r2.setTotalAssets(new BigDecimal("250"));

        financialReportRepository.saveAll(List.of(r1, r2));

        Optional<FinancialReport> found1 = financialReportRepository.findByStockCodeAndReportDate("600001", LocalDate.of(2023, 6, 30));
        Optional<FinancialReport> found2 = financialReportRepository.findById(id2);
        assertTrue(found1.isPresent());
        assertEquals(0, new BigDecimal("100").compareTo(found1.get().getTotalAssets()));
        assertTrue(found2.isPresent());
        assertEquals(0, new BigDecimal("250").compareTo(found2.get().getTotalAssets()));
    }

    @Test
    void shouldFindByStockCode() {
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600003", LocalDate.of(2023, 12, 31)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600003", LocalDate.of(2023, 6, 30)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600004", LocalDate.of(2023, 12, 31)));

        List<FinancialReport> results = financialReportRepository.findByStockCode("600003");

        assertEquals(2, results.size());
        assertTrue(results.get(0).getReportDate().isAfter(results.get(1).getReportDate()));
    }

    @Test
    void shouldFindByStockCodeAndYear() {
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600005", LocalDate.of(2023, 12, 31)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600005", LocalDate.of(2022, 12, 31)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600005", LocalDate.of(2023, 6, 30)));

        List<FinancialReport> results = financialReportRepository.findByStockCodeAndYear("600005", 2023);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getReportYear() == 2023));
    }

    @Test
    void shouldFindByStockCodeAndDateRange() {
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600006", LocalDate.of(2023, 3, 31)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600006", LocalDate.of(2023, 6, 30)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600006", LocalDate.of(2023, 9, 30)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600006", LocalDate.of(2023, 12, 31)));

        List<FinancialReport> results = financialReportRepository.findByStockCodeAndDateRange(
                "600006", LocalDate.of(2023, 5, 1), LocalDate.of(2023, 10, 1));

        assertEquals(2, results.size());
    }

    @Test
    void shouldFindByStockCodeAndReportDate() {
        LocalDate reportDate = LocalDate.of(2023, 12, 31);
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600007", reportDate));

        Optional<FinancialReport> found = financialReportRepository.findByStockCodeAndReportDate("600007", reportDate);

        assertTrue(found.isPresent());
        assertEquals("600007", found.get().getStockCode());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<FinancialReport> found = financialReportRepository.findByStockCodeAndReportDate("999999", LocalDate.of(2023, 12, 31));
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCheckExists() {
        LocalDate reportDate = LocalDate.of(2023, 12, 31);
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600008", reportDate));

        assertTrue(financialReportRepository.existsByStockCodeAndReportDate("600008", reportDate));
        assertFalse(financialReportRepository.existsByStockCodeAndReportDate("600008", LocalDate.of(2022, 12, 31)));
    }

    @Test
    void shouldDoNothingWhenSaveAllEmptyOrNull() {
        // saveAll(null) 会 NPE，但空列表应正常返回
        assertDoesNotThrow(() -> financialReportRepository.saveAll(List.of()));
    }

    @Test
    void shouldHandleNullBigDecimalAndJsonb() {
        FinancialReport report = TestDataFactory.report("600009", LocalDate.of(2023, 12, 31));
        report.setTotalAssets(null);
        report.setTotalLiabilities(null);
        report.setBalanceSheet(null);
        report.setProfitSheet(null);
        report.setCashFlowSheet(null);

        financialReportRepository.save(report);

        Optional<FinancialReport> found = financialReportRepository.findByStockCodeAndReportDate("600009", LocalDate.of(2023, 12, 31));
        assertTrue(found.isPresent());
        assertNull(found.get().getTotalAssets(), "NULL BigDecimal 应正确映射为 null");
        assertNull(found.get().getTotalLiabilities(), "NULL BigDecimal 应正确映射为 null");
        assertNull(found.get().getBalanceSheet(), "NULL JSONB 应正确映射为 null");
        assertNull(found.get().getProfitSheet(), "NULL JSONB 应正确映射为 null");
        assertNull(found.get().getCashFlowSheet(), "NULL JSONB 应正确映射为 null");
    }

    @Test
    void shouldFindByStockCodeAndDateRangeWithExactBoundary() {
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600010", LocalDate.of(2023, 6, 30)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600010", LocalDate.of(2023, 9, 30)));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600010", LocalDate.of(2023, 12, 31)));

        // 边界：startDate = endDate，应只匹配当天
        List<FinancialReport> exact = financialReportRepository.findByStockCodeAndDateRange(
                "600010", LocalDate.of(2023, 9, 30), LocalDate.of(2023, 9, 30));
        assertEquals(1, exact.size());
        assertEquals(LocalDate.of(2023, 9, 30), exact.get(0).getReportDate());

        // 边界：包含首尾
        List<FinancialReport> inclusive = financialReportRepository.findByStockCodeAndDateRange(
                "600010", LocalDate.of(2023, 6, 30), LocalDate.of(2023, 12, 31));
        assertEquals(3, inclusive.size());
    }
}
