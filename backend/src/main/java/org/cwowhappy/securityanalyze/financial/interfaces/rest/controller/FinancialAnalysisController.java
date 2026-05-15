package org.cwowhappy.securityanalyze.financial.interfaces.rest.controller;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.application.dto.*;
import org.cwowhappy.securityanalyze.financial.application.service.FinancialAnalysisAppService;
import org.cwowhappy.securityanalyze.financial.application.service.FinancialIndicatorAppService;
import org.cwowhappy.securityanalyze.financial.application.service.FinancialReportAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 财务分析 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/stocks/{stockCode}/financial")
@RequiredArgsConstructor
public class FinancialAnalysisController {

    private final FinancialReportAppService reportAppService;
    private final FinancialIndicatorAppService indicatorAppService;
    private final FinancialAnalysisAppService analysisAppService;

    @GetMapping("/income")
    public ApiResponse<List<FinancialIncomeDTO>> getIncomeStatements(
            @PathVariable String stockCode,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<FinancialIncomeDTO> data = reportAppService.getIncomeStatements(stockCode, reportType, limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/balance")
    public ApiResponse<List<FinancialBalanceDTO>> getBalanceSheets(
            @PathVariable String stockCode,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<FinancialBalanceDTO> data = reportAppService.getBalanceSheets(stockCode, reportType, limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/cashflow")
    public ApiResponse<List<FinancialCashflowDTO>> getCashflowStatements(
            @PathVariable String stockCode,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<FinancialCashflowDTO> data = reportAppService.getCashflowStatements(stockCode, reportType, limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/indicator")
    public ApiResponse<List<FinancialIndicatorDTO>> getIndicators(
            @PathVariable String stockCode,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        List<FinancialIndicatorDTO> data = indicatorAppService.getIndicators(stockCode, reportType, limit);
        return ApiResponse.success(data);
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendDataDTO>> getTrend(
            @PathVariable String stockCode,
            @RequestParam String metrics,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false, defaultValue = "8") int periods) {
        List<String> metricList = Arrays.asList(metrics.split(","));
        List<TrendDataDTO> data = analysisAppService.getTrend(stockCode, metricList, reportType, periods);
        return ApiResponse.success(data);
    }

    @GetMapping("/dupont")
    public ApiResponse<DupontAnalysisDTO> getDupontAnalysis(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(required = false) String reportType) {
        DupontAnalysisDTO data = analysisAppService.getDupontAnalysis(stockCode, reportDate, reportType);
        return ApiResponse.success(data);
    }

    @GetMapping("/peer-comparison")
    public ApiResponse<PeerComparisonDTO> getPeerComparison(
            @PathVariable String stockCode,
            @RequestParam String metric,
            @RequestParam(required = false) String reportType) {
        PeerComparisonDTO data = analysisAppService.getPeerComparison(stockCode, metric, reportType);
        return ApiResponse.success(data);
    }
}
