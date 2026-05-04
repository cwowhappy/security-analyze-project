package com.example.securityanalyze.industry.application;

import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.industry.api.IndustryListItem;
import com.example.securityanalyze.industry.api.IndustryListResponse;
import com.example.securityanalyze.industry.api.IndustryTrendResponse;
import com.example.securityanalyze.industry.api.TrendDataPoint;
import com.example.securityanalyze.industry.domain.IndustryTrendGateway;
import com.example.securityanalyze.industry.infrastructure.IndustryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryServiceTest {

    @Mock
    private IndustryRepository industryRepository;

    @Mock
    private IndustryTrendGateway industryTrendGateway;

    @InjectMocks
    private IndustryService industryService;

    @Test
    void shouldListIndustries() {
        IndustryListItem item = new IndustryListItem();
        item.setIndustryName("白酒");
        item.setCompanyCount(5);

        when(industryRepository.findIndustries()).thenReturn(List.of(item));

        IndustryListResponse response = industryService.listIndustries();

        assertEquals(1, response.getData().size());
        assertEquals("白酒", response.getData().get(0).getIndustryName());
        assertEquals(5, response.getData().get(0).getCompanyCount());
        assertEquals(1, response.getTotal());
    }

    @Test
    void shouldListCompaniesByIndustry() {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode("600519");
        item.setStockName("贵州茅台");

        when(industryRepository.findCompaniesByIndustry("白酒", 0, 20))
                .thenReturn(List.of(item));
        when(industryRepository.countCompaniesByIndustry("白酒")).thenReturn(1L);

        CompanyListResponse response = industryService.listCompaniesByIndustry("白酒", 0, 20);

        assertEquals(1, response.getItems().size());
        assertEquals(1L, response.getTotal());
    }

    @Test
    void shouldGetRealIndustryTrend() {
        TrendDataPoint point = new TrendDataPoint();
        point.setDate("2024-01-01");
        point.setClose(new java.math.BigDecimal("1000.00"));

        when(industryTrendGateway.fetchTrend("白酒", "3m")).thenReturn(List.of(point));

        IndustryTrendResponse response = industryService.getIndustryTrend("白酒", "3m");

        assertEquals("白酒", response.getIndustryName());
        assertFalse(response.isFallback());
        assertEquals(1, response.getData().size());
    }

    @Test
    void shouldReturnFallbackTrendWhenRealDataEmpty() {
        when(industryTrendGateway.fetchTrend("白酒", "3m")).thenReturn(List.of());

        IndustryTrendResponse response = industryService.getIndustryTrend("白酒", "3m");

        assertTrue(response.isFallback());
        assertFalse(response.getData().isEmpty());
    }

    @Test
    void shouldGenerateFallbackTrendForDifferentPeriods() {
        when(industryTrendGateway.fetchTrend("银行", "1m")).thenReturn(List.of());

        IndustryTrendResponse response = industryService.getIndustryTrend("银行", "1m");

        assertTrue(response.isFallback());
        assertEquals(22, response.getData().size());
    }
}
