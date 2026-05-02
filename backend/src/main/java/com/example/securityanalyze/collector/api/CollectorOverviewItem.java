package com.example.securityanalyze.collector.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectorOverviewItem {

    private String dataType;
    private String dataTypeLabel;
    private Integer totalRows;
    private LocalDateTime lastUpdatedAt;
    private String lastTaskStatus;
    private Long lastTaskDurationSeconds;
}
