package com.example.securityanalyze.industry.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 行业趋势数据点（领域对象）
 */
@Data
public class IndustryTrendPoint {

    private String date;
    private BigDecimal close;
    private BigDecimal changePercent;
}
