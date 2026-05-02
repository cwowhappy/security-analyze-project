package com.example.securityanalyze.finance.api;

import lombok.Data;

import java.util.List;

@Data
public class FinanceReportListResponse {

    private String stockCode;
    private String stockName;
    private List<FinanceReportListItem> items;
}
