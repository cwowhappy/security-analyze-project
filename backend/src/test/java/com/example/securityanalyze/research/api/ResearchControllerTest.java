package com.example.securityanalyze.research.api;

import com.example.securityanalyze.research.application.FundamentalAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ResearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FundamentalAnalysisService service;

    @Test
    void shouldReturn200WhenOverviewExists() throws Exception {
        FundamentalOverviewResponse response = new FundamentalOverviewResponse();
        response.setStockCode("600519");
        response.setStockName("贵州茅台");
        response.setIndustry("白酒");
        response.setMarket("SH");

        AnnualMetricDto metric = new AnnualMetricDto();
        metric.setReportDate("2023-12-31");
        metric.setReportYear(2023);
        metric.setTotalRevenue(new BigDecimal("150545774400"));
        metric.setGrossMargin(new BigDecimal("87.92"));
        response.setMetrics(List.of(metric));

        when(service.getOverview("600519")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/research/fundamental/overview/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCode").value("600519"))
                .andExpect(jsonPath("$.stockName").value("贵州茅台"))
                .andExpect(jsonPath("$.metrics[0].reportYear").value(2023))
                .andExpect(jsonPath("$.metrics[0].grossMargin").value(87.92));
    }

    @Test
    void shouldReturn404WhenOverviewNotFound() throws Exception {
        when(service.getOverview("999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/research/fundamental/overview/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldScreenCompanies() throws Exception {
        ScreenCompanyItemResponse item = new ScreenCompanyItemResponse();
        item.setStockCode("600519");
        item.setStockName("贵州茅台");
        item.setIndustry("白酒");
        item.setMarket("SH");
        item.setLatestRevenue(new BigDecimal("150545774400"));

        FundamentalScreenResponse response = new FundamentalScreenResponse();
        response.setItems(List.of(item));
        response.setTotal(1L);
        response.setPage(0);
        response.setSize(20);

        when(service.screenCompanies("茅台", null, null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/research/fundamental/screen")
                        .param("keyword", "茅台")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stockCode").value("600519"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldGetIndustryPeers() throws Exception {
        PeerMetricDto peer = new PeerMetricDto();
        peer.setStockCode("000001");
        peer.setStockName("平安银行");
        peer.setIndustry("银行");
        peer.setRoe(new BigDecimal("10.5"));

        IndustryPeersResponse response = new IndustryPeersResponse();
        response.setPeers(List.of(peer));

        when(service.getIndustryPeers("600000")).thenReturn(response);

        mockMvc.perform(get("/api/research/fundamental/industry-peers/600000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peers[0].stockCode").value("000001"))
                .andExpect(jsonPath("$.peers[0].roe").value(10.5));
    }

    @Test
    void shouldNormalizePaginationParams() throws Exception {
        FundamentalScreenResponse response = new FundamentalScreenResponse();
        response.setItems(List.of());
        response.setTotal(0L);
        response.setPage(0);
        response.setSize(20);

        when(service.screenCompanies(null, null, null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/research/fundamental/screen?page=-1&size=0"))
                .andExpect(status().isOk());
    }
}
