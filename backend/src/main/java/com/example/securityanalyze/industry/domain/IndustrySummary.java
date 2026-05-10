package com.example.securityanalyze.industry.domain;

import lombok.Data;

/**
 * 行业统计摘要（领域对象）
 */
@Data
public class IndustrySummary {

    private String industryName;
    private Integer companyCount;
}
