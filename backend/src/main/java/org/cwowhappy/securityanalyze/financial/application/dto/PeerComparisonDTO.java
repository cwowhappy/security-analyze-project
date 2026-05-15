package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 同业对比结果传输对象。
 */
@Getter
@Builder
public class PeerComparisonDTO {

    private String stockCode;
    private String metric;
    private String metricName;

    /** 当前股票指标值 */
    private BigDecimal stockValue;

    /** 行业平均值 */
    private BigDecimal industryAvg;

    /** 行业中位数 */
    private BigDecimal industryMedian;

    /** 行业最高值 */
    private BigDecimal industryMax;

    /** 行业最低值 */
    private BigDecimal industryMin;

    /** 同业明细列表 */
    private List<PeerItem> peers;

    @Getter
    @Builder
    public static class PeerItem {
        private String stockCode;
        private String stockName;
        private BigDecimal value;
    }
}
