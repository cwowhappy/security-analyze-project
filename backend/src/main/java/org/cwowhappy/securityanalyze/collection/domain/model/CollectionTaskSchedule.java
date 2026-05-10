package org.cwowhappy.securityanalyze.collection.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 采集任务调度领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class CollectionTaskSchedule {

    private final CollectionTaskScheduleId id;
    private String name;
    private String taskType;
    private String taskParams;
    private String dataSource;
    private String cronExpression;
    private Boolean enabled;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime createdAt;
}
