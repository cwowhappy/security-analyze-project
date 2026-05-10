package com.example.securityanalyze.company.api;

import com.example.securityanalyze.company.application.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @Test
    void shouldListCompanies() throws Exception {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode("600519");
        item.setStockName("贵州茅台");
        item.setIndustry("白酒");
        item.setRegion("贵州");
        item.setListingDate(LocalDate.of(2020, 1, 1));
        item.setMarket("SH");

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(List.of(item));
        response.setTotal(1L);
        response.setPage(0);
        response.setSize(20);

        when(companyService.listCompanies(null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stockCode").value("600519"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldGetCompanyDetail() throws Exception {
        CompanyDetailResponse detail = new CompanyDetailResponse();
        detail.setStockCode("600519");
        detail.setStockName("贵州茅台");
        detail.setIndustry("白酒");
        detail.setRegion("贵州");
        detail.setRegisteredCapital(BigDecimal.valueOf(10000));

        when(companyService.getCompanyDetail("600519")).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/companies/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("600519"))
                .andExpect(jsonPath("$.industry").value("白酒"));
    }

    @Test
    void shouldReturn404WhenCompanyNotFound() throws Exception {
        when(companyService.getCompanyDetail("999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/companies/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNormalizePaginationParams() throws Exception {
        CompanyListResponse response = new CompanyListResponse();
        response.setItems(List.of());
        response.setTotal(0L);
        response.setPage(0);
        response.setSize(20);

        when(companyService.listCompanies(null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/companies?page=-1&size=0"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldBatchQueryCompanies() throws Exception {
        CompanyListItem item1 = new CompanyListItem();
        item1.setStockCode("600519");
        item1.setStockName("贵州茅台");
        item1.setIndustry("白酒");

        when(companyService.batchQuery(List.of("600519", "000001")))
                .thenReturn(List.of(item1));

        mockMvc.perform(post("/api/companies/batch")
                        .contentType("application/json")
                        .content("[\"600519\",\"000001\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockCode").value("600519"));
    }
}
