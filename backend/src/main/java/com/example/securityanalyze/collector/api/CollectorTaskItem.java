package com.example.securityanalyze.collector.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectorTaskItem {

    private Long id;
    private String taskName;
    private String taskType;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private Integer rowsAffected;
    private Long durationSeconds;
}
