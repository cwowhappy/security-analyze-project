package org.cwowhappy.securityanalyze.collection.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务调度应用层 DTO。
 */
@Data
@Builder
public class CollectionTaskScheduleDTO {

    private String id;
    private String name;
    private String taskType;
    private String taskParams;
    private String dataSource;
    private String cronExpression;
    private Boolean enabled;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime createdAt;
}
