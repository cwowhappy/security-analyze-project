package com.example.securityanalyze.collector.domain;

import java.util.List;

/**
 * 采集仪表盘 Repository 接口
 */
public interface CollectorDashboardRepository {

    List<CollectorOverview> findOverview();

    List<CollectorTask> findTasks(String dataType, String status, int offset, int limit);

    long countTasks(String dataType, String status);
}
