package org.cwowhappy.securityanalyze.stock.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class Stock {

    private final StockId id;
    private String symbol;
    private String name;
    private String market;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private LocalDateTime updatedAt;

    /**
     * 更新价格。
     */
    public void updatePrice(BigDecimal newPrice, BigDecimal newChangePercent) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        this.currentPrice = newPrice;
        this.changePercent = newChangePercent;
        this.updatedAt = LocalDateTime.now();
    }
}
