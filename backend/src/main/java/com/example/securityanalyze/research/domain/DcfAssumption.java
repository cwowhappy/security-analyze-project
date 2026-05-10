package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DCF 估值输入参数（领域模型）
 */
@Data
public class DcfAssumption {

    /** 预测期增长率（默认 10%） */
    private BigDecimal growthRate;

    /** 折现率（默认 8%） */
    private BigDecimal discountRate;

    /** 永续增长率（默认 3%） */
    private BigDecimal terminalGrowthRate;

    /** 预测年限（默认 10 年） */
    private Integer projectionYears;

    /** 自由现金流基数（可选，不传则自动取最近年报经营现金流） */
    private BigDecimal baseCashFlow;
}
