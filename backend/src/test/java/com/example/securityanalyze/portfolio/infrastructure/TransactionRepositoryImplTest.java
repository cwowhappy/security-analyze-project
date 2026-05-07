package com.example.securityanalyze.portfolio.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
import com.example.securityanalyze.portfolio.domain.TransactionRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import({TransactionRepositoryImpl.class, PortfolioRepositoryImpl.class})
class TransactionRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private Long createPortfolio() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("txuser", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "交易测试组合", PortfolioType.REAL);
        return TestDataFactory.insertPortfolio(jdbcTemplate, p);
    }

    @Test
    void shouldSaveAndFindById() {
        Long portfolioId = createPortfolio();
        TransactionRecord tx = TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1688.00"), new BigDecimal("100"));

        TransactionRecord saved = transactionRepository.save(tx);
        assertNotNull(saved.getId());
        assertEquals(0, new BigDecimal("168800.00").compareTo(saved.getAmount()));

        Optional<TransactionRecord> found = transactionRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("600519", found.get().getStockCode());
    }

    @Test
    void shouldFindByIdAndPortfolioId() {
        Long portfolioId = createPortfolio();
        TransactionRecord tx = TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1688.00"), new BigDecimal("100"));
        TransactionRecord saved = transactionRepository.save(tx);

        Optional<TransactionRecord> found = transactionRepository.findByIdAndPortfolioId(saved.getId(), portfolioId);
        assertTrue(found.isPresent());

        Optional<TransactionRecord> notFound = transactionRepository.findByIdAndPortfolioId(saved.getId(), 99999L);
        assertTrue(notFound.isEmpty());
    }

    @Test
    void shouldFindByPortfolioIdWithPagination() {
        Long portfolioId = createPortfolio();
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100")));
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "000001", TradeType.SELL,
                new BigDecimal("10"), new BigDecimal("500")));

        List<TransactionRecord> list = transactionRepository.findByPortfolioId(portfolioId, null, null, null, null, 0, 10);
        assertEquals(2, list.size());
    }

    @Test
    void shouldFilterByStockCode() {
        Long portfolioId = createPortfolio();
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100")));
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "000001", TradeType.BUY,
                new BigDecimal("10"), new BigDecimal("500")));

        List<TransactionRecord> list = transactionRepository.findByPortfolioId(portfolioId, "600519", null, null, null, 0, 10);
        assertEquals(1, list.size());
        assertEquals("600519", list.get(0).getStockCode());
    }

    @Test
    void shouldFilterByTradeType() {
        Long portfolioId = createPortfolio();
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100")));
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "600519", TradeType.SELL,
                new BigDecimal("1100"), new BigDecimal("50")));

        List<TransactionRecord> list = transactionRepository.findByPortfolioId(portfolioId, null, TradeType.SELL, null, null, 0, 10);
        assertEquals(1, list.size());
        assertEquals(TradeType.SELL, list.get(0).getTradeType());
    }

    @Test
    void shouldFilterByDateRange() {
        Long portfolioId = createPortfolio();
        TransactionRecord tx1 = TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100"));
        tx1.setTradeDate(LocalDate.of(2026, 1, 1));
        TransactionRecord tx2 = TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1100"), new BigDecimal("100"));
        tx2.setTradeDate(LocalDate.of(2026, 3, 1));
        transactionRepository.save(tx1);
        transactionRepository.save(tx2);

        List<TransactionRecord> list = transactionRepository.findByPortfolioId(portfolioId, null, null, "2026-02-01", "2026-12-31", 0, 10);
        assertEquals(1, list.size());
        assertEquals(LocalDate.of(2026, 3, 1), list.get(0).getTradeDate());
    }

    @Test
    void shouldCountByPortfolioId() {
        Long portfolioId = createPortfolio();
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100")));

        long count = transactionRepository.countByPortfolioId(portfolioId, null, null, null, null);
        assertEquals(1, count);
    }

    @Test
    void shouldUpdateTransaction() {
        Long portfolioId = createPortfolio();
        TransactionRecord tx = TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100"));
        TransactionRecord saved = transactionRepository.save(tx);

        saved.setQuantity(new BigDecimal("200"));
        saved.setAmount(new BigDecimal("200000"));
        saved.setRemark("修改备注");
        transactionRepository.update(saved);

        Optional<TransactionRecord> found = transactionRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("200").compareTo(found.get().getQuantity()));
        assertEquals("修改备注", found.get().getRemark());
    }

    @Test
    void shouldSoftDelete() {
        Long portfolioId = createPortfolio();
        TransactionRecord tx = TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100"));
        TransactionRecord saved = transactionRepository.save(tx);

        transactionRepository.softDelete(saved.getId());

        Optional<TransactionRecord> found = transactionRepository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindActiveByPortfolioIdAndStockCode() {
        Long portfolioId = createPortfolio();
        transactionRepository.save(TestDataFactory.transaction(portfolioId, "600519", TradeType.BUY,
                new BigDecimal("1000"), new BigDecimal("100")));
        TransactionRecord tx2 = TestDataFactory.transaction(portfolioId, "600519", TradeType.SELL,
                new BigDecimal("1100"), new BigDecimal("50"));
        TransactionRecord saved2 = transactionRepository.save(tx2);
        transactionRepository.softDelete(saved2.getId());

        List<TransactionRecord> active = transactionRepository.findActiveByPortfolioIdAndStockCode(portfolioId, "600519");
        assertEquals(1, active.size());
        assertEquals(TradeType.BUY, active.get(0).getTradeType());
    }
}
