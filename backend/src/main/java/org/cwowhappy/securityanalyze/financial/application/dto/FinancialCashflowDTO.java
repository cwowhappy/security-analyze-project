package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 现金流量表传输对象。
 */
@Getter
@Builder
public class FinancialCashflowDTO {

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
    private BigDecimal cfoToNetProfit;
}
