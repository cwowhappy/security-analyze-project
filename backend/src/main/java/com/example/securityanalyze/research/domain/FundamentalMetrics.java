package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.util.List;

@Data
public class FundamentalMetrics {

    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private List<AnnualMetric> annualMetrics;
}
