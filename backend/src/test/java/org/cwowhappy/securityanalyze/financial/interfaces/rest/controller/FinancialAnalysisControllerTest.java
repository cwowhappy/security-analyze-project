package org.cwowhappy.securityanalyze.financial.interfaces.rest.controller;

import org.cwowhappy.securityanalyze.financial.application.dto.*;
import org.cwowhappy.securityanalyze.financial.application.service.FinancialAnalysisAppService;
import org.cwowhappy.securityanalyze.financial.application.service.FinancialIndicatorAppService;
import org.cwowhappy.securityanalyze.financial.application.service.FinancialReportAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FinancialAnalysisController Web 层测试（@WebMvcTest，只加载 Controller 层）。
 */
@WebMvcTest(FinancialAnalysisController.class)
class FinancialAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialReportAppService reportAppService;

    @MockitoBean
    private FinancialIndicatorAppService indicatorAppService;

    @MockitoBean
    private FinancialAnalysisAppService analysisAppService;

    @Test
    void shouldReturnIncomeStatements() throws Exception {
        FinancialIncomeDTO dto = FinancialIncomeDTO.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .revenue(new BigDecimal("100000000.00"))
                .netProfit(new BigDecimal("15000000.00"))
                .build();
        when(reportAppService.getIncomeStatements("000001", null, 20))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/stocks/000001/financial/income")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].stockCode").value("000001"));
    }

    @Test
    void shouldReturnBalanceSheets() throws Exception {
        FinancialBalanceDTO dto = FinancialBalanceDTO.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .totalAssets(new BigDecimal("500000000.00"))
                .build();
        when(reportAppService.getBalanceSheets("000001", null, 20))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/stocks/000001/financial/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].totalAssets").value(500000000.00));
    }

    @Test
    void shouldReturnCashflowStatements() throws Exception {
        FinancialCashflowDTO dto = FinancialCashflowDTO.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .cfOperating(new BigDecimal("50000000.00"))
                .build();
        when(reportAppService.getCashflowStatements("000001", null, 20))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/stocks/000001/financial/cashflow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].cfOperating").value(50000000.00));
    }

    @Test
    void shouldReturnIndicators() throws Exception {
        FinancialIndicatorDTO dto = FinancialIndicatorDTO.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .roe(new BigDecimal("12.50"))
                .build();
        when(indicatorAppService.getIndicators("000001", null, 20))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/stocks/000001/financial/indicator")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roe").value(12.50));
    }

    @Test
    void shouldReturnTrendData() throws Exception {
        TrendDataDTO dto = TrendDataDTO.builder()
                .stockCode("000001")
                .metric("roe")
                .data(List.of(
                        TrendDataDTO.TrendPoint.builder()
                                .reportDate(LocalDate.of(2024, 12, 31))
                                .value(new BigDecimal("12.50"))
                                .build()
                ))
                .build();
        when(analysisAppService.getTrend(eq("000001"), anyList(), eq(null), eq(8)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/stocks/000001/financial/trend")
                        .param("metrics", "roe,grossMargin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].metric").value("roe"));
    }

    @Test
    void shouldReturnDupontAnalysis() throws Exception {
        DupontAnalysisDTO dto = DupontAnalysisDTO.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .roe(new BigDecimal("12.50"))
                .netMargin(new BigDecimal("15.00"))
                .assetTurnover(new BigDecimal("0.80"))
                .equityMultiplier(new BigDecimal("1.50"))
                .build();
        when(analysisAppService.getDupontAnalysis("000001", LocalDate.of(2024, 12, 31), null))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/stocks/000001/financial/dupont")
                        .param("reportDate", "2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roe").value(12.50));
    }

    @Test
    void shouldReturnPeerComparison() throws Exception {
        PeerComparisonDTO dto = PeerComparisonDTO.builder()
                .stockCode("000001")
                .metric("roe")
                .metricName("ROE")
                .stockValue(new BigDecimal("12.50"))
                .industryAvg(new BigDecimal("10.00"))
                .industryMedian(new BigDecimal("9.50"))
                .industryMax(new BigDecimal("25.00"))
                .industryMin(new BigDecimal("2.00"))
                .peers(List.of(
                        PeerComparisonDTO.PeerItem.builder()
                                .stockCode("000001")
                                .stockName("平安银行")
                                .value(new BigDecimal("12.50"))
                                .build()
                ))
                .build();
        when(analysisAppService.getPeerComparison("000001", "roe", null))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/stocks/000001/financial/peer-comparison")
                        .param("metric", "roe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.metric").value("roe"))
                .andExpect(jsonPath("$.data.industryAvg").value(10.00));
    }
}
