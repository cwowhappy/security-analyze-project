package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JDBC 持久化实体，与数据库表 stock 映射。
 */
@Data
public class StockEntity {

    private String id;
    private String symbol;
    private String name;
    private String market;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private LocalDateTime updatedAt;
}
