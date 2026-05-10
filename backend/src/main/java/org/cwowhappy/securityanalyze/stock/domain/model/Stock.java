package org.cwowhappy.securityanalyze.stock.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class Stock {

    private final StockId id;
    private String stockCode;
    private String name;
    private String market;
    private String tsCode;
    private String fullName;
    private String exchange;
    private LocalDate listDate;
    private String industry;
    private String area;
    private Long totalShares;
    private Long floatShares;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
