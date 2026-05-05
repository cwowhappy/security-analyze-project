package com.example.securityanalyze.finance.api;

import com.example.securityanalyze.finance.application.FinanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinanceService financeService;

    @Test
    void shouldListReports() throws Exception {
        FinanceReportListResponse response = new FinanceReportListResponse();
        response.setStockCode("600519");
        response.setStockName("贵州茅台");

        FinanceReportListItem item = new FinanceReportListItem();
        item.setId(1L);
        item.setReportDate(LocalDate.of(2024, 3, 31));
        item.setReportType("季报");
        item.setTotalRevenue(BigDecimal.valueOf(1000));
        item.setNetProfit(BigDecimal.valueOf(200));
        response.setItems(List.of(item));

        when(financeService.listReports("600519")).thenReturn(response);

        mockMvc.perform(get("/api/finance/600519/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("600519"));
    }

    @Test
    void shouldGetReportDetail() throws Exception {
        FinanceReportResponse detail = new FinanceReportResponse();
        detail.setId(1L);
        detail.setStockCode("600519");

        when(financeService.getReportDetail(1L)).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/finance/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("600519"));
    }

    @Test
    void shouldReturn404WhenReportNotFound() throws Exception {
        when(financeService.getReportDetail(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/finance/reports/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetIndicators() throws Exception {
        FinanceIndicatorResponse response = new FinanceIndicatorResponse();
        response.setStockCode("600519");

        when(financeService.getIndicators("600519", List.of("totalRevenue", "netProfit", "grossMargin", "netMargin", "debtRatio"), null, null, null))
                .thenReturn(response);

        mockMvc.perform(get("/api/finance/600519/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("600519"));
    }
}
