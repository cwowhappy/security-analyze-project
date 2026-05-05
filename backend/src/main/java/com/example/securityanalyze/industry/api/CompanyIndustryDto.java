package com.example.securityanalyze.industry.api;

import lombok.Data;

/**
 * 公司行业分类 DTO
 */
@Data
public class CompanyIndustryDto {

    private String standardCode;
    private String standardName;
    private String level1Code;
    private String level1Name;
    private String level2Code;
    private String level2Name;
    private Boolean primary;
}
