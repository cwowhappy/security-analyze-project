package com.example.securityanalyze.portfolio.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioRepository;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserStatus;
import com.example.securityanalyze.user.infrastructure.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import({PortfolioRepositoryImpl.class, UserRepositoryImpl.class})
class PortfolioRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindById() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test1", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "测试组合", PortfolioType.REAL);

        Portfolio saved = portfolioRepository.save(p);
        assertNotNull(saved.getId());
        assertEquals("测试组合", saved.getName());

        Optional<Portfolio> found = portfolioRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("测试组合", found.get().getName());
        assertEquals(PortfolioType.REAL, found.get().getType());
    }

    @Test
    void shouldFindByIdAndUserId() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test2", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "组合A", PortfolioType.REAL);
        Portfolio saved = portfolioRepository.save(p);

        Optional<Portfolio> found = portfolioRepository.findByIdAndUserId(saved.getId(), userId);
        assertTrue(found.isPresent());

        Optional<Portfolio> notFound = portfolioRepository.findByIdAndUserId(saved.getId(), 99999L);
        assertTrue(notFound.isEmpty());
    }

    @Test
    void shouldFindByUserId() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test3", UserStatus.APPROVED));
        portfolioRepository.save(TestDataFactory.portfolio(userId, "组合1", PortfolioType.REAL));
        portfolioRepository.save(TestDataFactory.portfolio(userId, "组合2", PortfolioType.SIMULATION));

        List<Portfolio> list = portfolioRepository.findByUserId(userId);
        assertEquals(2, list.size());
    }

    @Test
    void shouldUpdatePortfolio() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test4", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "旧名称", PortfolioType.REAL);
        Portfolio saved = portfolioRepository.save(p);

        saved.setName("新名称");
        saved.setBroker("新券商");
        portfolioRepository.update(saved);

        Optional<Portfolio> found = portfolioRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("新名称", found.get().getName());
        assertEquals("新券商", found.get().getBroker());
    }

    @Test
    void shouldSoftDelete() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test5", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "待删除", PortfolioType.REAL);
        Portfolio saved = portfolioRepository.save(p);

        portfolioRepository.softDelete(saved.getId());

        Optional<Portfolio> found = portfolioRepository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldSoftDeleteByUserId() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test6", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "待删除", PortfolioType.REAL);
        Portfolio saved = portfolioRepository.save(p);

        portfolioRepository.softDeleteByUserId(saved.getId(), userId);

        Optional<Portfolio> found = portfolioRepository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldNotDeleteWhenUserIdMismatch() {
        Long userId = TestDataFactory.insertUser(jdbcTemplate, TestDataFactory.user("test7", UserStatus.APPROVED));
        Portfolio p = TestDataFactory.portfolio(userId, "保护组合", PortfolioType.REAL);
        Portfolio saved = portfolioRepository.save(p);

        portfolioRepository.softDeleteByUserId(saved.getId(), 99999L);

        Optional<Portfolio> found = portfolioRepository.findById(saved.getId());
        assertTrue(found.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenIdNotFound() {
        Optional<Portfolio> found = portfolioRepository.findById(99999L);
        assertTrue(found.isEmpty());
    }
}
