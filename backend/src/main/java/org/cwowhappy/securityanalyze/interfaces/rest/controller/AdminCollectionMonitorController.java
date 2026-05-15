package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorBaselineDTO;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorOverviewDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采集监控 Admin REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/collection/monitor")
@RequiredArgsConstructor
public class AdminCollectionMonitorController {

    private final CollectionTaskAppService taskAppService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<CollectionMonitorOverviewDTO>>> getOverview() {
        log.debug("查询采集监控覆盖度概览");
        List<CollectionMonitorOverviewDTO> result = taskAppService.getMonitorOverview();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/baseline")
    public ResponseEntity<ApiResponse<CollectionMonitorBaselineDTO>> getBaseline() {
        log.debug("查询数据基线");
        CollectionMonitorBaselineDTO result = taskAppService.getMonitorBaseline();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
