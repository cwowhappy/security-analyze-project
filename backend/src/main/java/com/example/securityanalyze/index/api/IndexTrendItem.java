package com.example.securityanalyze.index.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IndexTrendItem {

    private LocalDate tradeDate;

    private BigDecimal openPrice;

    private BigDecimal highPrice;

    private BigDecimal lowPrice;

    private BigDecimal closePrice;

    private Long volume;

    private BigDecimal amount;

    private BigDecimal amplitude;

    private BigDecimal changePct;

    private BigDecimal changeAmount;

    private BigDecimal turnoverRate;
}
