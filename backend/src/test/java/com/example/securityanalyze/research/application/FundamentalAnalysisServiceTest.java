package com.example.securityanalyze.research.application;

import com.example.securityanalyze.research.api.DcfRequest;
import com.example.securityanalyze.research.api.DcfResponse;
import com.example.securityanalyze.research.api.FundamentalOverviewResponse;
import com.example.securityanalyze.research.api.FundamentalScreenResponse;
import com.example.securityanalyze.research.api.IndustryPeersResponse;
import com.example.securityanalyze.research.api.ValuationHistoryResponse;
import com.example.securityanalyze.research.api.ValuationOverviewResponse;
import com.example.securityanalyze.research.domain.AnnualMetric;
import com.example.securityanalyze.research.domain.CompanyBasicInfo;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.IndustryRankItem;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.StockFundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.ValuationMetrics;
import com.example.securityanalyze.research.domain.ValuationMetricsRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundamentalAnalysisServiceTest {

    @Mock
    private FundamentalMetricsRepository repository;

    @Mock
    private StockFundamentalMetricsRepository stockFundamentalMetricsRepository;

    @Mock
    private ValuationMetricsRepository valuationMetricsRepository;

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

    // ========== 阶段C：估值分析测试 ==========

    @Test
    void shouldGetValuationOverview() {
        ValuationMetrics vm = new ValuationMetrics();
        vm.setStockCode("600519");
        vm.setTradeDate(LocalDate.of(2024, 1, 2));
        vm.setClosePrice(new BigDecimal("1680.50"));
        vm.setPeTtm(new BigDecimal("28.5"));
        vm.setPeTtmPercentile(new BigDecimal("0.72"));
        vm.setPb(new BigDecimal("8.3"));
        vm.setPbPercentile(new BigDecimal("0.65"));

        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockCode("600519");
        info.setStockName("贵州茅台");
        info.setTotalShares(new BigDecimal("1000000000"));

        when(valuationMetricsRepository.findLatestByStockCode("600519")).thenReturn(Optional.of(vm));
        when(valuationMetricsRepository.findCompanyBasicInfo("600519")).thenReturn(info);
        when(valuationMetricsRepository.findLatestFundamentalMetrics("600519")).thenReturn(Optional.empty());

        Optional<ValuationOverviewResponse> result = service.getValuationOverview("600519");

        assertTrue(result.isPresent());
        assertEquals("600519", result.get().getStockCode());
        assertEquals("贵州茅台", result.get().getStockName());
        assertEquals(0, new BigDecimal("1680.50").compareTo(result.get().getCurrentPrice()));
        assertNotNull(result.get().getCompositeScore());
        assertNotNull(result.get().getWarnings());
    }

    @Test
    void shouldReturnEmptyWhenValuationNotFound() {
        when(valuationMetricsRepository.findLatestByStockCode("999999")).thenReturn(Optional.empty());

        Optional<ValuationOverviewResponse> result = service.getValuationOverview("999999");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGetValuationHistory() {
        ValuationMetrics vm = new ValuationMetrics();
        vm.setStockCode("600519");
        vm.setTradeDate(LocalDate.of(2024, 1, 2));
        vm.setClosePrice(new BigDecimal("1680.50"));
        vm.setPeTtm(new BigDecimal("28.5"));

        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockCode("600519");
        info.setStockName("贵州茅台");

        when(valuationMetricsRepository.findHistoryByStockCode(eq("600519"), any(), any()))
                .thenReturn(List.of(vm));
        when(valuationMetricsRepository.findCompanyBasicInfo("600519")).thenReturn(info);

        ValuationHistoryResponse result = service.getValuationHistory("600519");

        assertEquals("600519", result.getStockCode());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void shouldCalculateDcfWithDefaultParams() {
        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockCode("600519");
        info.setTotalShares(new BigDecimal("1000000000"));

        ValuationMetrics vm = new ValuationMetrics();
        vm.setClosePrice(new BigDecimal("100"));

        when(valuationMetricsRepository.findCompanyBasicInfo("600519")).thenReturn(info);
        when(valuationMetricsRepository.findLatestOperatingCashFlow("600519"))
                .thenReturn(new BigDecimal("10000000000"));
        when(valuationMetricsRepository.findLatestByStockCode("600519")).thenReturn(Optional.of(vm));

        DcfRequest request = new DcfRequest();
        DcfResponse result = service.calculateDcf("600519", request);

        assertNotNull(result.getFairPrice());
        assertNotNull(result.getFairPriceRangeLow());
        assertNotNull(result.getFairPriceRangeHigh());
        assertNotNull(result.getAppliedAssumptions());
    }

    @Test
    void shouldCalculateDcfWithCustomParams() {
        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setTotalShares(new BigDecimal("1000000000"));

        ValuationMetrics vm = new ValuationMetrics();
        vm.setClosePrice(new BigDecimal("100"));

        when(valuationMetricsRepository.findCompanyBasicInfo("600519")).thenReturn(info);
        when(valuationMetricsRepository.findLatestByStockCode("600519")).thenReturn(Optional.of(vm));

        DcfRequest request = new DcfRequest();
        request.setGrowthRate(new BigDecimal("0.15"));
        request.setDiscountRate(new BigDecimal("0.10"));
        request.setTerminalGrowthRate(new BigDecimal("0.05"));
        request.setProjectionYears(5);
        request.setBaseCashFlow(new BigDecimal("5000000000"));

        DcfResponse result = service.calculateDcf("600519", request);

        assertNotNull(result.getFairPrice());
        assertNotNull(result.getUpsidePercent());
        assertEquals(new BigDecimal("0.15"), result.getAppliedAssumptions().getGrowthRate());
    }

    @Test
    void shouldHandleDcfWhenTotalSharesIsNull() {
        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setTotalShares(null);

        when(valuationMetricsRepository.findCompanyBasicInfo("600519")).thenReturn(info);

        DcfRequest request = new DcfRequest();
        request.setBaseCashFlow(new BigDecimal("1000000000"));

        DcfResponse result = service.calculateDcf("600519", request);

        // totalShares 为空时，无法计算 fairPrice
        assertNull(result.getFairPrice());
    }

    @Test
    void shouldGenerateHighPeWarning() {
        ValuationMetrics vm = new ValuationMetrics();
        vm.setPeTtmPercentile(new BigDecimal("0.95"));
        vm.setPbPercentile(new BigDecimal("0.92"));

        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockName("测试");
        info.setTotalShares(BigDecimal.ONE);

        when(valuationMetricsRepository.findLatestByStockCode("600001")).thenReturn(Optional.of(vm));
        when(valuationMetricsRepository.findCompanyBasicInfo("600001")).thenReturn(info);
        when(valuationMetricsRepository.findLatestFundamentalMetrics("600001")).thenReturn(Optional.empty());

        Optional<ValuationOverviewResponse> result = service.getValuationOverview("600001");

        assertTrue(result.isPresent());
        assertFalse(result.get().getWarnings().isEmpty());
        assertEquals("high", result.get().getWarnings().get(0).getLevel());
    }

    @Test
    void shouldGenerateMediumPeWarning() {
        ValuationMetrics vm = new ValuationMetrics();
        vm.setPeTtmPercentile(new BigDecimal("0.75"));
        vm.setPbPercentile(new BigDecimal("0.50"));

        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockName("测试");
        info.setTotalShares(BigDecimal.ONE);

        when(valuationMetricsRepository.findLatestByStockCode("600002")).thenReturn(Optional.of(vm));
        when(valuationMetricsRepository.findCompanyBasicInfo("600002")).thenReturn(info);
        when(valuationMetricsRepository.findLatestFundamentalMetrics("600002")).thenReturn(Optional.empty());

        Optional<ValuationOverviewResponse> result = service.getValuationOverview("600002");

        assertTrue(result.isPresent());
        assertFalse(result.get().getWarnings().isEmpty());
        assertEquals("medium", result.get().getWarnings().get(0).getLevel());
    }

    @Test
    void shouldNotGenerateWarningWhenPeIsLow() {
        ValuationMetrics vm = new ValuationMetrics();
        vm.setPeTtmPercentile(new BigDecimal("0.20"));
        vm.setPbPercentile(new BigDecimal("0.30"));

        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockName("测试");
        info.setTotalShares(BigDecimal.ONE);

        when(valuationMetricsRepository.findLatestByStockCode("600003")).thenReturn(Optional.of(vm));
        when(valuationMetricsRepository.findCompanyBasicInfo("600003")).thenReturn(info);
        when(valuationMetricsRepository.findLatestFundamentalMetrics("600003")).thenReturn(Optional.empty());

        Optional<ValuationOverviewResponse> result = service.getValuationOverview("600003");

        assertTrue(result.isPresent());
        assertTrue(result.get().getWarnings().isEmpty());
    }

    @Test
    void shouldCalculateCompositeScoreWithFundamentalMetrics() {
        ValuationMetrics vm = new ValuationMetrics();
        vm.setPeTtmPercentile(new BigDecimal("0.50"));
        vm.setPbPercentile(new BigDecimal("0.50"));
        vm.setPsTtmPercentile(new BigDecimal("0.50"));

        CompanyBasicInfo info = new CompanyBasicInfo();
        info.setStockName("测试");
        info.setTotalShares(BigDecimal.ONE);

        StockFundamentalMetrics fm = new StockFundamentalMetrics();
        fm.setRoe(new BigDecimal("15"));
        fm.setRevenueYoy(new BigDecimal("20"));
        fm.setCashflowProfitRatio(new BigDecimal("100"));
        fm.setPeriodExpenseRate(new BigDecimal("15"));

        when(valuationMetricsRepository.findLatestByStockCode("600004")).thenReturn(Optional.of(vm));
        when(valuationMetricsRepository.findCompanyBasicInfo("600004")).thenReturn(info);
        when(valuationMetricsRepository.findLatestFundamentalMetrics("600004")).thenReturn(Optional.of(fm));

        Optional<ValuationOverviewResponse> result = service.getValuationOverview("600004");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getCompositeScore());
        assertTrue(result.get().getCompositeScore().getFinancialHealthScore() > 0);
        assertTrue(result.get().getCompositeScore().getValuationAppealScore() > 0);
    }

    @Test
    void shouldGetIndustryRank() {
        IndustryRankItem item1 = new IndustryRankItem();
        item1.setStockCode("600001");
        item1.setStockName("公司A");
        item1.setIndustry("信息技术");
        item1.setRoe(new BigDecimal("15"));
        item1.setTotalRevenue(new BigDecimal("100000000"));

        IndustryRankItem item2 = new IndustryRankItem();
        item2.setStockCode("600002");
        item2.setStockName("公司B");
        item2.setIndustry("信息技术");
        item2.setRoe(new BigDecimal("10"));
        item2.setTotalRevenue(new BigDecimal("80000000"));

        when(repository.findIndustryByStockCode("600001")).thenReturn("信息技术");
        when(repository.findIndustryRankItems("信息技术")).thenReturn(List.of(item1, item2));

        var response = service.getIndustryRank("600001", "roe", "desc");

        assertEquals(1, response.getRank());
        assertEquals(2, response.getTotal());
        assertEquals("roe", response.getSortBy());
    }

    @Test
    void shouldReturnEmptyIndustryRankWhenIndustryNotFound() {
        when(repository.findIndustryByStockCode("999999")).thenReturn(null);

        var response = service.getIndustryRank("999999", "roe", "desc");

        assertEquals(0, response.getRank());
        assertEquals(0, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void shouldSortIndustryRankByRevenue() {
        IndustryRankItem item1 = new IndustryRankItem();
        item1.setStockCode("600001");
        item1.setTotalRevenue(new BigDecimal("100"));

        IndustryRankItem item2 = new IndustryRankItem();
        item2.setStockCode("600002");
        item2.setTotalRevenue(new BigDecimal("200"));

        when(repository.findIndustryByStockCode("600001")).thenReturn("信息技术");
        when(repository.findIndustryRankItems("信息技术")).thenReturn(List.of(item1, item2));

        var response = service.getIndustryRank("600001", "revenue", "asc");

        assertEquals(1, response.getRank());
        assertEquals("600001", response.getItems().get(0).getStockCode());
    }
}
