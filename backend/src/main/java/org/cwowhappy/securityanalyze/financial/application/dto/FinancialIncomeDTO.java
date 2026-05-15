package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 利润表传输对象。
 */
@Getter
@Builder
public class FinancialIncomeDTO {

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private BigDecimal basicEps;
    private BigDecimal dilutedEps;
    private BigDecimal totalRevenue;
    private BigDecimal revenue;
    private BigDecimal operatingCost;
    private BigDecimal grossProfit;
    private BigDecimal grossMargin;
    private BigDecimal sellingExpense;
    private BigDecimal adminExpense;
    private BigDecimal rdExpense;
    private BigDecimal financialExpense;
    private BigDecimal operatingProfit;
    private BigDecimal totalProfit;
    private BigDecimal netProfit;
    private BigDecimal npParentCompany;
    private BigDecimal npExclNonrecurring;
    private BigDecimal netMargin;
}
