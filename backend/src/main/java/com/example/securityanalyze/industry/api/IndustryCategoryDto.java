package com.example.securityanalyze.industry.api;

import lombok.Data;

/**
 * 行业分类 DTO（支持多标准、多级）
 */
@Data
public class IndustryCategoryDto {

    private String code;
    private String name;
    private Integer level;
    private String parentCode;
    private Integer companyCount;
}
