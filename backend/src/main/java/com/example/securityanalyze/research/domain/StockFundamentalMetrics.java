package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockFundamentalMetrics {

    private Long id;
    private String stockCode;
    private Integer reportYear;

    // 同比增长率
    private BigDecimal revenueYoy;
    private BigDecimal profitYoy;
    private BigDecimal assetGrowthRate;

    // 效率指标
    private BigDecimal roe;
    private BigDecimal roa;
    private BigDecimal assetTurnover;
    private BigDecimal equityMultiplier;

    // 偿债指标
    private BigDecimal currentRatio;
    private BigDecimal quickRatio;

    // 盈利质量
    private BigDecimal cashflowProfitRatio;
    private BigDecimal periodExpenseRate;

    // 审计字段
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
