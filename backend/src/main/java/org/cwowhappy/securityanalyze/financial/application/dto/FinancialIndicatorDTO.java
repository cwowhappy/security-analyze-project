package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务指标传输对象。
 */
@Getter
@Builder
public class FinancialIndicatorDTO {

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;

    // 盈利能力
    private BigDecimal roe;
    private BigDecimal roa;
    private BigDecimal roic;
    private BigDecimal grossMargin;
    private BigDecimal netMargin;
    private BigDecimal netMarginExcl;

    // 偿债能力
    private BigDecimal debtRatio;
    private BigDecimal currentRatio;
    private BigDecimal quickRatio;
    private BigDecimal netDebtRatio;
    private BigDecimal equityRatio;

    // 运营效率
    private BigDecimal dso;
    private BigDecimal dio;
    private BigDecimal dpo;
    private BigDecimal ccc;
    private BigDecimal assetTurnover;
    private BigDecimal fixedAssetTurnover;

    // 成长性
    private BigDecimal revenueGrowth;
    private BigDecimal npParentGrowth;
    private BigDecimal npExclGrowth;
    private BigDecimal cfoGrowth;
    private BigDecimal equityGrowth;
    private BigDecimal assetGrowth;

    // 估值
    private BigDecimal pe;
    private BigDecimal pb;
    private BigDecimal ps;
    private BigDecimal peg;
    private BigDecimal evEbitda;
    private BigDecimal dividendYield;
    private BigDecimal marketCap;

    // 现金流质量
    private BigDecimal cfoToNp;
}
