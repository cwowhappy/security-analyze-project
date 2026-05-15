package org.cwowhappy.securityanalyze.collection.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorBaselineDTO;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorOverviewDTO;
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
import java.util.List;
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
    private final ObjectMapper objectMapper;

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
        String taskParamsJson = null;
        if (dto.getTaskParams() != null) {
            try {
                taskParamsJson = objectMapper.writeValueAsString(dto.getTaskParams());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("taskParams 序列化失败", e);
            }
        }
        CollectionTask task = CollectionTask.builder()
                .id(CollectionTaskId.generate())
                .taskType(dto.getTaskType())
                .taskParams(taskParamsJson)
                .status("pending")
                .dataSource(dto.getDataSource())
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        CollectionTaskId id = taskRepository.save(task);
        log.info("采集任务创建成功: id={}", id);
        return id.getValue();
    }

    @Override
    public List<CollectionMonitorOverviewDTO> getMonitorOverview() {
        int ttlHours = 24; // 可从配置注入，这里先硬编码默认值
        return taskRepository.findMonitorOverview(ttlHours).stream()
                .map(o -> CollectionMonitorOverviewDTO.builder()
                        .taskType(o.getTaskType())
                        .totalCount(o.getTotalCount())
                        .recentSuccessCount(o.getRecentSuccessCount())
                        .recentExpiredCount(o.getRecentExpiredCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public CollectionMonitorBaselineDTO getMonitorBaseline() {
        return CollectionMonitorBaselineDTO.builder()
                .totalStocks(taskRepository.countAllStocks())
                .build();
    }

    private CollectionTaskDTO toDTO(CollectionTask task) {
        Object taskParamsObj = null;
        if (task.getTaskParams() != null) {
            try {
                taskParamsObj = objectMapper.readValue(task.getTaskParams(), Object.class);
            } catch (JsonProcessingException e) {
                log.warn("taskParams 反序列化失败, 返回原始字符串: {}", task.getTaskParams());
                taskParamsObj = task.getTaskParams();
            }
        }
        return CollectionTaskDTO.builder()
                .id(task.getId().getValue())
                .taskType(task.getTaskType())
                .taskParams(taskParamsObj)
                .status(task.getStatus())
                .dataSource(task.getDataSource())
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failCount(task.getFailCount())
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .mode(task.getMode())
                .sourcePriority(task.getSourcePriority())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
