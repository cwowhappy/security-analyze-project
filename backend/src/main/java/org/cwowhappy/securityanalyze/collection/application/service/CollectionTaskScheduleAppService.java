package org.cwowhappy.securityanalyze.collection.application.service;

import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskScheduleDTO;

import java.util.List;

/**
 * 采集任务调度应用服务接口。
 */
public interface CollectionTaskScheduleAppService {

    List<CollectionTaskScheduleDTO> findAll();

    String create(CollectionTaskScheduleDTO dto);

    void update(String id, CollectionTaskScheduleDTO dto);

    void delete(String id);
}
