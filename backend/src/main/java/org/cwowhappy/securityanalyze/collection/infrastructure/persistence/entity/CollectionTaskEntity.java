package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务 JDBC 持久化实体，与数据库表 tb_collection_task 映射。
 */
@Data
public class CollectionTaskEntity {

    private String id;
    private String taskType;
    private String taskParams;
    private String status;
    private String dataSource;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private LocalDateTime scheduledAt;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
