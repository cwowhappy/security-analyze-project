package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PeerMetric {

    private String stockCode;
    private String stockName;
    private String industry;
    private BigDecimal totalRevenue;
    private BigDecimal parentNetProfit;
    private BigDecimal roe;
    private BigDecimal debtRatio;
}
