package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.stock.domain.model.Stock;
import org.cwowhappy.securityanalyze.stock.domain.model.StockId;
import org.cwowhappy.securityanalyze.stock.domain.repository.StockRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcStockRepository 集成测试。
 * 使用 Testcontainers 启动真实 PostgreSQL，验证 JDBC 实现的持久化语义。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcStockRepositoryTest {

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
    private StockRepository stockRepository;

    @Test
    @Transactional
    void shouldSaveAndFindStockByStockCode() {
        // 给定
        Stock stock = buildStock("000001", "000001.SZ", "平安银行");

        // 当
        StockId savedId = stockRepository.save(stock);
        Optional<Stock> found = stockRepository.findByStockCode("000001");

        // 则
        assertThat(found).isPresent();
        Stock result = found.get();
        assertThat(result.getId()).isEqualTo(savedId);
        assertThat(result.getStockCode()).isEqualTo("000001");
        assertThat(result.getTsCode()).isEqualTo("000001.SZ");
        assertThat(result.getName()).isEqualTo("平安银行");
        assertThat(result.getFullName()).isEqualTo("平安银行股份有限公司");
        assertThat(result.getMarket()).isEqualTo("主板");
        assertThat(result.getExchange()).isEqualTo("SZ");
        assertThat(result.getListDate()).isEqualTo(LocalDate.of(1991, 4, 3));
        assertThat(result.getIndustry()).isEqualTo("银行");
        assertThat(result.getArea()).isEqualTo("深圳");
        assertThat(result.getTotalShares()).isEqualTo(19405918198L);
        assertThat(result.getFloatShares()).isEqualTo(19405562184L);
    }

    @Test
    @Transactional
    void shouldFindAllStocks() {
        // 给定
        Stock stock1 = buildStock("000001", "000001.SZ", "平安银行");
        Stock stock2 = buildStock("000002", "000002.SZ", "万科A");
        stockRepository.save(stock1);
        stockRepository.save(stock2);

        // 当
        List<Stock> all = stockRepository.findAll();

        // 则
        assertThat(all).hasSize(2);
        assertThat(all).extracting(Stock::getStockCode).containsExactlyInAnyOrder("000001", "000002");
    }

    @Test
    @Transactional
    void shouldUpdateStockWhenSaveExistingId() {
        // 给定：先保存一条记录
        StockId id = StockId.generate();
        Stock stock = Stock.builder()
                .id(id)
                .stockCode("000001")
                .tsCode("000001.SZ")
                .name("平安银行")
                .fullName("平安银行股份有限公司")
                .market("主板")
                .exchange("SZ")
                .listDate(LocalDate.of(1991, 4, 3))
                .industry("银行")
                .area("深圳")
                .totalShares(19405918198L)
                .floatShares(19405562184L)
                .build();
        stockRepository.save(stock);

        // 当：使用相同 ID，修改名称与总股本后再次保存（Upsert）
        Stock updated = Stock.builder()
                .id(id)
                .stockCode("000001")
                .tsCode("000001.SZ")
                .name("平安银行（已更名）")
                .fullName("平安银行股份有限公司")
                .market("主板")
                .exchange("SZ")
                .listDate(LocalDate.of(1991, 4, 3))
                .industry("银行")
                .area("深圳")
                .totalShares(20000000000L)
                .floatShares(19405562184L)
                .build();
        stockRepository.save(updated);

        // 则
        Optional<Stock> found = stockRepository.findByStockCode("000001");
        assertThat(found).isPresent();
        Stock result = found.get();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("平安银行（已更名）");
        assertThat(result.getTotalShares()).isEqualTo(20000000000L);
    }

    private Stock buildStock(String stockCode, String tsCode, String name) {
        return Stock.builder()
                .id(StockId.generate())
                .stockCode(stockCode)
                .tsCode(tsCode)
                .name(name)
                .fullName(name + "股份有限公司")
                .market("主板")
                .exchange("SZ")
                .listDate(LocalDate.of(1991, 4, 3))
                .industry("银行")
                .area("深圳")
                .totalShares(19405918198L)
                .floatShares(19405562184L)
                .build();
    }
}
