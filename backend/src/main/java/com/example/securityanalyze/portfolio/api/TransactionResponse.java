package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.domain.TradeType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long id;
    private Long portfolioId;
    private String stockCode;
    private String stockName;
    private LocalDate tradeDate;
    private TradeType tradeType;
    private String tradeTypeLabel;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal fee;
    private BigDecimal tax;
    private BigDecimal amount;
    private BigDecimal realizedPnl;
    private String remark;
    private LocalDateTime createdAt;
}
