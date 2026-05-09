package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IndustryRankItemDto {

    private String stockCode;
    private String stockName;
    private String industry;
    private BigDecimal totalRevenue;
    private BigDecimal parentNetProfit;
    private BigDecimal grossMargin;
    private BigDecimal roe;
    private BigDecimal debtRatio;
}
