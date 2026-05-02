package com.example.securityanalyze.industry.api;

import lombok.Data;

import java.util.List;

@Data
public class IndustryTrendResponse {

    private String industryName;
    private String period;
    private List<TrendDataPoint> data;
    private boolean fallback;
}
