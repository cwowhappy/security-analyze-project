package com.example.securityanalyze.industry.domain;

import lombok.Data;

/**
 * 行业分类标准字典（申万、东财等）
 */
@Data
public class IndustryClassificationStandard {

    private Integer id;
    private String code;
    private String name;
    private Integer levelCount;
    private String description;
}
