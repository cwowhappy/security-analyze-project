package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AnnualMetricDto {

    private String reportDate;
    private Integer reportYear;

    // 盈利能力
    private BigDecimal totalRevenue;
    private BigDecimal operateIncome;
    private BigDecimal operateCost;
    private BigDecimal parentNetProfit;
    private BigDecimal grossMargin;
    private BigDecimal netMargin;
    private BigDecimal roe;

    // 资产负债
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal debtRatio;

    // 现金流
    private BigDecimal operatingCashFlow;
    private BigDecimal investingCashFlow;
    private BigDecimal financingCashFlow;
    private BigDecimal endCce;
    private BigDecimal cashflowProfitRatio;

    // 成本费用
    private BigDecimal saleExpense;
    private BigDecimal manageExpense;
    private BigDecimal researchExpense;
    private BigDecimal financeExpense;
    private BigDecimal periodExpenseRate;
}
