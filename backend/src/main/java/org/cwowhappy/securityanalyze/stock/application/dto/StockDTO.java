package org.cwowhappy.securityanalyze.stock.application.dto;

import lombok.Builder;
import lombok.Data;
import org.cwowhappy.securityanalyze.shared.dto.CompanyBriefDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票应用层 DTO。
 */
@Data
@Builder
public class StockDTO {

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
    private CompanyBriefDTO company;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
