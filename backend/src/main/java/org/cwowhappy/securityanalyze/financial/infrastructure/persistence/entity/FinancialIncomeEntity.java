package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 利润表 JDBC 持久化实体，与数据库表 tb_financial_income 映射。
 */
@Data
public class FinancialIncomeEntity {

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
