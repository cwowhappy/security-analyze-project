package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JDBC 持久化实体，与数据库表 tb_stock_basic 映射。
 */
@Data
public class StockEntity {

    private String id;
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
    private String companyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
