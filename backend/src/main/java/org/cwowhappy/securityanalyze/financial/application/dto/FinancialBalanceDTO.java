package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产负债表传输对象。
 */
@Getter
@Builder
public class FinancialBalanceDTO {

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal equityParentCompany;
    private BigDecimal currentAssets;
    private BigDecimal nonCurrentAssets;
    private BigDecimal cashEquivalents;
    private BigDecimal accountsReceivable;
    private BigDecimal inventories;
    private BigDecimal currentLiabilities;
    private BigDecimal nonCurrentLiabilities;
    private BigDecimal accountsPayable;
    private BigDecimal shortTermBorrowings;
    private BigDecimal longTermBorrowings;
    private BigDecimal goodwill;
    private BigDecimal debtRatio;
}
