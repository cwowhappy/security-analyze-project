package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 现金流量表 JDBC 持久化实体，与数据库表 tb_financial_cashflow 映射。
 */
@Data
public class FinancialCashflowEntity {

    private String id;
    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private BigDecimal cfOperating;
    private BigDecimal cfInvesting;
    private BigDecimal cfFinancing;
    private BigDecimal netCashFlow;
    private BigDecimal freeCashFlow;
    private BigDecimal capex;
    private BigDecimal cashReceivedOperating;
    private BigDecimal taxPaid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
