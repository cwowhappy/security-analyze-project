package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(CompanyRepositoryImpl.class)
class CompanyRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindById() {
        Company company = TestDataFactory.company("91110000", "测试公司", "测公");
        Long id = TestDataFactory.insertCompany(jdbcTemplate, company);

        Optional<Company> found = companyRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals("测试公司", found.get().getCompanyName());
        assertEquals("测公", found.get().getShortName());
    }

    @Test
    void shouldReturnEmptyWhenIdNotFound() {
        Optional<Company> found = companyRepository.findById(99999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindAllById() {
        Long id1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110001", "公司A", "A"));
        Long id2 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110002", "公司B", "B"));
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110003", "公司C", "C"));

        List<Company> results = companyRepository.findAllById(List.of(id1, id2));

        assertEquals(2, results.size());
        List<String> names = results.stream().map(Company::getCompanyName).toList();
        assertTrue(names.contains("公司A"));
        assertTrue(names.contains("公司B"));
    }

    @Test
    void shouldReturnEmptyListWhenIdsEmpty() {
        List<Company> results = companyRepository.findAllById(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFindByKeyword() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110004", "贵州茅台", "茅台"));
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110005", "五浪液", "五浪"));

        List<Company> results = companyRepository.findByKeyword("茅台", 0, 10);

        assertEquals(1, results.size());
        assertEquals("贵州茅台", results.get(0).getCompanyName());
    }

    @Test
    void shouldFindByKeywordMatchingShortName() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110006", "全名很长", "短名"));

        List<Company> results = companyRepository.findByKeyword("短名", 0, 10);

        assertEquals(1, results.size());
        assertEquals("全名很长", results.get(0).getCompanyName());
    }

    @Test
    void shouldReturnAllWhenKeywordNullOrBlank() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110007", "公司D", "D"));
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110008", "公司E", "E"));

        List<Company> results = companyRepository.findByKeyword(null, 0, 10);

        assertTrue(results.size() >= 2);
    }

    @Test
    void shouldCountByKeyword() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110009", "关键字测试", "关键"));
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110010", "其他公司", "其他"));

        long countWithKeyword = companyRepository.countByKeyword("关键字");
        long countAll = companyRepository.countByKeyword(null);

        assertEquals(1L, countWithKeyword);
        assertTrue(countAll >= 2L);
    }

    @Test
    void shouldFindByStockCode() {
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110011", "关联公司", "关联"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600519", "贵州茅台"));

        Optional<Company> found = companyRepository.findByStockCode("600519");

        assertTrue(found.isPresent());
        assertEquals("关联公司", found.get().getCompanyName());
    }

    @Test
    void shouldReturnEmptyWhenStockCodeNotFound() {
        Optional<Company> found = companyRepository.findByStockCode("999999");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenOffsetExceedsTotal() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110012", "偏移测试", "偏移"));

        List<Company> results = companyRepository.findByKeyword(null, 1000, 10);

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHandleKeywordCaseInsensitive() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110013", "ABC科技公司", "ABC"));

        List<Company> upperCase = companyRepository.findByKeyword("ABC", 0, 10);
        List<Company> lowerCase = companyRepository.findByKeyword("abc", 0, 10);
        List<Company> mixedCase = companyRepository.findByKeyword("Abc", 0, 10);

        assertEquals(1, upperCase.size(), "ILIKE 应支持大小写不敏感");
        assertEquals(1, lowerCase.size(), "ILIKE 应支持大小写不敏感");
        assertEquals(1, mixedCase.size(), "ILIKE 应支持大小写不敏感");
        assertEquals("ABC科技公司", upperCase.get(0).getCompanyName());
    }

    @Test
    void shouldTrimKeyword() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110014", " 前后空格公司 ", "空格"));

        List<Company> withSpaces = companyRepository.findByKeyword(" 前后空格 ", 0, 10);

        assertEquals(1, withSpaces.size(), "关键字前后空格应被 trim");
        assertEquals(" 前后空格公司 ", withSpaces.get(0).getCompanyName());
    }
}
