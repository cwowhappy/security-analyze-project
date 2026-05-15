package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产负债表 JDBC 持久化实体，与数据库表 tb_financial_balance 映射。
 */
@Data
public class FinancialBalanceEntity {

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
