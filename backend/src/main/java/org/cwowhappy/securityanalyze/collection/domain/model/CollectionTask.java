package org.cwowhappy.securityanalyze.collection.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 采集任务领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class CollectionTask {

    private final CollectionTaskId id;
    private String taskType;
    private String taskParams;
    private String status;
    private String mode;
    private String sourcePriority;
    private String dataSource;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
