package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 指标统计值（最小、最大、中位数）
 */
@Data
public class MetricStats {

    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal median;
    private BigDecimal p30;  // 30% 分位
    private BigDecimal p70;  // 70% 分位
}
