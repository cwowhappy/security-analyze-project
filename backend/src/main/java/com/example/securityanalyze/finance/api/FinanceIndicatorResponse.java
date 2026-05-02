package com.example.securityanalyze.finance.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FinanceIndicatorResponse {

    private String stockCode;
    private List<IndicatorMetric> metrics;

    @Data
    public static class IndicatorMetric {
        private String metric;
        private String label;
        private String unit;
        private List<DataPoint> data;
    }

    @Data
    public static class DataPoint {
        private String reportDate;
        private BigDecimal value;
    }
}
