package org.cwowhappy.securityanalyze.collection.domain.repository;

import lombok.Data;

@Data
public class CollectionTaskOverview {
    private String taskType;
    private Long totalCount;
    private Long recentSuccessCount;
    private Long recentExpiredCount;
}
