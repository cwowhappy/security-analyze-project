package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单只股票某交易日的估值指标（领域模型）
 */
@Data
public class ValuationMetrics {

    private String stockCode;
    private LocalDate tradeDate;
    private BigDecimal closePrice;

    // 估值指标
    private BigDecimal peTtm;
    private BigDecimal peLyr;
    private BigDecimal pb;
    private BigDecimal psTtm;

    // 历史分位数（0~1）
    private BigDecimal peTtmPercentile;
    private BigDecimal pbPercentile;
    private BigDecimal psTtmPercentile;

    // DCF 近似
    private BigDecimal dcfFairPrice;
}
