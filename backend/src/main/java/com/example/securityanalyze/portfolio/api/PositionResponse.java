package com.example.securityanalyze.portfolio.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PositionResponse {
    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private BigDecimal currentQuantity;
    private BigDecimal avgCost;
    private BigDecimal closePrice;
    private BigDecimal marketValue;
    private BigDecimal totalCost;
    private BigDecimal floatingPnl;
    private BigDecimal floatingPnlRate;
    private BigDecimal realizedPnl;
    private LocalDate firstBuyDate;
    private LocalDate lastTradeDate;
    private BigDecimal weight;
}
