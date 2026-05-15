package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIncomeRepository;
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

/**
 * JdbcFinancialIncomeRepository 集成测试。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcFinancialIncomeRepositoryTest {

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
    private FinancialIncomeRepository incomeRepository;

    @Test
    @Transactional
    void shouldSaveAndFindIncomeByStockCode() {
        FinancialIncome income = buildIncome("000001", LocalDate.of(2024, 12, 31), "Y");

        incomeRepository.save(income);
        List<FinancialIncome> found = incomeRepository.findByStockCode("000001");

        assertThat(found).hasSize(1);
        FinancialIncome result = found.get(0);
        assertThat(result.getStockCode()).isEqualTo("000001");
        assertThat(result.getReportDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(result.getReportType()).isEqualTo("Y");
        assertThat(result.getRevenue()).isEqualByComparingTo("100000000.00");
        assertThat(result.getNetProfit()).isEqualByComparingTo("15000000.00");
    }

    @Test
    @Transactional
    void shouldFindByStockCodeAndReportType() {
        incomeRepository.save(buildIncome("000001", LocalDate.of(2024, 12, 31), "Y"));
        incomeRepository.save(buildIncome("000001", LocalDate.of(2024, 9, 30), "Q3"));
        incomeRepository.save(buildIncome("000001", LocalDate.of(2023, 12, 31), "Y"));

        List<FinancialIncome> results = incomeRepository.findByStockCode("000001", "Y", 10);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(FinancialIncome::getReportType).containsOnly("Y");
    }

    @Test
    @Transactional
    void shouldFindLatestByReportType() {
        incomeRepository.save(buildIncome("000001", LocalDate.of(2023, 12, 31), "Y"));
        incomeRepository.save(buildIncome("000001", LocalDate.of(2024, 12, 31), "Y"));

        Optional<FinancialIncome> latest = incomeRepository.findLatest("000001", "Y");

        assertThat(latest).isPresent();
        assertThat(latest.get().getReportDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @Transactional
    void shouldFindByStockCodeAndReportDate() {
        incomeRepository.save(buildIncome("000001", LocalDate.of(2024, 12, 31), "Y"));

        Optional<FinancialIncome> found = incomeRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y");

        assertThat(found).isPresent();
        assertThat(found.get().getRevenue()).isEqualByComparingTo("100000000.00");
    }

    @Test
    @Transactional
    void shouldUpsertWhenSaveExistingRecord() {
        FinancialIncome income = buildIncome("000001", LocalDate.of(2024, 12, 31), "Y");
        incomeRepository.save(income);

        FinancialIncome updated = FinancialIncome.builder()
                .id(income.getId())
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .revenue(new BigDecimal("200000000.00"))
                .netProfit(new BigDecimal("30000000.00"))
                .build();
        incomeRepository.save(updated);

        Optional<FinancialIncome> found = incomeRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y");
        assertThat(found).isPresent();
        assertThat(found.get().getRevenue()).isEqualByComparingTo("200000000.00");
        assertThat(found.get().getNetProfit()).isEqualByComparingTo("30000000.00");
    }

    @Test
    @Transactional
    void shouldSaveAllBatch() {
        FinancialIncome i1 = buildIncome("000001", LocalDate.of(2024, 12, 31), "Y");
        FinancialIncome i2 = buildIncome("000001", LocalDate.of(2023, 12, 31), "Y");

        incomeRepository.saveAll(List.of(i1, i2));

        List<FinancialIncome> found = incomeRepository.findByStockCode("000001");
        assertThat(found).hasSize(2);
    }

    private FinancialIncome buildIncome(String stockCode, LocalDate reportDate, String reportType) {
        return FinancialIncome.builder()
                .stockCode(stockCode)
                .reportDate(reportDate)
                .reportType(reportType)
                .basicEps(new BigDecimal("1.50"))
                .dilutedEps(new BigDecimal("1.48"))
                .totalRevenue(new BigDecimal("120000000.00"))
                .revenue(new BigDecimal("100000000.00"))
                .operatingCost(new BigDecimal("60000000.00"))
                .grossProfit(new BigDecimal("40000000.00"))
                .sellingExpense(new BigDecimal("5000000.00"))
                .adminExpense(new BigDecimal("8000000.00"))
                .rdExpense(new BigDecimal("3000000.00"))
                .financialExpense(new BigDecimal("2000000.00"))
                .operatingProfit(new BigDecimal("20000000.00"))
                .totalProfit(new BigDecimal("21000000.00"))
                .netProfit(new BigDecimal("15000000.00"))
                .npParentCompany(new BigDecimal("14000000.00"))
                .npExclNonrecurring(new BigDecimal("13000000.00"))
                .build();
    }
}
