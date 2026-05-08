package com.example.securityanalyze.portfolio.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PortfolioSummaryResponse {
    private Long portfolioId;
    private String portfolioName;
    private BigDecimal totalMarketValue;
    private BigDecimal totalCost;
    private BigDecimal totalFloatingPnl;
    private BigDecimal totalFloatingPnlRate;
    private BigDecimal totalRealizedPnl;
    private BigDecimal totalAssetReturn;
    private BigDecimal totalAssetReturnRate;
    private Integer holdingCount;
    private LocalDate latestTradeDate;
}
