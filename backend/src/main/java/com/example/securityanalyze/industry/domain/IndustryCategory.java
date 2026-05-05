package com.example.securityanalyze.industry.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行业分类维度（支持多级、多标准）
 */
@Data
public class IndustryCategory {

    private Long id;
    private String standardCode;
    private Integer level;
    private String code;
    private String name;
    private String parentCode;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 非持久化字段：该分类下的公司数量（仅用于列表展示）
     */
    private transient Integer companyCount;
}
