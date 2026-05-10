package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskScheduleDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskScheduleAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCollectionTaskScheduleRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 采集任务调度 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/collection/schedules")
@RequiredArgsConstructor
public class CollectionTaskScheduleController {

    private final CollectionTaskScheduleAppService scheduleAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionTaskScheduleDTO>>> listSchedules() {
        log.debug("查询采集任务调度列表");
        List<CollectionTaskScheduleDTO> list = scheduleAppService.findAll();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createSchedule(
            @Valid @RequestBody CreateCollectionTaskScheduleRequest request) {
        log.info("创建采集任务调度: name={}", request.getName());
        String taskParamsJson = null;
        if (request.getTaskParams() != null) {
            taskParamsJson = toJsonString(request.getTaskParams());
        }
        CollectionTaskScheduleDTO dto = CollectionTaskScheduleDTO.builder()
                .name(request.getName())
                .taskType(request.getTaskType())
                .cronExpression(request.getCronExpression())
                .taskParams(taskParamsJson)
                .dataSource(request.getDataSource())
                .build();
        String id = scheduleAppService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateSchedule(
            @PathVariable String id,
            @RequestBody CreateCollectionTaskScheduleRequest request) {
        log.info("更新采集任务调度: id={}", id);
        String taskParamsJson = null;
        if (request.getTaskParams() != null) {
            taskParamsJson = toJsonString(request.getTaskParams());
        }
        CollectionTaskScheduleDTO dto = CollectionTaskScheduleDTO.builder()
                .name(request.getName())
                .taskType(request.getTaskType())
                .cronExpression(request.getCronExpression())
                .taskParams(taskParamsJson)
                .dataSource(request.getDataSource())
                .build();
        scheduleAppService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success("更新成功", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable String id) {
        log.info("删除采集任务调度: id={}", id);
        scheduleAppService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("taskParams 序列化失败", e);
        }
    }
}
