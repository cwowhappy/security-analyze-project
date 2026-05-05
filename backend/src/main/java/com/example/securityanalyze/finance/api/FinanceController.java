package com.example.securityanalyze.finance.api;

import com.example.securityanalyze.finance.application.FinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/{stockCode}/reports")
    public ResponseEntity<FinanceReportListResponse> listReports(
            @PathVariable String stockCode) {
        log.info("查询财务报告列表, stockCode={}", stockCode);
        FinanceReportListResponse response = financeService.listReports(stockCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<FinanceReportResponse> getReportDetail(
            @PathVariable Long reportId) {
        log.info("查询财务报告详情, reportId={}", reportId);
        Optional<FinanceReportResponse> detail = financeService.getReportDetail(reportId);
        return detail.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{stockCode}/indicators")
    public ResponseEntity<FinanceIndicatorResponse> getIndicators(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "totalRevenue,netProfit,grossMargin,netMargin,debtRatio") String metrics,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String reportType) {
        log.info("查询财务指标, stockCode={}, metrics={}, reportType={}", stockCode, metrics, reportType);
        List<String> metricList = Arrays.asList(metrics.split(","));
        FinanceIndicatorResponse response = financeService.getIndicators(stockCode, metricList, startDate, endDate, reportType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{stockCode}/indicators/yearly")
    public ResponseEntity<FinanceIndicatorResponse> getYearlyIndicators(
            @PathVariable String stockCode,
            @RequestParam int year) {
        log.info("查询年度财务指标对比, stockCode={}, year={}", stockCode, year);
        FinanceIndicatorResponse response = financeService.getYearlyIndicators(stockCode, year);
        return ResponseEntity.ok(response);
    }
}
