package com.example.securityanalyze.research.api;

import lombok.Data;

/**
 * 综合评分 DTO
 */
@Data
public class CompositeScoreDto {

    /** 财务健康分（0-100） */
    private Integer financialHealthScore;

    /** 估值吸引力分（0-100） */
    private Integer valuationAppealScore;

    /** 综合得分（0-100） */
    private Integer overallScore;
}
