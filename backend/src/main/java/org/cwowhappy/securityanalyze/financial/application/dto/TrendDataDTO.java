package org.cwowhappy.securityanalyze.financial.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 趋势数据传输对象（用于图表展示）。
 */
@Getter
@Builder
public class TrendDataDTO {

    private String stockCode;
    private String metric;
    private List<TrendPoint> data;

    @Getter
    @Builder
    public static class TrendPoint {
        private LocalDate reportDate;
        private BigDecimal value;
    }
}
