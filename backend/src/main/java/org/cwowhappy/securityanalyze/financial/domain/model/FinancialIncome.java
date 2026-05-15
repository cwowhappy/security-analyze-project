package org.cwowhappy.securityanalyze.financial.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 利润表领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class FinancialIncome {

    private String id;
    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private BigDecimal basicEps;
    private BigDecimal dilutedEps;
    private BigDecimal totalRevenue;
    private BigDecimal revenue;
    private BigDecimal operatingCost;
    private BigDecimal grossProfit;
    private BigDecimal sellingExpense;
    private BigDecimal adminExpense;
    private BigDecimal rdExpense;
    private BigDecimal financialExpense;
    private BigDecimal operatingProfit;
    private BigDecimal totalProfit;
    private BigDecimal netProfit;
    private BigDecimal npParentCompany;
    private BigDecimal npExclNonrecurring;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
