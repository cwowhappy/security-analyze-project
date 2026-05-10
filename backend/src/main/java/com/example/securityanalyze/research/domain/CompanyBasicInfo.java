package com.example.securityanalyze.research.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 公司基本信息（用于估值分析）
 */
@Data
public class CompanyBasicInfo {

    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private BigDecimal totalShares;
}
