package org.cwowhappy.securityanalyze.collection.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTask;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskRepository;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 采集任务应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionTaskAppServiceImpl implements CollectionTaskAppService {

    private final CollectionTaskRepository taskRepository;

    @Override
    public Optional<CollectionTaskDTO> findById(String id) {
        log.debug("查询采集任务: id={}", id);
        return taskRepository.findById(CollectionTaskId.of(id))
                .map(this::toDTO);
    }

    @Override
    public PageResult<CollectionTaskDTO> findByPage(PageQuery pageQuery, String status, String taskType) {
        log.debug("分页查询采集任务: page={}, size={}, status={}, taskType={}",
                pageQuery.getPage(), pageQuery.getSize(), status, taskType);
        PageResult<CollectionTask> domainResult = taskRepository.findByPage(pageQuery, status, taskType);
        return PageResult.<CollectionTaskDTO>builder()
                .list(domainResult.getList().stream().map(this::toDTO).collect(Collectors.toList()))
                .total(domainResult.getTotal())
                .page(domainResult.getPage())
                .size(domainResult.getSize())
                .build();
    }

    @Override
    @Transactional
    public String createTask(CollectionTaskDTO dto) {
        log.info("创建采集任务: taskType={}", dto.getTaskType());
        CollectionTask task = CollectionTask.builder()
                .id(CollectionTaskId.generate())
                .taskType(dto.getTaskType())
                .taskParams(dto.getTaskParams())
                .status("pending")
                .dataSource(dto.getDataSource())
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .scheduledAt(dto.getScheduledAt())
                .createdAt(LocalDateTime.now())
                .build();
        CollectionTaskId id = taskRepository.save(task);
        log.info("采集任务创建成功: id={}", id);
        return id.getValue();
    }

    private CollectionTaskDTO toDTO(CollectionTask task) {
        return CollectionTaskDTO.builder()
                .id(task.getId().getValue())
                .taskType(task.getTaskType())
                .taskParams(task.getTaskParams())
                .status(task.getStatus())
                .dataSource(task.getDataSource())
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failCount(task.getFailCount())
                .scheduledAt(task.getScheduledAt())
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
