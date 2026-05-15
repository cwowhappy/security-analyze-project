package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialCashflow;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialCashflowRepository;
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
class JdbcFinancialCashflowRepositoryTest {

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
    private FinancialCashflowRepository cashflowRepository;

    @Test
    @Transactional
    void shouldSaveAndFindCashflowByStockCode() {
        FinancialCashflow cf = buildCashflow("000001", LocalDate.of(2024, 12, 31), "Y");
        cashflowRepository.save(cf);
        List<FinancialCashflow> found = cashflowRepository.findByStockCode("000001");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCfOperating()).isEqualByComparingTo("50000000.00");
    }

    @Test
    @Transactional
    void shouldFindLatestByReportType() {
        cashflowRepository.save(buildCashflow("000001", LocalDate.of(2023, 12, 31), "Y"));
        cashflowRepository.save(buildCashflow("000001", LocalDate.of(2024, 12, 31), "Y"));
        Optional<FinancialCashflow> latest = cashflowRepository.findLatest("000001", "Y");
        assertThat(latest).isPresent();
        assertThat(latest.get().getReportDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @Transactional
    void shouldUpsertWhenSaveExistingRecord() {
        FinancialCashflow cf = buildCashflow("000001", LocalDate.of(2024, 12, 31), "Y");
        cashflowRepository.save(cf);
        FinancialCashflow updated = FinancialCashflow.builder()
                .id(cf.getId())
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .cfOperating(new BigDecimal("60000000.00"))
                .build();
        cashflowRepository.save(updated);
        Optional<FinancialCashflow> found = cashflowRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y");
        assertThat(found).isPresent();
        assertThat(found.get().getCfOperating()).isEqualByComparingTo("60000000.00");
    }

    @Test
    @Transactional
    void shouldSaveAllBatch() {
        FinancialCashflow cf1 = buildCashflow("000001", LocalDate.of(2024, 12, 31), "Y");
        FinancialCashflow cf2 = buildCashflow("000001", LocalDate.of(2023, 12, 31), "Y");
        cashflowRepository.saveAll(List.of(cf1, cf2));
        List<FinancialCashflow> found = cashflowRepository.findByStockCode("000001");
        assertThat(found).hasSize(2);
    }

    private FinancialCashflow buildCashflow(String stockCode, LocalDate reportDate, String reportType) {
        return FinancialCashflow.builder()
                .stockCode(stockCode)
                .reportDate(reportDate)
                .reportType(reportType)
                .cfOperating(new BigDecimal("50000000.00"))
                .cfInvesting(new BigDecimal("-20000000.00"))
                .cfFinancing(new BigDecimal("-10000000.00"))
                .netCashFlow(new BigDecimal("20000000.00"))
                .freeCashFlow(new BigDecimal("30000000.00"))
                .capex(new BigDecimal("20000000.00"))
                .cashReceivedOperating(new BigDecimal("80000000.00"))
                .taxPaid(new BigDecimal("10000000.00"))
                .build();
    }
}
