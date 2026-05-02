package com.example.securityanalyze.finance.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Table("financial_report")
public class FinancialReport {

    @Id
    private Long id;

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private Integer reportYear;
    private LocalDate noticeDate;
    private String currency;

    // 资产负债表
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal monetaryFunds;
    private BigDecimal accountsReceivable;
    private BigDecimal inventory;
    private BigDecimal totalCurrentAssets;
    private BigDecimal totalNoncurrentAssets;
    private BigDecimal totalCurrentLiabilities;
    private BigDecimal totalNoncurrentLiabilities;

    // 利润表
    private BigDecimal totalRevenue;
    private BigDecimal operateIncome;
    private BigDecimal operateCost;
    private BigDecimal saleExpense;
    private BigDecimal manageExpense;
    private BigDecimal researchExpense;
    private BigDecimal financeExpense;
    private BigDecimal operateProfit;
    private BigDecimal totalProfit;
    private BigDecimal netProfit;
    private BigDecimal parentNetProfit;

    // 现金流量表
    private BigDecimal operatingCashFlow;
    private BigDecimal investingCashFlow;
    private BigDecimal financingCashFlow;
    private BigDecimal cceAdd;
    private BigDecimal endCce;

    // JSONB 完整数据
    private Map<String, Object> balanceSheet;
    private Map<String, Object> profitSheet;
    private Map<String, Object> cashFlowSheet;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
