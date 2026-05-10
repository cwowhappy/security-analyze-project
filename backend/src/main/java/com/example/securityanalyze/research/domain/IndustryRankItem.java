package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 行业排名条目（领域对象）
 */
@Data
public class IndustryRankItem {

    private String stockCode;
    private String stockName;
    private String industry;
    private BigDecimal totalRevenue;
    private BigDecimal parentNetProfit;
    private BigDecimal grossMargin;
    private BigDecimal roe;
    private BigDecimal debtRatio;
}
