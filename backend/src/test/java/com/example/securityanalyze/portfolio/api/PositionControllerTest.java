package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.application.PortfolioService;
import com.example.securityanalyze.portfolio.application.TransactionService;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
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
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PositionController 单元测试。
 *
 * <p>覆盖持仓列表查询（listPositions）与组合汇总（getSummary）两个核心接口。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser")
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldListPositionsWithMarketValueAndWeight() throws Exception {
        // Given
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setName("测试组合");
        when(portfolioService.getPortfolio("testuser", 1L)).thenReturn(portfolio);

        Map<String, Object> row1 = Map.of(
                "stock_code", "600519",
                "current_quantity", new BigDecimal("100"),
                "total_cost", new BigDecimal("168800"),
                "avg_cost", new BigDecimal("1688"),
                "realized_pnl", new BigDecimal("0"),
                "stock_name", "贵州茅台",
                "market", "SH",
                "close_price", new BigDecimal("1710")
        );
        Map<String, Object> row2 = Map.of(
                "stock_code", "000001",
                "current_quantity", new BigDecimal("500"),
                "total_cost", new BigDecimal("5000"),
                "avg_cost", new BigDecimal("10"),
                "realized_pnl", new BigDecimal("100"),
                "stock_name", "平安银行",
                "market", "SZ",
                "close_price", new BigDecimal("12")
        );
        when(portfolioService.listPositionsWithQuote(1L)).thenReturn(List.of(row1, row2));

        // When & Then
        mockMvc.perform(get("/api/portfolios/1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stockCode").value("600519"))
                .andExpect(jsonPath("$[0].stockName").value("贵州茅台"))
                .andExpect(jsonPath("$[0].closePrice").value(1710))
                .andExpect(jsonPath("$[0].marketValue").value(171000))   // 1710 * 100
                .andExpect(jsonPath("$[0].floatingPnl").value(2200))     // 171000 - 1688*100
                .andExpect(jsonPath("$[1].stockCode").value("000001"))
                .andExpect(jsonPath("$[1].marketValue").value(6000));    // 12 * 500
    }

    @Test
    void shouldReturnEmptyPositions() throws Exception {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        when(portfolioService.getPortfolio("testuser", 1L)).thenReturn(portfolio);
        when(portfolioService.listPositionsWithQuote(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/portfolios/1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldDenyAccessWhenPortfolioNotBelongToUser() throws Exception {
        when(portfolioService.getPortfolio("testuser", 1L))
                .thenThrow(new PortfolioAccessDeniedException("无权限访问该组合"));

        mockMvc.perform(get("/api/portfolios/1/positions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetSummaryWithPortfolioName() throws Exception {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setName("测试组合");
        when(portfolioService.getPortfolio("testuser", 1L)).thenReturn(portfolio);

        Map<String, Object> row = Map.of(
                "stock_code", "600519",
                "current_quantity", new BigDecimal("100"),
                "total_cost", new BigDecimal("168800"),
                "avg_cost", new BigDecimal("1688"),
                "realized_pnl", new BigDecimal("500"),
                "close_price", new BigDecimal("1710")
        );
        when(portfolioService.listPositionsWithQuote(1L)).thenReturn(List.of(row));

        TransactionRecord tx = new TransactionRecord();
        tx.setTradeDate(LocalDate.of(2026, 5, 5));
        when(transactionService.listTransactions(1L, null, null, null, null, 0, Integer.MAX_VALUE))
                .thenReturn(List.of(tx));

        mockMvc.perform(get("/api/portfolios/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioName").value("测试组合"))
                .andExpect(jsonPath("$.totalCost").value(168800))
                .andExpect(jsonPath("$.totalMarketValue").value(171000))     // 1710 * 100
                .andExpect(jsonPath("$.totalFloatingPnl").value(2200))        // 171000 - 168800
                .andExpect(jsonPath("$.totalRealizedPnl").value(500))
                .andExpect(jsonPath("$.totalAssetReturn").value(2700))       // 2200 + 500
                .andExpect(jsonPath("$.holdingCount").value(1))
                .andExpect(jsonPath("$.latestTradeDate").value("2026-05-05"));
    }

    @Test
    void shouldGetSummaryWithZeroValuesWhenNoPositions() throws Exception {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setName("空组合");
        when(portfolioService.getPortfolio("testuser", 1L)).thenReturn(portfolio);
        when(portfolioService.listPositionsWithQuote(1L)).thenReturn(List.of());
        when(transactionService.listTransactions(1L, null, null, null, null, 0, Integer.MAX_VALUE))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/portfolios/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioName").value("空组合"))
                .andExpect(jsonPath("$.totalCost").value(0))
                .andExpect(jsonPath("$.totalMarketValue").value(0))
                .andExpect(jsonPath("$.totalFloatingPnl").value(0))
                .andExpect(jsonPath("$.totalRealizedPnl").value(0))
                .andExpect(jsonPath("$.holdingCount").value(0))
                .andExpect(jsonPath("$.latestTradeDate").doesNotExist());
    }
}
