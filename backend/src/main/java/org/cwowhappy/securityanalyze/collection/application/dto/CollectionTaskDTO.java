package org.cwowhappy.securityanalyze.collection.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务应用层 DTO。
 */
@Data
@Builder
public class CollectionTaskDTO {

    private String id;
    private String taskType;
    private Object taskParams;
    private String status;
    private String dataSource;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
