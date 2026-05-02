package com.example.securityanalyze.finance.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class FinanceReportResponse {

    private Long id;
    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private Integer reportYear;
    private LocalDate noticeDate;
    private String currency;

    private FinanceSummary summary;

    private Map<String, Object> balanceSheet;
    private Map<String, Object> profitSheet;
    private Map<String, Object> cashFlowSheet;

    @Data
    public static class FinanceSummary {
        private BigDecimal totalAssets;
        private BigDecimal totalLiabilities;
        private BigDecimal totalEquity;
        private BigDecimal totalRevenue;
        private BigDecimal operateCost;
        private BigDecimal operateProfit;
        private BigDecimal netProfit;
        private BigDecimal parentNetProfit;
        private BigDecimal operatingCashFlow;
    }
}
