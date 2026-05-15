package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 杜邦分析结果传输对象。
 */
@Getter
@Builder
public class DupontAnalysisDTO {

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;

    /** ROE = 净利率 × 资产周转率 × 权益乘数 */
    private BigDecimal roe;

    /** 净利率（盈利能力） */
    private BigDecimal netMargin;

    /** 资产周转率（运营效率） */
    private BigDecimal assetTurnover;

    /** 权益乘数（财务杠杆）= 总资产 / 净资产 */
    private BigDecimal equityMultiplier;
}
