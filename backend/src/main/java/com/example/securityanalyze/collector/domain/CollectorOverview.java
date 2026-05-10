package com.example.securityanalyze.collector.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集概览条目（领域对象）
 */
@Data
public class CollectorOverview {

    private String dataType;
    private String dataTypeLabel;
    private Integer totalRows;
    private LocalDateTime lastUpdatedAt;
    private String lastTaskStatus;
    private Long lastTaskDurationSeconds;
}
