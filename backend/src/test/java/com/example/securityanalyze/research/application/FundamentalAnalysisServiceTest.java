package com.example.securityanalyze.research.application;

import com.example.securityanalyze.research.api.FundamentalOverviewResponse;
import com.example.securityanalyze.research.api.FundamentalScreenResponse;
import com.example.securityanalyze.research.api.IndustryPeersResponse;
import com.example.securityanalyze.research.domain.AnnualMetric;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
import com.example.securityanalyze.research.domain.StockFundamentalMetricsRepository;
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
class FundamentalAnalysisServiceTest {

    @Mock
    private FundamentalMetricsRepository repository;

    @Mock
    private StockFundamentalMetricsRepository stockFundamentalMetricsRepository;

    @InjectMocks
    private FundamentalAnalysisService service;

    @Test
    void shouldGetOverview() {
        FundamentalMetrics metrics = createMetrics("600519", "贵州茅台");
        when(repository.findByStockCode("600519", 5)).thenReturn(Optional.of(metrics));

        Optional<FundamentalOverviewResponse> result = service.getOverview("600519");

        assertTrue(result.isPresent());
        assertEquals("600519", result.get().getStockCode());
        assertEquals("贵州茅台", result.get().getStockName());
        assertEquals(1, result.get().getMetrics().size());
    }

    @Test
    void shouldReturnEmptyWhenOverviewNotFound() {
        when(repository.findByStockCode("999999", 5)).thenReturn(Optional.empty());

        Optional<FundamentalOverviewResponse> result = service.getOverview("999999");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCalculateGrossMarginCorrectly() {
        FundamentalMetrics metrics = createMetrics("600001", "测试");
        // operate_income=4800, operate_cost=2400 -> grossMargin=50%
        metrics.getAnnualMetrics().get(0).setOperateIncome(new BigDecimal("48000000"));
        metrics.getAnnualMetrics().get(0).setOperateCost(new BigDecimal("24000000"));

        when(repository.findByStockCode("600001", 5)).thenReturn(Optional.of(metrics));

        Optional<FundamentalOverviewResponse> result = service.getOverview("600001");

        assertTrue(result.isPresent());
        BigDecimal grossMargin = result.get().getMetrics().get(0).getGrossMargin();
        assertNotNull(grossMargin);
        assertEquals(0, new BigDecimal("50").compareTo(grossMargin.setScale(0, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    void shouldCalculateNetMarginCorrectly() {
        FundamentalMetrics metrics = createMetrics("600002", "测试");
        metrics.getAnnualMetrics().get(0).setParentNetProfit(new BigDecimal("5000000"));
        metrics.getAnnualMetrics().get(0).setOperateIncome(new BigDecimal("50000000"));

        when(repository.findByStockCode("600002", 5)).thenReturn(Optional.of(metrics));

        Optional<FundamentalOverviewResponse> result = service.getOverview("600002");

        BigDecimal netMargin = result.get().getMetrics().get(0).getNetMargin();
        assertNotNull(netMargin);
        assertEquals(0, new BigDecimal("10").compareTo(netMargin.setScale(0, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    void shouldCalculateRoeCorrectly() {
        FundamentalMetrics metrics = createMetrics("600003", "测试");
        metrics.getAnnualMetrics().get(0).setParentNetProfit(new BigDecimal("6000000"));
        metrics.getAnnualMetrics().get(0).setTotalEquity(new BigDecimal("60000000"));

        when(repository.findByStockCode("600003", 5)).thenReturn(Optional.of(metrics));

        Optional<FundamentalOverviewResponse> result = service.getOverview("600003");

        BigDecimal roe = result.get().getMetrics().get(0).getRoe();
        assertNotNull(roe);
        assertEquals(0, new BigDecimal("10").compareTo(roe.setScale(0, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        FundamentalMetrics metrics = createMetrics("600004", "测试");
        AnnualMetric m = metrics.getAnnualMetrics().get(0);
        m.setOperateIncome(null);
        m.setOperateCost(new BigDecimal("1000"));

        when(repository.findByStockCode("600004", 5)).thenReturn(Optional.of(metrics));

        Optional<FundamentalOverviewResponse> result = service.getOverview("600004");

        assertNull(result.get().getMetrics().get(0).getGrossMargin());
    }

    @Test
    void shouldHandleDivisionByZero() {
        FundamentalMetrics metrics = createMetrics("600005", "测试");
        AnnualMetric m = metrics.getAnnualMetrics().get(0);
        m.setOperateIncome(new BigDecimal("0"));
        m.setParentNetProfit(new BigDecimal("1000"));

        when(repository.findByStockCode("600005", 5)).thenReturn(Optional.of(metrics));

        Optional<FundamentalOverviewResponse> result = service.getOverview("600005");

        assertNull(result.get().getMetrics().get(0).getNetMargin());
    }

    @Test
    void shouldScreenCompanies() {
        ScreenCompanyItem item = new ScreenCompanyItem();
        item.setStockCode("600006");
        item.setStockName("测试");
        item.setIndustry("信息技术");
        item.setMarket("SH");

        when(repository.screenCompanies("测试", null, null, 0, 20))
                .thenReturn(List.of(item));
        when(repository.countScreenCompanies("测试", null, null))
                .thenReturn(1L);

        FundamentalScreenResponse response = service.screenCompanies("测试", null, null, 0, 20);

        assertEquals(1, response.getItems().size());
        assertEquals("600006", response.getItems().get(0).getStockCode());
        assertEquals(1L, response.getTotal());
    }

    @Test
    void shouldNormalizePagination() {
        when(repository.screenCompanies(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.countScreenCompanies(any(), any(), any()))
                .thenReturn(0L);

        FundamentalScreenResponse response = service.screenCompanies(null, null, null, -1, 0);

        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void shouldGetIndustryPeers() {
        PeerMetric peer = new PeerMetric();
        peer.setStockCode("600007");
        peer.setStockName("同行");
        peer.setIndustry("信息技术");

        when(repository.findIndustryPeers("600008")).thenReturn(List.of(peer));

        IndustryPeersResponse response = service.getIndustryPeers("600008");

        assertEquals(1, response.getPeers().size());
        assertEquals("600007", response.getPeers().get(0).getStockCode());
    }

    private FundamentalMetrics createMetrics(String stockCode, String stockName) {
        FundamentalMetrics metrics = new FundamentalMetrics();
        metrics.setStockCode(stockCode);
        metrics.setStockName(stockName);
        metrics.setIndustry("信息技术");
        metrics.setMarket("SH");

        AnnualMetric m = new AnnualMetric();
        m.setReportDate(LocalDate.of(2023, 12, 31));
        m.setReportYear(2023);
        m.setTotalRevenue(new BigDecimal("100000000"));
        m.setOperateIncome(new BigDecimal("80000000"));
        m.setOperateCost(new BigDecimal("40000000"));
        m.setParentNetProfit(new BigDecimal("10000000"));
        m.setTotalAssets(new BigDecimal("500000000"));
        m.setTotalLiabilities(new BigDecimal("200000000"));
        m.setTotalEquity(new BigDecimal("300000000"));
        m.setSaleExpense(new BigDecimal("5000000"));
        m.setManageExpense(new BigDecimal("8000000"));
        m.setResearchExpense(new BigDecimal("3000000"));
        m.setFinanceExpense(new BigDecimal("2000000"));
        m.setOperatingCashFlow(new BigDecimal("12000000"));
        m.setInvestingCashFlow(new BigDecimal("-3000000"));
        m.setFinancingCashFlow(new BigDecimal("-4000000"));
        m.setEndCce(new BigDecimal("25000000"));

        metrics.setAnnualMetrics(List.of(m));
        return metrics;
    }
}
