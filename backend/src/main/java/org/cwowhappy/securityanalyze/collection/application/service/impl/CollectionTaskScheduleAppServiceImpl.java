package org.cwowhappy.securityanalyze.collection.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskScheduleDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskScheduleAppService;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskSchedule;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskScheduleId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskScheduleRepository;
import org.cwowhappy.securityanalyze.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 采集任务调度应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionTaskScheduleAppServiceImpl implements CollectionTaskScheduleAppService {

    private final CollectionTaskScheduleRepository scheduleRepository;

    @Override
    public List<CollectionTaskScheduleDTO> findAll() {
        log.debug("查询所有采集任务调度");
        return scheduleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String create(CollectionTaskScheduleDTO dto) {
        log.info("创建采集任务调度: name={}, taskType={}", dto.getName(), dto.getTaskType());
        CollectionTaskSchedule schedule = CollectionTaskSchedule.builder()
                .id(CollectionTaskScheduleId.generate())
                .name(dto.getName())
                .taskType(dto.getTaskType())
                .taskParams(dto.getTaskParams())
                .dataSource(dto.getDataSource())
                .cronExpression(dto.getCronExpression())
                .enabled(Boolean.TRUE)
                .createdAt(LocalDateTime.now())
                .build();
        CollectionTaskScheduleId id = scheduleRepository.save(schedule);
        log.info("采集任务调度创建成功: id={}", id);
        return id.getValue();
    }

    @Override
    @Transactional
    public void update(String id, CollectionTaskScheduleDTO dto) {
        log.info("更新采集任务调度: id={}", id);
        CollectionTaskSchedule existing = scheduleRepository.findById(CollectionTaskScheduleId.of(id))
                .orElseThrow(() -> new NotFoundException("CollectionTaskSchedule", id));

        CollectionTaskSchedule updated = CollectionTaskSchedule.builder()
                .id(existing.getId())
                .name(dto.getName() != null ? dto.getName() : existing.getName())
                .taskType(dto.getTaskType() != null ? dto.getTaskType() : existing.getTaskType())
                .taskParams(dto.getTaskParams() != null ? dto.getTaskParams() : existing.getTaskParams())
                .dataSource(dto.getDataSource() != null ? dto.getDataSource() : existing.getDataSource())
                .cronExpression(dto.getCronExpression() != null ? dto.getCronExpression() : existing.getCronExpression())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : existing.getEnabled())
                .lastTriggeredAt(existing.getLastTriggeredAt())
                .createdAt(existing.getCreatedAt())
                .build();

        scheduleRepository.save(updated);
        log.info("采集任务调度更新成功: id={}", id);
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("删除采集任务调度: id={}", id);
        scheduleRepository.deleteById(CollectionTaskScheduleId.of(id));
        log.info("采集任务调度删除成功: id={}", id);
    }

    private CollectionTaskScheduleDTO toDTO(CollectionTaskSchedule schedule) {
        return CollectionTaskScheduleDTO.builder()
                .id(schedule.getId().getValue())
                .name(schedule.getName())
                .taskType(schedule.getTaskType())
                .taskParams(schedule.getTaskParams())
                .dataSource(schedule.getDataSource())
                .cronExpression(schedule.getCronExpression())
                .enabled(schedule.getEnabled())
                .lastTriggeredAt(schedule.getLastTriggeredAt())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}
