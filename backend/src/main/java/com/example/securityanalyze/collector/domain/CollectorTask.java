package com.example.securityanalyze.collector.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务记录（领域对象）
 */
@Data
public class CollectorTask {

    private Long id;
    private String taskName;
    private String taskType;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private Integer rowsAffected;
    private Long durationSeconds;
}
