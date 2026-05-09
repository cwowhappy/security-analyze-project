package com.example.securityanalyze.industry.application;

import lombok.Data;

import java.time.LocalDate;

/**
 * 行业下的公司信息（应用层结果对象，替代 company.api.CompanyListItem）
 */
@Data
public class IndustryCompanyItem {

    private String stockCode;
    private String stockName;
    private String industry;
    private String region;
    private LocalDate listingDate;
    private String market;
}
