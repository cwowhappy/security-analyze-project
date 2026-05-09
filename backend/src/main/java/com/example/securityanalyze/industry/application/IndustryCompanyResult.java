package com.example.securityanalyze.industry.application;

import lombok.Data;

import java.util.List;

/**
 * 行业公司列表查询结果（应用层结果对象，替代 company.api.CompanyListResponse）
 */
@Data
public class IndustryCompanyResult {

    private List<IndustryCompanyItem> items;
    private long total;
    private int page;
    private int size;
}
