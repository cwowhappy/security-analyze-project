package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 估值历史序列单项 DTO
 */
@Data
public class ValuationHistoryItemDto {

    private String tradeDate;
    private BigDecimal closePrice;
    private BigDecimal peTtm;
    private BigDecimal peLyr;
    private BigDecimal pb;
    private BigDecimal psTtm;
}
