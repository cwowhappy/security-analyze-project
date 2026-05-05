package com.example.securityanalyze.index.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("index_history")
public class IndexHistory {

    @Id
    private Long id;

    private String indexCode;

    private LocalDate tradeDate;

    private String granularity;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
