package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialBalanceRepository;
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
class JdbcFinancialBalanceRepositoryTest {

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
    private FinancialBalanceRepository balanceRepository;

    @Test
    @Transactional
    void shouldSaveAndFindBalanceByStockCode() {
        FinancialBalance balance = buildBalance("000001", LocalDate.of(2024, 12, 31), "Y");

        balanceRepository.save(balance);
        List<FinancialBalance> found = balanceRepository.findByStockCode("000001");

        assertThat(found).hasSize(1);
        FinancialBalance result = found.get(0);
        assertThat(result.getStockCode()).isEqualTo("000001");
        assertThat(result.getTotalAssets()).isEqualByComparingTo("500000000.00");
        assertThat(result.getTotalLiabilities()).isEqualByComparingTo("300000000.00");
    }

    @Test
    @Transactional
    void shouldFindByStockCodeAndReportType() {
        balanceRepository.save(buildBalance("000001", LocalDate.of(2024, 12, 31), "Y"));
        balanceRepository.save(buildBalance("000001", LocalDate.of(2024, 9, 30), "Q3"));

        List<FinancialBalance> results = balanceRepository.findByStockCode("000001", "Y", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReportType()).isEqualTo("Y");
    }

    @Test
    @Transactional
    void shouldFindLatestByReportType() {
        balanceRepository.save(buildBalance("000001", LocalDate.of(2023, 12, 31), "Y"));
        balanceRepository.save(buildBalance("000001", LocalDate.of(2024, 12, 31), "Y"));

        Optional<FinancialBalance> latest = balanceRepository.findLatest("000001", "Y");

        assertThat(latest).isPresent();
        assertThat(latest.get().getReportDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @Transactional
    void shouldUpsertWhenSaveExistingRecord() {
        FinancialBalance balance = buildBalance("000001", LocalDate.of(2024, 12, 31), "Y");
        balanceRepository.save(balance);

        FinancialBalance updated = FinancialBalance.builder()
                .id(balance.getId())
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .totalAssets(new BigDecimal("600000000.00"))
                .totalLiabilities(new BigDecimal("350000000.00"))
                .build();
        balanceRepository.save(updated);

        Optional<FinancialBalance> found = balanceRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y");
        assertThat(found).isPresent();
        assertThat(found.get().getTotalAssets()).isEqualByComparingTo("600000000.00");
    }

    @Test
    @Transactional
    void shouldSaveAllBatch() {
        FinancialBalance b1 = buildBalance("000001", LocalDate.of(2024, 12, 31), "Y");
        FinancialBalance b2 = buildBalance("000001", LocalDate.of(2023, 12, 31), "Y");

        balanceRepository.saveAll(List.of(b1, b2));

        List<FinancialBalance> found = balanceRepository.findByStockCode("000001");
        assertThat(found).hasSize(2);
    }

    private FinancialBalance buildBalance(String stockCode, LocalDate reportDate, String reportType) {
        return FinancialBalance.builder()
                .stockCode(stockCode)
                .reportDate(reportDate)
                .reportType(reportType)
                .totalAssets(new BigDecimal("500000000.00"))
                .totalLiabilities(new BigDecimal("300000000.00"))
                .totalEquity(new BigDecimal("200000000.00"))
                .equityParentCompany(new BigDecimal("190000000.00"))
                .currentAssets(new BigDecimal("250000000.00"))
                .nonCurrentAssets(new BigDecimal("250000000.00"))
                .cashEquivalents(new BigDecimal("50000000.00"))
                .accountsReceivable(new BigDecimal("30000000.00"))
                .inventories(new BigDecimal("40000000.00"))
                .currentLiabilities(new BigDecimal("150000000.00"))
                .nonCurrentLiabilities(new BigDecimal("150000000.00"))
                .accountsPayable(new BigDecimal("25000000.00"))
                .shortTermBorrowings(new BigDecimal("50000000.00"))
                .longTermBorrowings(new BigDecimal("80000000.00"))
                .goodwill(new BigDecimal("10000000.00"))
                .build();
    }
}
