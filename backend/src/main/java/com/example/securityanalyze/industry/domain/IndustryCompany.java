package com.example.securityanalyze.industry.domain;

import lombok.Data;

import java.time.LocalDate;

/**
 * 行业下的公司信息（领域对象）
 */
@Data
public class IndustryCompany {

    private String stockCode;
    private String stockName;
    private String industry;
    private String region;
    private LocalDate listingDate;
    private String market;
}
