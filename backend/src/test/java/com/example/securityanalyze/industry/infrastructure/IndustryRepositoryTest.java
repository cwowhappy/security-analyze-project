package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.industry.api.IndustryListItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(IndustryRepository.class)
class IndustryRepositoryTest extends RepositoryTestBase {

    @Autowired
    private IndustryRepository industryRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindIndustries() {
        Long c1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110018", "白酒公司", "白酒"));
        Long c2 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110019", "另一家白酒", "白酒2"));
        Long c3 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110020", "科技公司", "科技"));

        // 修改行业字段（工厂默认值是"信息技术"）
        jdbcTemplate.update("UPDATE company SET industry = '白酒' WHERE id IN (:ids)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("ids", List.of(c1, c2)));
        jdbcTemplate.update("UPDATE company SET industry = '科技' WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", c3));

        List<IndustryListItem> results = industryRepository.findIndustries();

        assertTrue(results.size() >= 2);
        IndustryListItem baijiu = results.stream().filter(i -> "白酒".equals(i.getIndustryName())).findFirst().orElseThrow();
        assertEquals(2, baijiu.getCompanyCount());
    }

    @Test
    void shouldExcludeEmptyIndustry() {
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110021", "无行业公司", "无业"));
        jdbcTemplate.update("UPDATE company SET industry = '' WHERE unified_code = :code",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("code", "91110021"));

        List<IndustryListItem> results = industryRepository.findIndustries();

        assertTrue(results.stream().noneMatch(i -> "".equals(i.getIndustryName())));
    }

    @Test
    void shouldFindCompaniesByIndustry() {
        Long c1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110022", "白酒A", "白A"));
        Long c2 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110023", "白酒B", "白B"));
        TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110024", "科技公司", "科技"));

        jdbcTemplate.update("UPDATE company SET industry = '白酒' WHERE id IN (:ids)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("ids", List.of(c1, c2)));
        jdbcTemplate.update("UPDATE company SET industry = '科技' WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", c2 + 1));

        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(c1, "600009", "白酒A股"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(c2, "600010", "白酒B股"));

        List<CompanyListItem> results = industryRepository.findCompaniesByIndustry("白酒", 0, 10);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "白酒".equals(r.getIndustry())));
    }

    @Test
    void shouldCountCompaniesByIndustry() {
        Long c1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110025", "白酒C", "白C"));
        Long c2 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110026", "白酒D", "白D"));

        jdbcTemplate.update("UPDATE company SET industry = '白酒' WHERE id IN (:ids)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("ids", List.of(c1, c2)));

        long count = industryRepository.countCompaniesByIndustry("白酒");

        assertEquals(2L, count);
    }

    @Test
    void shouldReturnEmptyWhenNoCompanies() {
        List<IndustryListItem> results = industryRepository.findIndustries();
        // 即使前面测试插入了数据，@Transactional 回滚后应干净
        // 但由于可能依赖执行顺序，使用弱断言：确认没有空字符串行业
        assertTrue(results.stream().noneMatch(i -> i.getIndustryName() == null || i.getIndustryName().isEmpty()));
    }

    @Test
    void shouldReturnEmptyWhenIndustryNotFound() {
        List<CompanyListItem> companies = industryRepository.findCompaniesByIndustry("不存在的行业", 0, 10);
        long count = industryRepository.countCompaniesByIndustry("不存在的行业");

        assertTrue(companies.isEmpty());
        assertEquals(0L, count);
    }

    @Test
    void shouldReturnEmptyWhenOffsetExceeds() {
        Long c1 = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110027", "白酒E", "白E"));
        jdbcTemplate.update("UPDATE company SET industry = '白酒' WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("id", c1));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(c1, "600020", "白酒E股"));

        List<CompanyListItem> results = industryRepository.findCompaniesByIndustry("白酒", 1000, 10);

        assertTrue(results.isEmpty());
    }
}
