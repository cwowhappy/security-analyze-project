package com.example.securityanalyze.portfolio.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.PositionRepository;
import com.example.securityanalyze.user.domain.UserStatus;
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

@Import({PositionRepositoryImpl.class, PortfolioRepositoryImpl.class})
class PositionRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private Long createPortfolio() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("posuser", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "持仓测试组合", PortfolioType.REAL);
        return TestDataFactory.insertPortfolio(jdbcTemplate, p);
    }

    @Test
    void shouldSaveAndFindById() {
        Long portfolioId = createPortfolio();
        Position pos = TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688"));

        Position saved = positionRepository.save(pos);
        assertNotNull(saved.getId());

        Optional<Position> found = positionRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("100").compareTo(found.get().getCurrentQuantity()));
    }

    @Test
    void shouldFindByPortfolioIdAndStockCode() {
        Long portfolioId = createPortfolio();
        Position pos = TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688"));
        positionRepository.save(pos);

        Optional<Position> found = positionRepository.findByPortfolioIdAndStockCode(portfolioId, "600519");
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("1688").compareTo(found.get().getAvgCost()));
    }

    @Test
    void shouldFindByPortfolioId() {
        Long portfolioId = createPortfolio();
        positionRepository.save(TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688")));
        positionRepository.save(TestDataFactory.position(portfolioId, "000001", new BigDecimal("500"),
                new BigDecimal("5000"), new BigDecimal("10")));

        List<Position> list = positionRepository.findByPortfolioId(portfolioId);
        assertEquals(2, list.size());
    }

    @Test
    void shouldUpdatePosition() {
        Long portfolioId = createPortfolio();
        Position pos = TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688"));
        Position saved = positionRepository.save(pos);

        saved.setCurrentQuantity(new BigDecimal("200"));
        saved.setTotalCost(new BigDecimal("337600"));
        saved.setAvgCost(new BigDecimal("1688"));
        positionRepository.update(saved);

        Optional<Position> found = positionRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("200").compareTo(found.get().getCurrentQuantity()));
    }

    @Test
    void shouldSoftDelete() {
        Long portfolioId = createPortfolio();
        Position pos = TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688"));
        Position saved = positionRepository.save(pos);

        positionRepository.softDelete(saved.getId());

        Optional<Position> found = positionRepository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldSoftDeleteByPortfolioIdAndStockCode() {
        Long portfolioId = createPortfolio();
        Position pos = TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688"));
        positionRepository.save(pos);

        positionRepository.softDeleteByPortfolioIdAndStockCode(portfolioId, "600519");

        Optional<Position> found = positionRepository.findByPortfolioIdAndStockCode(portfolioId, "600519");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<Position> found = positionRepository.findById(99999L);
        assertTrue(found.isEmpty());
    }

    // ------------------------------------------------------------------
    // findByPortfolioIdWithQuote() 集成测试（持仓分析 SQL 查询）
    // ------------------------------------------------------------------

    @Test
    void shouldReturnPositionWithQuoteAndCompanyInfo() {
        Long portfolioId = createPortfolio();

        Company company = TestDataFactory.company("91110000", "贵州茅台酒股份有限公司", "贵州茅台");
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, company);

        CompanySecurity security = TestDataFactory.security(companyId, "600519", "贵州茅台");
        TestDataFactory.insertCompanySecurity(jdbcTemplate, security);

        Position pos = TestDataFactory.position(portfolioId, "600519", new BigDecimal("100"),
                new BigDecimal("168800"), new BigDecimal("1688"));
        pos.setFirstBuyDate(LocalDate.of(2026, 1, 1));
        pos.setLastTradeDate(LocalDate.of(2026, 5, 5));
        TestDataFactory.insertPosition(jdbcTemplate, pos);

        TestDataFactory.insertDailyQuote(jdbcTemplate, "600519", LocalDate.of(2026, 5, 5),
                new BigDecimal("1700"), new BigDecimal("1720"), new BigDecimal("1690"),
                new BigDecimal("1710"), 10000L, new BigDecimal("17100000"));

        List<Map<String, Object>> rows = positionRepository.findByPortfolioIdWithQuote(portfolioId);

        assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals("600519", row.get("stock_code"));
        assertEquals("贵州茅台", row.get("stock_name"));
        assertEquals("SH", row.get("market"));
        assertEquals("信息技术", row.get("industry"));
        assertEquals(0, new BigDecimal("1710").compareTo(toBigDecimal(row.get("close_price"))));
    }

    @Test
    void shouldReturnPositionWithoutQuoteWhenNoDailyQuote() {
        Long portfolioId = createPortfolio();

        Company company = TestDataFactory.company("91110001", "测试公司", "测试");
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, company);
        CompanySecurity security = TestDataFactory.security(companyId, "000001", "平安银行");
        TestDataFactory.insertCompanySecurity(jdbcTemplate, security);

        Position pos = TestDataFactory.position(portfolioId, "000001", new BigDecimal("500"),
                new BigDecimal("5000"), new BigDecimal("10"));
        TestDataFactory.insertPosition(jdbcTemplate, pos);

        List<Map<String, Object>> rows = positionRepository.findByPortfolioIdWithQuote(portfolioId);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("close_price"));
        assertEquals("平安银行", rows.get(0).get("stock_name"));
    }

    @Test
    void shouldReturnPositionWithoutCompanyInfoWhenNoSecurityMapping() {
        Long portfolioId = createPortfolio();
        Position pos = TestDataFactory.position(portfolioId, "999999", new BigDecimal("100"),
                new BigDecimal("10000"), new BigDecimal("100"));
        TestDataFactory.insertPosition(jdbcTemplate, pos);

        List<Map<String, Object>> rows = positionRepository.findByPortfolioIdWithQuote(portfolioId);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("stock_name"));
        assertNull(rows.get(0).get("industry"));
        assertNull(rows.get(0).get("market"));
    }

    @Test
    void shouldReturnEmptyListWhenNoPositionsForQuote() {
        Long portfolioId = createPortfolio();
        List<Map<String, Object>> rows = positionRepository.findByPortfolioIdWithQuote(portfolioId);
        assertTrue(rows.isEmpty());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return new BigDecimal(val.toString());
    }
}
