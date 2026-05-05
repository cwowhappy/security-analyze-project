package com.example.securityanalyze.company.application;

import com.example.securityanalyze.company.api.CompanyDetailResponse;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import com.example.securityanalyze.industry.domain.CompanyIndustryMappingRepository;
import com.example.securityanalyze.industry.domain.IndustryCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySecurityRepository companySecurityRepository;

    @Mock
    private CompanyIndustryMappingRepository companyIndustryMappingRepository;

    @Mock
    private IndustryCategoryRepository industryCategoryRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void shouldListCompanies() {
        CompanySecurity sec1 = createSecurity(1L, "600519", "贵州茅台", "SH");
        CompanySecurity sec2 = createSecurity(2L, "000001", "平安银行", "SZ");

        Company comp1 = createCompany(1L, "白酒", "贵州");
        Company comp2 = createCompany(2L, "银行", "广东");

        when(companySecurityRepository.findByKeyword("茅台", 0, 20))
                .thenReturn(List.of(sec1, sec2));
        when(companySecurityRepository.countByKeyword("茅台")).thenReturn(2L);
        when(companyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(comp1, comp2));

        CompanyListResponse response = companyService.listCompanies("茅台", 0, 20);

        assertEquals(2, response.getItems().size());
        assertEquals(2L, response.getTotal());
        assertEquals("600519", response.getItems().get(0).getStockCode());
        assertEquals("白酒", response.getItems().get(0).getIndustry());
        verify(companySecurityRepository).findByKeyword("茅台", 0, 20);
        verify(companyRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    void shouldListCompaniesWithEmptyResult() {
        when(companySecurityRepository.findByKeyword("notexist", 0, 20)).thenReturn(List.of());
        when(companySecurityRepository.countByKeyword("notexist")).thenReturn(0L);
        when(companyRepository.findAllById(List.of())).thenReturn(List.of());

        CompanyListResponse response = companyService.listCompanies("notexist", 0, 20);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0L, response.getTotal());
    }

    @Test
    void shouldGetCompanyDetail() {
        CompanySecurity primary = createSecurity(1L, "600519", "贵州茅台", "SH");
        Company company = createCompany(1L, "白酒", "贵州");
        CompanySecurity sec2 = createSecurity(1L, "600519.SH", "贵州茅台", "SH");

        when(companySecurityRepository.findByStockCode("600519")).thenReturn(Optional.of(primary));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companySecurityRepository.findByCompanyId(1L)).thenReturn(List.of(primary, sec2));

        Optional<CompanyDetailResponse> detail = companyService.getCompanyDetail("600519");

        assertTrue(detail.isPresent());
        assertEquals("600519", detail.get().getStockCode());
        assertEquals("白酒", detail.get().getIndustry());
        assertEquals(2, detail.get().getSecurities().size());
    }

    @Test
    void shouldReturnEmptyWhenStockCodeNotFound() {
        when(companySecurityRepository.findByStockCode("999999")).thenReturn(Optional.empty());

        Optional<CompanyDetailResponse> detail = companyService.getCompanyDetail("999999");

        assertTrue(detail.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenCompanyNotFound() {
        CompanySecurity primary = createSecurity(1L, "600519", "贵州茅台", "SH");
        when(companySecurityRepository.findByStockCode("600519")).thenReturn(Optional.of(primary));
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<CompanyDetailResponse> detail = companyService.getCompanyDetail("600519");

        assertTrue(detail.isEmpty());
    }

    private CompanySecurity createSecurity(Long companyId, String stockCode, String stockName, String market) {
        CompanySecurity s = new CompanySecurity();
        s.setCompanyId(companyId);
        s.setStockCode(stockCode);
        s.setStockName(stockName);
        s.setMarket(market);
        s.setListingDate(LocalDate.of(2020, 1, 1));
        return s;
    }

    private Company createCompany(Long id, String industry, String region) {
        Company c = new Company();
        c.setId(id);
        c.setIndustry(industry);
        c.setRegion(region);
        c.setRegisteredCapital(BigDecimal.valueOf(10000));
        return c;
    }
}
