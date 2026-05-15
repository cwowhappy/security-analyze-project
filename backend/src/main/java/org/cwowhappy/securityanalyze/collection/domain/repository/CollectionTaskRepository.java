package org.cwowhappy.securityanalyze.collection.domain.repository;

import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTask;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskId;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 采集任务领域仓库接口。
 */
public interface CollectionTaskRepository {

    Optional<CollectionTask> findById(CollectionTaskId id);

    PageResult<CollectionTask> findByPage(PageQuery pageQuery, String status, String taskType);

    List<CollectionTask> findByStatus(String status);

    CollectionTaskId save(CollectionTask task);

    List<CollectionTaskOverview> findMonitorOverview(int ttlHours);

    Long countAllStocks();
}
