package com.example.securityanalyze.portfolio.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("position")
public class Position {
    @Id
    private Long id;
    private Long portfolioId;
    private String stockCode;
    private BigDecimal currentQuantity;
    private BigDecimal totalCost;
    private BigDecimal avgCost;
    private BigDecimal realizedPnl;
    private LocalDate firstBuyDate;
    private LocalDate lastTradeDate;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime updatedAt;
}
