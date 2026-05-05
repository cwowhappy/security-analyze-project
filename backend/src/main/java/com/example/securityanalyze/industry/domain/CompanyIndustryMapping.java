package com.example.securityanalyze.industry.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司与行业分类的映射关系（支持一对多）
 */
@Data
public class CompanyIndustryMapping {

    private Long id;
    private Long companyId;
    private String standardCode;
    private String level1Code;
    private String level2Code;
    private Boolean primary;
    private LocalDateTime createdAt;
}
