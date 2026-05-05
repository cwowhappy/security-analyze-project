package com.example.securityanalyze.finance.application;

import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import com.example.securityanalyze.finance.api.FinanceIndicatorResponse;
import com.example.securityanalyze.finance.api.FinanceReportListResponse;
import com.example.securityanalyze.finance.api.FinanceReportResponse;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.finance.domain.FinancialReportRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private FinancialReportRepository reportRepository;

    @Mock
    private CompanySecurityRepository companySecurityRepository;

    @InjectMocks
    private FinanceService financeService;

    @Test
    void shouldListReports() {
        FinancialReport report = createReport("600519", LocalDate.of(2024, 3, 31));
        CompanySecurity security = new CompanySecurity();
        security.setStockName("贵州茅台");

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(report));
        when(companySecurityRepository.findByStockCode("600519")).thenReturn(Optional.of(security));

        FinanceReportListResponse response = financeService.listReports("600519");

        assertEquals("600519", response.getStockCode());
        assertEquals("贵州茅台", response.getStockName());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void shouldListReportsWithUnknownStockName() {
        FinancialReport report = createReport("600519", LocalDate.of(2024, 3, 31));

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(report));
        when(companySecurityRepository.findByStockCode("600519")).thenReturn(Optional.empty());

        FinanceReportListResponse response = financeService.listReports("600519");

        assertEquals("600519", response.getStockName());
    }

    @Test
    void shouldGetReportDetail() {
        FinancialReport report = createReport("600519", LocalDate.of(2024, 3, 31));
        report.setId(1L);

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        Optional<FinanceReportResponse> detail = financeService.getReportDetail(1L);

        assertTrue(detail.isPresent());
        assertEquals("600519", detail.get().getStockCode());
    }

    @Test
    void shouldReturnEmptyWhenReportNotFound() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<FinanceReportResponse> detail = financeService.getReportDetail(999L);

        assertTrue(detail.isEmpty());
    }

    @Test
    void shouldGetIndicatorsWithDateRange() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setTotalRevenue(BigDecimal.valueOf(1000));
        r1.setNetProfit(BigDecimal.valueOf(200));
        r1.setTotalAssets(BigDecimal.valueOf(5000));
        r1.setTotalEquity(BigDecimal.valueOf(3000));
        r1.setOperateIncome(BigDecimal.valueOf(1000));
        r1.setOperateCost(BigDecimal.valueOf(600));
        r1.setTotalLiabilities(BigDecimal.valueOf(2000));

        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

        when(reportRepository.findByStockCodeAndDateRange("600519", start, end))
                .thenReturn(List.of(r1));

        List<String> metrics = List.of("totalRevenue", "netProfit", "totalAssets", "totalEquity", "grossMargin", "netMargin", "debtRatio");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, start, end, "年报");

        assertEquals("600519", response.getStockCode());
        assertFalse(response.getMetrics().isEmpty());
    }

    @Test
    void shouldGetIndicatorsWithoutDateRange() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setTotalRevenue(BigDecimal.valueOf(1000));

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("totalRevenue");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertEquals(1, response.getMetrics().size());
        assertEquals("totalRevenue", response.getMetrics().get(0).getMetric());
    }

    @Test
    void shouldReturnNullForUnknownMetric() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("unknownMetric");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertTrue(response.getMetrics().isEmpty(), "未知 metric 应返回空列表");
    }

    @Test
    void shouldHandleNullValuesInMetrics() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setTotalRevenue(null);
        r1.setNetProfit(null);

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("totalRevenue", "netProfit");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertEquals(2, response.getMetrics().size());
        assertTrue(response.getMetrics().get(0).getData().isEmpty(), "value 为 null 时不应添加数据点");
        assertTrue(response.getMetrics().get(1).getData().isEmpty(), "value 为 null 时不应添加数据点");
    }

    @Test
    void shouldHandleZeroRevenueInGrossMargin() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setOperateIncome(BigDecimal.ZERO);
        r1.setOperateCost(BigDecimal.valueOf(600));

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("grossMargin");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertEquals(1, response.getMetrics().size());
        assertTrue(response.getMetrics().get(0).getData().isEmpty(), "营业收入为 0 时不应计算毛利率");
    }

    @Test
    void shouldHandleNullCostInGrossMargin() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setOperateIncome(BigDecimal.valueOf(1000));
        r1.setOperateCost(null);

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("grossMargin");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertTrue(response.getMetrics().get(0).getData().isEmpty(), "营业成本为 null 时不应计算毛利率");
    }

    @Test
    void shouldHandleNullRevenueInNetMargin() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setOperateIncome(null);
        r1.setNetProfit(BigDecimal.valueOf(200));

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("netMargin");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertTrue(response.getMetrics().get(0).getData().isEmpty(), "营业收入为 null 时不应计算净利率");
    }

    @Test
    void shouldHandleZeroAssetsInDebtRatio() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setTotalAssets(BigDecimal.ZERO);
        r1.setTotalLiabilities(BigDecimal.valueOf(2000));

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("debtRatio");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertTrue(response.getMetrics().get(0).getData().isEmpty(), "总资产为 0 时不应计算资产负债率");
    }

    @Test
    void shouldHandleNullLiabilitiesInDebtRatio() {
        FinancialReport r1 = createReport("600519", LocalDate.of(2024, 3, 31));
        r1.setTotalAssets(BigDecimal.valueOf(5000));
        r1.setTotalLiabilities(null);

        when(reportRepository.findByStockCode("600519")).thenReturn(List.of(r1));

        List<String> metrics = List.of("debtRatio");
        FinanceIndicatorResponse response = financeService.getIndicators("600519", metrics, null, null, "季报");

        assertTrue(response.getMetrics().get(0).getData().isEmpty(), "总负债为 null 时不应计算资产负债率");
    }

    private FinancialReport createReport(String stockCode, LocalDate reportDate) {
        FinancialReport r = new FinancialReport();
        r.setStockCode(stockCode);
        r.setReportDate(reportDate);
        r.setReportType("季报");
        r.setReportYear(2024);
        r.setCurrency("CNY");
        return r;
    }
}
