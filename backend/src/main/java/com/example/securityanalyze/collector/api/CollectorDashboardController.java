package com.example.securityanalyze.collector.api;

import com.example.securityanalyze.collector.application.CollectorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collector/dashboard")
@RequiredArgsConstructor
public class CollectorDashboardController {

    private final CollectorDashboardService collectorDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<CollectorOverviewResponse> getOverview() {
        CollectorOverviewResponse response = collectorDashboardService.getOverview();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<CollectorTaskListResponse> listTasks(
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 100) {
            size = 100;
        }
        if (size < 1) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }

        CollectorTaskListResponse response = collectorDashboardService.listTasks(dataType, status, page, size);
        return ResponseEntity.ok(response);
    }
}
