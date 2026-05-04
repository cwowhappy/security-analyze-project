package com.example.securityanalyze.collector.api;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.collector.application.CollectorDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/collector/dashboard")
@RequiredArgsConstructor
public class CollectorDashboardController {

    private final CollectorDashboardService collectorDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<CollectorOverviewResponse> getOverview() {
        log.debug("查询采集概览");
        CollectorOverviewResponse response = collectorDashboardService.getOverview();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<CollectorTaskListResponse> listTasks(
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int[] normalized = PageUtils.normalize(page, size);
        page = normalized[0];
        size = normalized[1];

        log.debug("查询采集任务列表, dataType={}, status={}, page={}, size={}", dataType, status, page, size);
        CollectorTaskListResponse response = collectorDashboardService.listTasks(dataType, status, page, size);
        return ResponseEntity.ok(response);
    }
}
