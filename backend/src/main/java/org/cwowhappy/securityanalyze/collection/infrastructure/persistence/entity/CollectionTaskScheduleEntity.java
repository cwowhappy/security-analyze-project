package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务调度 JDBC 持久化实体，与数据库表 tb_collection_task_schedule 映射。
 */
@Data
public class CollectionTaskScheduleEntity {

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
