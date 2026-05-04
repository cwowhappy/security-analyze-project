package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(CompanySecurityRepositoryImpl.class)
class CompanySecurityRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private CompanySecurityRepository companySecurityRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByCompanyId() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110012", "集团A", "集A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600001", "股票A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600002", "股票B"));

        List<CompanySecurity> results = companySecurityRepository.findByCompanyId(companyId);

        assertEquals(2, results.size());
        assertEquals("600001", results.get(0).getStockCode());
        assertEquals("600002", results.get(1).getStockCode());
    }

    @Test
    void shouldFindByStockCode() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110013", "集团B", "集B"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600003", "股票C"));

        Optional<CompanySecurity> found = companySecurityRepository.findByStockCode("600003");

        assertTrue(found.isPresent());
        assertEquals("股票C", found.get().getStockName());
        assertEquals(companyId, found.get().getCompanyId());
    }

    @Test
    void shouldReturnEmptyWhenStockCodeNotFound() {
        Optional<CompanySecurity> found = companySecurityRepository.findByStockCode("999999");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindByKeywordUsingStockCode() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110014", "集团C", "集C"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "000001", "平安银行"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword("000001", 0, 10);

        assertEquals(1, results.size());
        assertEquals("000001", results.get(0).getStockCode());
    }

    @Test
    void shouldFindByKeywordUsingStockNamePrefix() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110015", "集团D", "集D"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "000002", "万科A"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword("万科", 0, 10);

        assertEquals(1, results.size());
        assertEquals("万科A", results.get(0).getStockName());
    }

    @Test
    void shouldReturnAllWhenKeywordNullOrBlank() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110016", "集团E", "集E"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600004", "股票D"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600005", "股票E"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword(null, 0, 10);

        assertTrue(results.size() >= 2);
    }

    @Test
    void shouldCountByKeyword() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110017", "集团F", "集F"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600006", "茅台股份"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600007", "其他股份"));

        long countWithKeyword = companySecurityRepository.countByKeyword("茅台");
        long countAll = companySecurityRepository.countByKeyword(null);

        assertEquals(1L, countWithKeyword);
        assertTrue(countAll >= 2L);
    }

    @Test
    void shouldReturnEmptyWhenCompanyIdNotFound() {
        List<CompanySecurity> results = companySecurityRepository.findByCompanyId(99999L);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenOffsetExceedsTotal() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110018", "集团G", "集G"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600008", "股票G"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword(null, 1000, 10);

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHandleKeywordCaseInsensitive() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110019", "集团H", "集H"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600009", "ABC银行"));

        List<CompanySecurity> upperCase = companySecurityRepository.findByKeyword("ABC", 0, 10);
        List<CompanySecurity> lowerCase = companySecurityRepository.findByKeyword("abc", 0, 10);

        assertEquals(1, upperCase.size(), "stock_name ILIKE 应支持大小写不敏感");
        assertEquals(1, lowerCase.size(), "stock_name ILIKE 应支持大小写不敏感");
    }
}
