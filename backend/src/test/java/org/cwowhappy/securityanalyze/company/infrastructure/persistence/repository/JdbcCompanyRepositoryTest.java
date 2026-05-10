package org.cwowhappy.securityanalyze.company.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.company.domain.model.Company;
import org.cwowhappy.securityanalyze.company.domain.model.CompanyId;
import org.cwowhappy.securityanalyze.company.domain.repository.CompanyRepository;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
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

/**
 * JdbcCompanyRepository 集成测试。
 * 使用 Testcontainers 启动真实 PostgreSQL，验证公司信息的持久化与查询能力。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcCompanyRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("db-security-analyze")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @Transactional
    void shouldSaveAndFindCompanyByUscCode() {
        // 给定
        Company company = buildCompany("9144030019218538XX", "平安银行股份有限公司", "银行");

        // 当
        CompanyId savedId = companyRepository.save(company);
        Optional<Company> found = companyRepository.findByUscCode("9144030019218538XX");

        // 则
        assertThat(found).isPresent();
        Company result = found.get();
        assertThat(result.getId()).isEqualTo(savedId);
        assertThat(result.getUnifiedSocialCreditCode()).isEqualTo("9144030019218538XX");
        assertThat(result.getName()).isEqualTo("平安银行股份有限公司");
        assertThat(result.getShortName()).isEqualTo("平安银行");
        assertThat(result.getIndustry()).isEqualTo("银行");
        assertThat(result.getProvince()).isEqualTo("广东省");
        assertThat(result.getCity()).isEqualTo("深圳市");
        assertThat(result.getRegCapital()).isEqualByComparingTo(new BigDecimal("1940591.8198"));
        assertThat(result.getSetupDate()).isEqualTo(LocalDate.of(1987, 12, 22));
        assertThat(result.getEmployees()).isEqualTo(44277);
    }

    @Test
    @Transactional
    void shouldFindCompaniesByPage() {
        // 给定：保存 3 条记录
        companyRepository.save(buildCompany("9144030019218538A1", "公司A", "科技"));
        companyRepository.save(buildCompany("9144030019218538A2", "公司B", "金融"));
        companyRepository.save(buildCompany("9144030019218538A3", "公司C", "消费"));

        // 当：查询第 1 页，每页 2 条
        PageQuery query = new PageQuery();
        query.setPage(1);
        query.setSize(2);
        PageResult<Company> page = companyRepository.findByPage(query);

        // 则
        assertThat(page.getTotal()).isEqualTo(3L);
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    @Transactional
    void shouldFindCompaniesByIndustry() {
        // 给定
        Company bank = buildCompany("9144030019218538B1", "平安银行股份有限公司", "银行");
        Company tech = buildCompany("9144030019218538B2", "腾讯科技有限公司", "科技");
        companyRepository.save(bank);
        companyRepository.save(tech);

        // 当
        List<Company> results = companyRepository.findByIndustry("银行");

        // 则
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("平安银行股份有限公司");
        assertThat(results.get(0).getIndustry()).isEqualTo("银行");
    }

    private Company buildCompany(String uscCode, String name, String industry) {
        return Company.builder()
                .id(CompanyId.generate())
                .unifiedSocialCreditCode(uscCode)
                .name(name)
                .shortName(name.length() > 4 ? name.substring(0, 4) : name)
                .industry(industry)
                .province("广东省")
                .city("深圳市")
                .regCapital(new BigDecimal("1940591.8198"))
                .setupDate(LocalDate.of(1987, 12, 22))
                .employees(44277)
                .build();
    }
}
