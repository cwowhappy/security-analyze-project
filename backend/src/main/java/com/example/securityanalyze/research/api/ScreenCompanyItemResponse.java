package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScreenCompanyItemResponse {

    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private BigDecimal latestRevenue;
    private BigDecimal latestProfit;
}
