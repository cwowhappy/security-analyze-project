package org.cwowhappy.securityanalyze.financial.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 现金流量表领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class FinancialCashflow {

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
