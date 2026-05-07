package com.example.securityanalyze.portfolio.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("transaction_record")
public class TransactionRecord {
    @Id
    private Long id;
    private Long portfolioId;
    private String stockCode;
    private LocalDate tradeDate;
    private TradeType tradeType;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal fee;
    private BigDecimal tax;
    private BigDecimal amount;
    private BigDecimal realizedPnl;
    private String remark;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
}
