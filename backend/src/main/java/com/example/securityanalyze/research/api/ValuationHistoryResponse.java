package com.example.securityanalyze.research.api;

import lombok.Data;

import java.util.List;

/**
 * 估值历史趋势响应 DTO
 */
@Data
public class ValuationHistoryResponse {

    private String stockCode;
    private String stockName;
    private List<ValuationHistoryItemDto> items;
}
