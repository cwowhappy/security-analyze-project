package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCollectionTaskRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



/**
 * 采集任务 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/collection/tasks")
@RequiredArgsConstructor
public class CollectionTaskController {

    private final CollectionTaskAppService taskAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<CollectionTaskDTO>>> listTasks(
            PageQuery pageQuery,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        log.debug("查询采集任务列表: page={}, size={}, status={}, taskType={}",
                pageQuery.getPage(), pageQuery.getSize(), status, taskType);
        PageResult<CollectionTaskDTO> result = taskAppService.findByPage(pageQuery, status, taskType);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CollectionTaskDTO>> getTask(@PathVariable String id) {
        log.debug("查询采集任务详情: id={}", id);
        return taskAppService.findById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "采集任务不存在: " + id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionTaskDTO>> createTask(@Valid @RequestBody CreateCollectionTaskRequest request) {
        log.info("创建采集任务: taskType={}", request.getTaskType());
        CollectionTaskDTO dto = CollectionTaskDTO.builder()
                .taskType(request.getTaskType())
                .taskParams(request.getTaskParams())
                .dataSource(request.getDataSource())
                .build();
        String id = taskAppService.createTask(dto);
        CollectionTaskDTO created = taskAppService.findById(id)
                .orElseThrow(() -> new IllegalStateException("创建任务后无法查询: " + id));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", created));
    }
}
