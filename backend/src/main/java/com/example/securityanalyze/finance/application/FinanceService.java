package com.example.securityanalyze.finance.application;

import com.example.securityanalyze.finance.api.FinanceIndicatorResponse;
import com.example.securityanalyze.finance.api.FinanceReportListItem;
import com.example.securityanalyze.finance.api.FinanceReportListResponse;
import com.example.securityanalyze.finance.api.FinanceReportResponse;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.finance.domain.FinancialReportRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinancialReportRepository reportRepository;
    private final CompanySecurityRepository companySecurityRepository;

    public FinanceReportListResponse listReports(String stockCode) {
        log.info("查询财务报告列表, stockCode={}", stockCode);
        List<FinancialReport> reports = reportRepository.findByStockCode(stockCode);

        String stockName = companySecurityRepository.findByStockCode(stockCode)
                .map(CompanySecurity::getStockName)
                .orElse(stockCode);

        List<FinanceReportListItem> items = reports.stream()
                .map(this::toListItem)
                .toList();

        FinanceReportListResponse response = new FinanceReportListResponse();
        response.setStockCode(stockCode);
        response.setStockName(stockName);
        response.setItems(items);
        log.info("查询财务报告列表完成, stockCode={}, 返回{}条记录", stockCode, items.size());
        return response;
    }

    public Optional<FinanceReportResponse> getReportDetail(Long reportId) {
        log.info("查询财务报告详情, reportId={}", reportId);
        return reportRepository.findById(reportId)
                .map(this::toDetailResponse);
    }

    public FinanceIndicatorResponse getIndicators(String stockCode, List<String> metrics,
                                                   LocalDate startDate, LocalDate endDate) {
        log.info("计算财务指标, stockCode={}, metrics={}, startDate={}, endDate={}",
                stockCode, metrics, startDate, endDate);
        List<FinancialReport> reports;
        if (startDate != null && endDate != null) {
            reports = reportRepository.findByStockCodeAndDateRange(stockCode, startDate, endDate);
        } else {
            reports = reportRepository.findByStockCode(stockCode);
        }
        // 正序排列，便于趋势展示
        List<FinancialReport> sorted = reports.reversed();

        FinanceIndicatorResponse response = new FinanceIndicatorResponse();
        response.setStockCode(stockCode);
        response.setMetrics(new ArrayList<>());

        for (String metric : metrics) {
            FinanceIndicatorResponse.IndicatorMetric im = buildMetric(metric, sorted);
            if (im != null) {
                response.getMetrics().add(im);
            }
        }

        return response;
    }

    private FinanceReportListItem toListItem(FinancialReport report) {
        FinanceReportListItem item = new FinanceReportListItem();
        item.setId(report.getId());
        item.setReportDate(report.getReportDate());
        item.setReportType(report.getReportType());
        item.setReportYear(report.getReportYear());
        item.setNoticeDate(report.getNoticeDate());
        item.setTotalRevenue(report.getTotalRevenue());
        item.setNetProfit(report.getNetProfit());
        item.setParentNetProfit(report.getParentNetProfit());
        item.setTotalAssets(report.getTotalAssets());
        item.setTotalEquity(report.getTotalEquity());
        return item;
    }

    private FinanceReportResponse toDetailResponse(FinancialReport report) {
        FinanceReportResponse response = new FinanceReportResponse();
        response.setId(report.getId());
        response.setStockCode(report.getStockCode());
        response.setReportDate(report.getReportDate());
        response.setReportType(report.getReportType());
        response.setReportYear(report.getReportYear());
        response.setNoticeDate(report.getNoticeDate());
        response.setCurrency(report.getCurrency());

        FinanceReportResponse.FinanceSummary summary = new FinanceReportResponse.FinanceSummary();
        summary.setTotalAssets(report.getTotalAssets());
        summary.setTotalLiabilities(report.getTotalLiabilities());
        summary.setTotalEquity(report.getTotalEquity());
        summary.setTotalRevenue(report.getTotalRevenue());
        summary.setOperateCost(report.getOperateCost());
        summary.setOperateProfit(report.getOperateProfit());
        summary.setNetProfit(report.getNetProfit());
        summary.setParentNetProfit(report.getParentNetProfit());
        summary.setOperatingCashFlow(report.getOperatingCashFlow());
        response.setSummary(summary);

        response.setBalanceSheet(report.getBalanceSheet());
        response.setProfitSheet(report.getProfitSheet());
        response.setCashFlowSheet(report.getCashFlowSheet());

        return response;
    }

    private FinanceIndicatorResponse.IndicatorMetric buildMetric(String metric, List<FinancialReport> reports) {
        return switch (metric) {
            case "totalRevenue" -> buildMetric(metric, "营业总收入", "元", reports, FinancialReport::getTotalRevenue);
            case "netProfit" -> buildMetric(metric, "净利润", "元", reports, FinancialReport::getNetProfit);
            case "parentNetProfit" -> buildMetric(metric, "归母净利润", "元", reports, FinancialReport::getParentNetProfit);
            case "totalAssets" -> buildMetric(metric, "总资产", "元", reports, FinancialReport::getTotalAssets);
            case "totalEquity" -> buildMetric(metric, "净资产", "元", reports, FinancialReport::getTotalEquity);
            case "operatingCashFlow" ->
                    buildMetric(metric, "经营现金流净额", "元", reports, FinancialReport::getOperatingCashFlow);
            case "grossMargin" -> buildGrossMarginMetric(reports);
            case "netMargin" -> buildNetMarginMetric(reports);
            case "debtRatio" -> buildDebtRatioMetric(reports);
            default -> null;
        };
    }

    private FinanceIndicatorResponse.IndicatorMetric buildMetric(
            String metric, String label, String unit,
            List<FinancialReport> reports,
            java.util.function.Function<FinancialReport, BigDecimal> extractor) {

        FinanceIndicatorResponse.IndicatorMetric im = new FinanceIndicatorResponse.IndicatorMetric();
        im.setMetric(metric);
        im.setLabel(label);
        im.setUnit(unit);
        im.setData(new ArrayList<>());

        for (FinancialReport report : reports) {
            BigDecimal value = extractor.apply(report);
            if (value != null) {
                FinanceIndicatorResponse.DataPoint dp = new FinanceIndicatorResponse.DataPoint();
                dp.setReportDate(report.getReportDate().toString());
                dp.setValue(value);
                im.getData().add(dp);
            }
        }
        return im;
    }

    private FinanceIndicatorResponse.IndicatorMetric buildGrossMarginMetric(List<FinancialReport> reports) {
        FinanceIndicatorResponse.IndicatorMetric im = new FinanceIndicatorResponse.IndicatorMetric();
        im.setMetric("grossMargin");
        im.setLabel("毛利率");
        im.setUnit("%");
        im.setData(new ArrayList<>());

        for (FinancialReport report : reports) {
            BigDecimal revenue = report.getOperateIncome();
            BigDecimal cost = report.getOperateCost();
            if (revenue != null && cost != null && revenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = revenue.subtract(cost)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(revenue, 2, RoundingMode.HALF_UP);
                FinanceIndicatorResponse.DataPoint dp = new FinanceIndicatorResponse.DataPoint();
                dp.setReportDate(report.getReportDate().toString());
                dp.setValue(margin);
                im.getData().add(dp);
            }
        }
        return im;
    }

    private FinanceIndicatorResponse.IndicatorMetric buildNetMarginMetric(List<FinancialReport> reports) {
        FinanceIndicatorResponse.IndicatorMetric im = new FinanceIndicatorResponse.IndicatorMetric();
        im.setMetric("netMargin");
        im.setLabel("净利率");
        im.setUnit("%");
        im.setData(new ArrayList<>());

        for (FinancialReport report : reports) {
            BigDecimal revenue = report.getOperateIncome();
            BigDecimal profit = report.getNetProfit();
            if (revenue != null && profit != null && revenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = profit.multiply(BigDecimal.valueOf(100))
                        .divide(revenue, 2, RoundingMode.HALF_UP);
                FinanceIndicatorResponse.DataPoint dp = new FinanceIndicatorResponse.DataPoint();
                dp.setReportDate(report.getReportDate().toString());
                dp.setValue(margin);
                im.getData().add(dp);
            }
        }
        return im;
    }

    private FinanceIndicatorResponse.IndicatorMetric buildDebtRatioMetric(List<FinancialReport> reports) {
        FinanceIndicatorResponse.IndicatorMetric im = new FinanceIndicatorResponse.IndicatorMetric();
        im.setMetric("debtRatio");
        im.setLabel("资产负债率");
        im.setUnit("%");
        im.setData(new ArrayList<>());

        for (FinancialReport report : reports) {
            BigDecimal assets = report.getTotalAssets();
            BigDecimal liabilities = report.getTotalLiabilities();
            if (assets != null && liabilities != null && assets.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = liabilities.multiply(BigDecimal.valueOf(100))
                        .divide(assets, 2, RoundingMode.HALF_UP);
                FinanceIndicatorResponse.DataPoint dp = new FinanceIndicatorResponse.DataPoint();
                dp.setReportDate(report.getReportDate().toString());
                dp.setValue(ratio);
                im.getData().add(dp);
            }
        }
        return im;
    }
}
