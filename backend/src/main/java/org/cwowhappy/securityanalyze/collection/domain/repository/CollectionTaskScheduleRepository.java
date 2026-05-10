package org.cwowhappy.securityanalyze.collection.domain.repository;

import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskSchedule;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskScheduleId;

import java.util.List;
import java.util.Optional;

/**
 * 采集任务调度领域仓库接口。
 */
public interface CollectionTaskScheduleRepository {

    Optional<CollectionTaskSchedule> findById(CollectionTaskScheduleId id);

    List<CollectionTaskSchedule> findAll();

    List<CollectionTaskSchedule> findEnabled();

    CollectionTaskScheduleId save(CollectionTaskSchedule schedule);

    void deleteById(CollectionTaskScheduleId id);
}
