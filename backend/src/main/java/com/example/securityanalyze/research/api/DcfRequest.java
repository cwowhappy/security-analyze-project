package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DCF 估值计算请求 DTO
 */
@Data
public class DcfRequest {

    /** 预测期增长率（如 0.10 表示 10%） */
    private BigDecimal growthRate;

    /** 折现率（如 0.08 表示 8%） */
    private BigDecimal discountRate;

    /** 永续增长率（如 0.03 表示 3%） */
    private BigDecimal terminalGrowthRate;

    /** 预测年限（默认 10） */
    private Integer projectionYears;

    /** 自由现金流基数（可选，不传则自动取最近年报经营现金流） */
    private BigDecimal baseCashFlow;
}
