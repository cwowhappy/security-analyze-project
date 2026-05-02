package com.example.securityanalyze.industry.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrendDataPoint {

    private String date;
    private BigDecimal close;
    private BigDecimal changePercent;
}
