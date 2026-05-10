package org.cwowhappy.securityanalyze.collection.application.service;

import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskDTO;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;

import java.util.Optional;

/**
 * 采集任务应用服务接口。
 */
public interface CollectionTaskAppService {

    Optional<CollectionTaskDTO> findById(String id);

    PageResult<CollectionTaskDTO> findByPage(PageQuery pageQuery, String status, String taskType);

    String createTask(CollectionTaskDTO dto);
}
