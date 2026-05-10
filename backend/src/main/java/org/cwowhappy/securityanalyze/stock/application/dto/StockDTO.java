package org.cwowhappy.securityanalyze.stock.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票应用层 DTO。
 */
@Data
@Builder
public class StockDTO {

    private String id;
    private String symbol;
    private String name;
    private String market;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private LocalDateTime updatedAt;
}
