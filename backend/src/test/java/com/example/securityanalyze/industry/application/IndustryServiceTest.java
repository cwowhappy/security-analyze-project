package com.example.securityanalyze.industry.application;

import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.industry.api.IndustryCategoryDto;
import com.example.securityanalyze.industry.api.IndustryListResponse;
import com.example.securityanalyze.industry.api.IndustryTrendResponse;
import com.example.securityanalyze.industry.domain.IndustryTrendPoint;
import com.example.securityanalyze.industry.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryServiceTest {

    @Mock
    private IndustryCategoryRepository industryCategoryRepository;

    @Mock
    private CompanyIndustryMappingRepository companyIndustryMappingRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private IndustryTrendGateway industryTrendGateway;

    @InjectMocks
    private IndustryService industryService;

    @Test
    void shouldListIndustries() {
        IndustryCategory category = new IndustryCategory();
        category.setCode("BK0428");
        category.setName("白酒");
        category.setLevel(2);
        category.setCompanyCount(5);

        when(industryCategoryRepository.findByStandardAndLevelWithCount("EM", 2))
                .thenReturn(List.of(category));

        IndustryListResponse response = industryService.listIndustries("EM", 2, null);

        assertEquals(1, response.getData().size());
        assertEquals("白酒", response.getData().get(0).getName());
        assertEquals(5, response.getData().get(0).getCompanyCount());
        assertEquals(1, response.getTotal());
    }

    @Test
    void shouldGetRealIndustryTrend() {
        IndustryCategory category = new IndustryCategory();
        category.setCode("BK0428");
        category.setName("白酒");
        when(industryCategoryRepository.findByCode("EM", "BK0428")).thenReturn(Optional.of(category));

        IndustryTrendPoint point = new IndustryTrendPoint();
        point.setDate("2024-01-01");
        point.setClose(new java.math.BigDecimal("1000.00"));

        when(industryTrendGateway.fetchTrend("白酒", "3m")).thenReturn(List.of(point));

        IndustryTrendResponse response = industryService.getIndustryTrend("EM", "BK0428", "3m");

        assertEquals("白酒", response.getIndustryName());
        assertFalse(response.isFallback());
        assertEquals(1, response.getData().size());
    }

    @Test
    void shouldReturnFallbackTrendWhenRealDataEmpty() {
        IndustryCategory category = new IndustryCategory();
        category.setCode("BK0428");
        category.setName("白酒");
        when(industryCategoryRepository.findByCode("EM", "BK0428")).thenReturn(Optional.of(category));

        when(industryTrendGateway.fetchTrend("白酒", "3m")).thenReturn(List.of());

        IndustryTrendResponse response = industryService.getIndustryTrend("EM", "BK0428", "3m");

        assertTrue(response.isFallback());
        assertFalse(response.getData().isEmpty());
    }

    @Test
    void shouldGenerateFallbackTrendForDifferentPeriods() {
        IndustryCategory category = new IndustryCategory();
        category.setCode("BK0475");
        category.setName("银行");
        when(industryCategoryRepository.findByCode("EM", "BK0475")).thenReturn(Optional.of(category));

        when(industryTrendGateway.fetchTrend("银行", "1m")).thenReturn(List.of());

        IndustryTrendResponse response = industryService.getIndustryTrend("EM", "BK0475", "1m");

        assertTrue(response.isFallback());
        assertEquals(22, response.getData().size());
    }
}
