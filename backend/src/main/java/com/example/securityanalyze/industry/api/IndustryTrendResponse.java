package com.example.securityanalyze.industry.api;

import lombok.Data;

import java.util.List;

@Data
public class IndustryTrendResponse {

    private String standard;
    private String industryName;
    private String industryCode;
    private String period;
    private List<TrendDataPoint> data;
    private boolean fallback;
}
