package org.cwowhappy.securityanalyze.collection.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 采集监控覆盖度概览 DTO。
 */
@Data
@Builder
public class CollectionMonitorOverviewDTO {

    private String taskType;
    private Long totalCount;
    private Long recentSuccessCount;
    private Long recentExpiredCount;
}
