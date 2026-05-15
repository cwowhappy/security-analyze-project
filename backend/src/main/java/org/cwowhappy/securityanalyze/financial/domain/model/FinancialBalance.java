package org.cwowhappy.securityanalyze.financial.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产负债表领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class FinancialBalance {

    private String id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
