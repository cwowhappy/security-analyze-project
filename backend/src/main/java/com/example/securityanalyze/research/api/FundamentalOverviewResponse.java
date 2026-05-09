package com.example.securityanalyze.research.api;

import lombok.Data;

import java.util.List;

@Data
public class FundamentalOverviewResponse {

    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private List<AnnualMetricDto> metrics;
}
