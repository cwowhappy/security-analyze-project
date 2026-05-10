package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
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
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110001", "测试公司", "测试"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600001", "测试A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600002", "测试B"));

        List<CompanySecurity> results = companySecurityRepository.findByCompanyId(companyId);

        assertEquals(2, results.size());
    }

    @Test
    void shouldFindByCompanyIds() {
        Long companyId1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110002", "公司A", "A"));
        Long companyId2 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110003", "公司B", "B"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId1, "600003", "证券A"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId2, "600004", "证券B"));

        List<CompanySecurity> results = companySecurityRepository.findByCompanyIds(List.of(companyId1, companyId2));

        assertEquals(2, results.size());
    }

    @Test
    void shouldReturnEmptyListWhenCompanyIdsEmpty() {
        List<CompanySecurity> results = companySecurityRepository.findByCompanyIds(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFindByStockCode() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110004", "代码测试", "代码"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600005", "代码证券"));

        Optional<CompanySecurity> result = companySecurityRepository.findByStockCode("600005");

        assertTrue(result.isPresent());
        assertEquals("代码证券", result.get().getStockName());
    }

    @Test
    void shouldReturnEmptyWhenStockCodeNotFound() {
        Optional<CompanySecurity> result = companySecurityRepository.findByStockCode("999999");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindByKeywordWithExactCodeMatch() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110005", "关键词测试", "关键"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600006", "关键词证券"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword("600006", 0, 10);

        assertEquals(1, results.size());
        assertEquals("600006", results.get(0).getStockCode());
    }

    @Test
    void shouldFindByKeywordWithPrefixNameMatch() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110006", "前缀测试", "前缀"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600007", "前缀证券"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword("前缀", 0, 10);

        assertEquals(1, results.size());
        assertEquals("前缀证券", results.get(0).getStockName());
    }

    @Test
    void shouldReturnAllWhenKeywordNull() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110007", "空关键词", "空词"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600008", "空词证券"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword(null, 0, 10);

        assertTrue(results.size() >= 1);
    }

    @Test
    void shouldCountByKeyword() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110008", "计数测试", "计数"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600009", "计数证券"));

        long countWithKeyword = companySecurityRepository.countByKeyword("600009");
        long countAll = companySecurityRepository.countByKeyword(null);

        assertEquals(1L, countWithKeyword);
        assertTrue(countAll >= 1L);
    }

    @Test
    void shouldReturnEmptyListWhenCompanyIdsNull() {
        List<CompanySecurity> results = companySecurityRepository.findByCompanyIds(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnAllWhenKeywordBlank() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110009", "空白关键词", "空白"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600010", "空白证券"));

        List<CompanySecurity> results = companySecurityRepository.findByKeyword("", 0, 10);
        assertTrue(results.stream().anyMatch(r -> "600010".equals(r.getStockCode())));
    }

    @Test
    void shouldCountAllWhenKeywordBlank() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110010", "计数空白", "计数空白"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600011", "计数空白证券"));

        long count = companySecurityRepository.countByKeyword("   ");
        assertTrue(count >= 1L);
    }
}
