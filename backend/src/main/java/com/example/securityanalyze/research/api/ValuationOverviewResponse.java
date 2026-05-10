package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 估值概览响应 DTO
 */
@Data
public class ValuationOverviewResponse {

    private String stockCode;
    private String stockName;

    /** 当前股价 */
    private BigDecimal currentPrice;

    /** 总市值（元） */
    private BigDecimal marketCap;

    // 估值指标
    private BigDecimal peTtm;
    private BigDecimal peTtmPercentile;
    private BigDecimal peLyr;
    private BigDecimal pb;
    private BigDecimal pbPercentile;
    private BigDecimal psTtm;
    private BigDecimal psTtmPercentile;

    // 综合评分
    private CompositeScoreDto compositeScore;

    // 估值预警
    private List<ValuationWarningDto> warnings;
}
