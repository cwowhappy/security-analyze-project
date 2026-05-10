package org.cwowhappy.securityanalyze.company.application.service;

import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.company.application.service.impl.CompanyAppServiceImpl;
import org.cwowhappy.securityanalyze.company.domain.model.Company;
import org.cwowhappy.securityanalyze.company.domain.model.CompanyId;
import org.cwowhappy.securityanalyze.company.domain.repository.CompanyRepository;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 公司应用服务单元测试（纯 Mockito，不启动 Spring 上下文）。
 */
@ExtendWith(MockitoExtension.class)
class CompanyAppServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyAppServiceImpl companyAppService;

    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        sampleCompany = Company.builder()
                .id(CompanyId.of("comp001"))
                .unifiedSocialCreditCode("9144030019218538XX")
                .name("平安银行股份有限公司")
                .shortName("平安银行")
                .industry("银行")
                .province("广东省")
                .city("深圳市")
                .regCapital(new BigDecimal("1940591.8198"))
                .setupDate(LocalDate.of(1987, 12, 22))
                .employees(44277)
                .build();
    }

    @Test
    void shouldReturnCompanyWhenFoundByUscCode() {
        // Arrange
        when(companyRepository.findByUscCode("9144030019218538XX")).thenReturn(Optional.of(sampleCompany));

        // Act
        Optional<CompanyDTO> result = companyAppService.findByUscCode("9144030019218538XX");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUnifiedSocialCreditCode()).isEqualTo("9144030019218538XX");
        assertThat(result.get().getName()).isEqualTo("平安银行股份有限公司");
        assertThat(result.get().getShortName()).isEqualTo("平安银行");
        verify(companyRepository, times(1)).findByUscCode("9144030019218538XX");
    }

    @Test
    void shouldReturnEmptyWhenCompanyNotFoundByUscCode() {
        // Arrange
        when(companyRepository.findByUscCode("999999999999999999")).thenReturn(Optional.empty());

        // Act
        Optional<CompanyDTO> result = companyAppService.findByUscCode("999999999999999999");

        // Assert
        assertThat(result).isEmpty();
        verify(companyRepository, times(1)).findByUscCode("999999999999999999");
    }

    @Test
    void shouldReturnPageResultWhenFindByPage() {
        // Arrange
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(1);
        pageQuery.setSize(10);

        PageResult<Company> repoResult = PageResult.<Company>builder()
                .list(List.of(sampleCompany))
                .total(1)
                .page(1)
                .size(10)
                .build();
        when(companyRepository.findByPage(pageQuery, "银行", "广东省", "平安"))
                .thenReturn(repoResult);

        // Act
        PageResult<CompanyDTO> result = companyAppService.findByPage(pageQuery, "银行", "广东省", "平安");

        // Assert
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getList().get(0).getName()).isEqualTo("平安银行股份有限公司");
        verify(companyRepository, times(1)).findByPage(pageQuery, "银行", "广东省", "平安");
    }

    @Test
    void shouldCreateCompanyAndReturnId() {
        // Arrange
        CompanyDTO dto = CompanyDTO.builder()
                .unifiedSocialCreditCode("9144030019218538XX")
                .name("平安银行股份有限公司")
                .shortName("平安银行")
                .industry("银行")
                .province("广东省")
                .city("深圳市")
                .build();
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company company = inv.getArgument(0);
            return company.getId();
        });

        // Act
        String id = companyAppService.createCompany(dto);

        // Assert
        assertThat(id).isNotNull();
        verify(companyRepository, times(1)).save(any(Company.class));
    }
}
