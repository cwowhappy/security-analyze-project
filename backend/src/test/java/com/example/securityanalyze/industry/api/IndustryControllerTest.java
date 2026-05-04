package com.example.securityanalyze.industry.api;

import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.industry.application.IndustryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class IndustryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndustryService industryService;

    @Test
    void shouldListIndustries() throws Exception {
        IndustryListItem item = new IndustryListItem();
        item.setIndustryName("白酒");
        item.setCompanyCount(5);

        IndustryListResponse response = new IndustryListResponse();
        response.setData(List.of(item));
        response.setTotal(1);

        when(industryService.listIndustries()).thenReturn(response);

        mockMvc.perform(get("/api/industries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].industryName").value("白酒"));
    }

    @Test
    void shouldListCompaniesByIndustry() throws Exception {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode("600519");
        item.setStockName("贵州茅台");

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(List.of(item));
        response.setTotal(1L);
        response.setPage(0);
        response.setSize(20);

        when(industryService.listCompaniesByIndustry("白酒", 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/industries/白酒/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stockCode").value("600519"));
    }

    @Test
    void shouldGetIndustryTrend() throws Exception {
        TrendDataPoint point = new TrendDataPoint();
        point.setDate("2024-01-01");
        point.setClose(new java.math.BigDecimal("1000.00"));

        IndustryTrendResponse response = new IndustryTrendResponse();
        response.setIndustryName("白酒");
        response.setPeriod("3m");
        response.setData(List.of(point));
        response.setFallback(false);

        when(industryService.getIndustryTrend("白酒", "3m")).thenReturn(response);

        mockMvc.perform(get("/api/industries/白酒/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industryName").value("白酒"));
    }
}
