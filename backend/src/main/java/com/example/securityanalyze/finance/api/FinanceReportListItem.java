package com.example.securityanalyze.finance.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceReportListItem {

    private Long id;
    private LocalDate reportDate;
    private String reportType;
    private Integer reportYear;
    private LocalDate noticeDate;
    private BigDecimal totalRevenue;
    private BigDecimal netProfit;
    private BigDecimal parentNetProfit;
    private BigDecimal totalAssets;
    private BigDecimal totalEquity;
}
