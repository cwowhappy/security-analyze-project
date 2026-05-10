package com.example.securityanalyze.research.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.StockFundamentalMetricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(StockFundamentalMetricsRepositoryImpl.class)
class StockFundamentalMetricsRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private StockFundamentalMetricsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByStockCodeAndYear() {
        insertMetric("600001", 2023, new BigDecimal("10.5"), new BigDecimal("8.2"));

        Optional<StockFundamentalMetrics> result = repository.findByStockCodeAndYear("600001", 2023);

        assertTrue(result.isPresent());
        assertEquals("600001", result.get().getStockCode());
        assertEquals(2023, result.get().getReportYear());
        assertEquals(0, new BigDecimal("10.5").compareTo(result.get().getRoe()));
        assertEquals(0, new BigDecimal("8.2").compareTo(result.get().getRoa()));
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<StockFundamentalMetrics> result = repository.findByStockCodeAndYear("999999", 2023);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindByStockCode() {
        insertMetric("600002", 2021, new BigDecimal("5.0"), null);
        insertMetric("600002", 2022, new BigDecimal("6.0"), null);
        insertMetric("600002", 2023, new BigDecimal("7.0"), null);

        List<StockFundamentalMetrics> results = repository.findByStockCode("600002", 5);

        assertEquals(3, results.size());
        // 默认按 report_year DESC
        assertEquals(2023, results.get(0).getReportYear());
        assertEquals(2022, results.get(1).getReportYear());
        assertEquals(2021, results.get(2).getReportYear());
    }

    @Test
    void shouldRespectLimit() {
        insertMetric("600003", 2020, new BigDecimal("1.0"), null);
        insertMetric("600003", 2021, new BigDecimal("2.0"), null);
        insertMetric("600003", 2022, new BigDecimal("3.0"), null);

        List<StockFundamentalMetrics> results = repository.findByStockCode("600003", 2);

        assertEquals(2, results.size());
        assertEquals(2022, results.get(0).getReportYear());
        assertEquals(2021, results.get(1).getReportYear());
    }

    @Test
    void shouldBatchUpsert() {
        StockFundamentalMetrics m1 = new StockFundamentalMetrics();
        m1.setStockCode("600004");
        m1.setReportYear(2023);
        m1.setRoe(new BigDecimal("12.34"));
        m1.setRoa(new BigDecimal("5.67"));
        m1.setRevenueYoy(new BigDecimal("15.5"));

        StockFundamentalMetrics m2 = new StockFundamentalMetrics();
        m2.setStockCode("600004");
        m2.setReportYear(2022);
        m2.setRoe(new BigDecimal("10.00"));
        m2.setProfitYoy(new BigDecimal("8.8"));

        repository.batchUpsert(List.of(m1, m2));

        Optional<StockFundamentalMetrics> r1 = repository.findByStockCodeAndYear("600004", 2023);
        assertTrue(r1.isPresent());
        assertEquals(0, new BigDecimal("12.34").compareTo(r1.get().getRoe()));

        Optional<StockFundamentalMetrics> r2 = repository.findByStockCodeAndYear("600004", 2022);
        assertTrue(r2.isPresent());
        assertEquals(0, new BigDecimal("10.00").compareTo(r2.get().getRoe()));
    }

    @Test
    void shouldUpdateOnConflict() {
        insertMetric("600005", 2023, new BigDecimal("5.0"), new BigDecimal("3.0"));

        StockFundamentalMetrics m = new StockFundamentalMetrics();
        m.setStockCode("600005");
        m.setReportYear(2023);
        m.setRoe(new BigDecimal("9.99"));
        m.setRoa(new BigDecimal("4.44"));

        repository.batchUpsert(List.of(m));

        Optional<StockFundamentalMetrics> result = repository.findByStockCodeAndYear("600005", 2023);
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("9.99").compareTo(result.get().getRoe()));
        assertEquals(0, new BigDecimal("4.44").compareTo(result.get().getRoa()));
    }

    @Test
    void shouldSoftDelete() {
        insertMetric("600006", 2023, new BigDecimal("7.0"), null);

        repository.deleteByStockCodeAndYear("600006", 2023);

        Optional<StockFundamentalMetrics> result = repository.findByStockCodeAndYear("600006", 2023);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotReturnDeletedRecords() {
        insertMetric("600007", 2023, new BigDecimal("7.0"), null);
        insertMetric("600007", 2022, new BigDecimal("6.0"), null);

        repository.deleteByStockCodeAndYear("600007", 2023);

        List<StockFundamentalMetrics> results = repository.findByStockCode("600007", 5);
        assertEquals(1, results.size());
        assertEquals(2022, results.get(0).getReportYear());
    }

    @Test
    void shouldHandleNullValues() {
        StockFundamentalMetrics m = new StockFundamentalMetrics();
        m.setStockCode("600008");
        m.setReportYear(2023);
        m.setRoe(new BigDecimal("8.88"));
        // 其余字段保持 null

        repository.batchUpsert(List.of(m));

        Optional<StockFundamentalMetrics> result = repository.findByStockCodeAndYear("600008", 2023);
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("8.88").compareTo(result.get().getRoe()));
        assertNull(result.get().getRoa());
        assertNull(result.get().getRevenueYoy());
    }

    private void insertMetric(String stockCode, int year, BigDecimal roe, BigDecimal roa) {
        String sql = """
                INSERT INTO stock_fundamental_metrics
                    (stock_code, report_year, roe, roa, is_deleted, created_at, updated_at)
                VALUES (:stockCode, :year, :roe, :roa, FALSE, NOW(), NOW())
                ON CONFLICT (stock_code, report_year) DO UPDATE SET
                    roe = EXCLUDED.roe, roa = EXCLUDED.roa, is_deleted = FALSE, updated_at = NOW()
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);
        params.addValue("year", year);
        params.addValue("roe", roe);
        params.addValue("roa", roa);
        jdbcTemplate.update(sql, params);
    }
}
