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

    @Test
    void shouldBatchQueryCompanies() {
        CompanySecurity sec1 = createSecurity(1L, "600519", "贵州茅台", "SH");
        CompanySecurity sec2 = createSecurity(2L, "000001", "平安银行", "SZ");
        Company comp1 = createCompany(1L, "白酒", "贵州");
        Company comp2 = createCompany(2L, "银行", "广东");

        when(companySecurityRepository.findByStockCode("600519")).thenReturn(Optional.of(sec1));
        when(companySecurityRepository.findByStockCode("000001")).thenReturn(Optional.of(sec2));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(comp1));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(comp2));

        var results = companyService.batchQuery(List.of("600519", "000001"));

        assertEquals(2, results.size());
        assertEquals("600519", results.get(0).getStockCode());
        assertEquals("000001", results.get(1).getStockCode());
    }

    @Test
    void shouldReturnEmptyForBatchQueryWithInvalidCodes() {
        when(companySecurityRepository.findByStockCode("999999")).thenReturn(Optional.empty());

        var results = companyService.batchQuery(List.of("999999"));

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldLimitBatchQueryTo50() {
        var codes = new java.util.ArrayList<String>();
        for (int i = 0; i < 60; i++) {
            codes.add(String.format("%06d", i));
        }

        // 只 mock 前50个查询
        for (int i = 0; i < 50; i++) {
            CompanySecurity sec = createSecurity((long) i, String.format("%06d", i), "公司" + i, "SH");
            when(companySecurityRepository.findByStockCode(String.format("%06d", i)))
                    .thenReturn(Optional.of(sec));
            when(companyRepository.findById((long) i))
                    .thenReturn(Optional.of(createCompany((long) i, "行业" + i, "地区" + i)));
        }

        var results = companyService.batchQuery(codes);

        assertEquals(50, results.size());
    }

    @Test
    void shouldListCompaniesWhenCompanyInfoMissing() {
        CompanySecurity sec = createSecurity(999L, "600999", "孤儿证券", "SH");
        when(companySecurityRepository.findByKeyword(null, 0, 20))
                .thenReturn(List.of(sec));
        when(companySecurityRepository.countByKeyword(null)).thenReturn(1L);
        when(companyRepository.findAllById(List.of(999L))).thenReturn(List.of());

        CompanyListResponse response = companyService.listCompanies(null, 0, 20);

        assertEquals(1, response.getItems().size());
        assertNull(response.getItems().get(0).getIndustry());
        assertNull(response.getItems().get(0).getRegion());
    }

    @Test
    void shouldGetCompanyDetailWithIndustries() {
        CompanySecurity primary = createSecurity(1L, "600519", "贵州茅台", "SH");
        Company company = createCompany(1L, "白酒", "贵州");

        when(companySecurityRepository.findByStockCode("600519")).thenReturn(Optional.of(primary));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companySecurityRepository.findByCompanyId(1L)).thenReturn(List.of(primary));

        // 模拟多标准行业映射
        com.example.securityanalyze.industry.domain.CompanyIndustryMapping mapping1 =
                new com.example.securityanalyze.industry.domain.CompanyIndustryMapping();
        mapping1.setStandardCode("SW");
        mapping1.setLevel1Code("C15");
        mapping1.setLevel2Code("C1511");
        mapping1.setPrimary(true);

        com.example.securityanalyze.industry.domain.CompanyIndustryMapping mapping2 =
                new com.example.securityanalyze.industry.domain.CompanyIndustryMapping();
        mapping2.setStandardCode("EM");
        mapping2.setLevel1Code("E01");
        mapping2.setPrimary(false);

        when(companyIndustryMappingRepository.findByCompanyId(1L))
                .thenReturn(List.of(mapping1, mapping2));

        // 模拟行业分类查询
        com.example.securityanalyze.industry.domain.IndustryCategory cat1 =
                new com.example.securityanalyze.industry.domain.IndustryCategory();
        cat1.setCode("C15");
        cat1.setName("食品饮料");

        com.example.securityanalyze.industry.domain.IndustryCategory cat2 =
                new com.example.securityanalyze.industry.domain.IndustryCategory();
        cat2.setCode("C1511");
        cat2.setName("白酒");

        when(industryCategoryRepository.findByCode("SW", "C15")).thenReturn(Optional.of(cat1));
        when(industryCategoryRepository.findByCode("SW", "C1511")).thenReturn(Optional.of(cat2));

        Optional<CompanyDetailResponse> detail = companyService.getCompanyDetail("600519");

        assertTrue(detail.isPresent());
        assertEquals(2, detail.get().getIndustries().size());
        assertTrue(detail.get().getIndustries().get(0).getPrimary());
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
