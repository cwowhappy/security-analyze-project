package com.example.securityanalyze.research.api;

import lombok.Data;

/**
 * 估值预警 DTO
 */
@Data
public class ValuationWarningDto {

    /** 指标名称，如 PE_TTM */
    private String metric;

    /** 预警级别：high / medium / low */
    private String level;

    /** 预警描述 */
    private String message;
}
