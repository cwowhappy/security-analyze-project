package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIndicatorRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcFinancialIndicatorRepositoryTest {

    private static final String POSTGRES_IMAGE = "postgres:" + System.getenv().getOrDefault("TESTCONTAINERS_POSTGRES_VERSION", "16");
    private static final String TEST_DB_NAME = System.getenv().getOrDefault("TEST_DB_NAME", "db-security-analyze");
    private static final String TEST_DB_USER = System.getenv().getOrDefault("TEST_DB_USER", "test");
    private static final String TEST_DB_PASSWORD = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName(TEST_DB_NAME)
            .withUsername(TEST_DB_USER)
            .withPassword(TEST_DB_PASSWORD);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FinancialIndicatorRepository indicatorRepository;

    @Test
    @Transactional
    void shouldSaveAndFindIndicatorByStockCode() {
        FinancialIndicator indicator = buildIndicator("000001", LocalDate.of(2024, 12, 31), "Y");
        indicatorRepository.save(indicator);
        List<FinancialIndicator> found = indicatorRepository.findByStockCode("000001");
        assertThat(found).hasSize(1);
        FinancialIndicator result = found.get(0);
        assertThat(result.getRoe()).isEqualByComparingTo("12.50");
        assertThat(result.getGrossMargin()).isEqualByComparingTo("35.00");
        assertThat(result.getDebtRatio()).isEqualByComparingTo("60.00");
    }

    @Test
    @Transactional
    void shouldFindLatestByReportType() {
        indicatorRepository.save(buildIndicator("000001", LocalDate.of(2023, 12, 31), "Y"));
        indicatorRepository.save(buildIndicator("000001", LocalDate.of(2024, 12, 31), "Y"));
        Optional<FinancialIndicator> latest = indicatorRepository.findLatest("000001", "Y");
        assertThat(latest).isPresent();
        assertThat(latest.get().getReportDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @Transactional
    void shouldUpsertWhenSaveExistingRecord() {
        FinancialIndicator indicator = buildIndicator("000001", LocalDate.of(2024, 12, 31), "Y");
        indicatorRepository.save(indicator);
        FinancialIndicator updated = FinancialIndicator.builder()
                .id(indicator.getId())
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .roe(new BigDecimal("15.00"))
                .build();
        indicatorRepository.save(updated);
        Optional<FinancialIndicator> found = indicatorRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y");
        assertThat(found).isPresent();
        assertThat(found.get().getRoe()).isEqualByComparingTo("15.00");
    }

    @Test
    @Transactional
    void shouldSaveAllBatch() {
        FinancialIndicator i1 = buildIndicator("000001", LocalDate.of(2024, 12, 31), "Y");
        FinancialIndicator i2 = buildIndicator("000001", LocalDate.of(2023, 12, 31), "Y");
        indicatorRepository.saveAll(List.of(i1, i2));
        List<FinancialIndicator> found = indicatorRepository.findByStockCode("000001");
        assertThat(found).hasSize(2);
    }

    private FinancialIndicator buildIndicator(String stockCode, LocalDate reportDate, String reportType) {
        return FinancialIndicator.builder()
                .stockCode(stockCode)
                .reportDate(reportDate)
                .reportType(reportType)
                .roe(new BigDecimal("12.50"))
                .roa(new BigDecimal("8.00"))
                .grossMargin(new BigDecimal("35.00"))
                .netMargin(new BigDecimal("15.00"))
                .debtRatio(new BigDecimal("60.00"))
                .currentRatio(new BigDecimal("1.50"))
                .quickRatio(new BigDecimal("1.20"))
                .assetTurnover(new BigDecimal("0.80"))
                .revenueGrowth(new BigDecimal("10.00"))
                .npParentGrowth(new BigDecimal("12.00"))
                .pe(new BigDecimal("15.00"))
                .pb(new BigDecimal("1.50"))
                .dataSource("CALCULATED")
                .build();
    }
}
