package org.cwowhappy.securityanalyze.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 股票简要信息 DTO（跨模块共享）。
 */
@Data
@Builder
public class StockBriefDTO {

    private String stockCode;
    private String name;
    private String market;
    private String exchange;
    private LocalDate listDate;
}
