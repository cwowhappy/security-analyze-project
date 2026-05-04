package com.example.securityanalyze.collector.application;

import com.example.securityanalyze.collector.api.CollectorOverviewItem;
import com.example.securityanalyze.collector.api.CollectorOverviewResponse;
import com.example.securityanalyze.collector.api.CollectorTaskItem;
import com.example.securityanalyze.collector.api.CollectorTaskListResponse;
import com.example.securityanalyze.collector.infrastructure.CollectorDashboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorDashboardService {

    private final CollectorDashboardRepository collectorDashboardRepository;

    public CollectorOverviewResponse getOverview() {
        log.debug("查询采集概览");
        List<CollectorOverviewItem> items = collectorDashboardRepository.findOverview();
        CollectorOverviewResponse response = new CollectorOverviewResponse();
        response.setData(items);
        return response;
    }

    public CollectorTaskListResponse listTasks(String dataType, String status, int page, int size) {
        log.debug("查询采集任务列表, dataType={}, status={}, page={}, size={}", dataType, status, page, size);
        int offset = page * size;

        List<CollectorTaskItem> items = collectorDashboardRepository.findTasks(dataType, status, offset, size);
        long total = collectorDashboardRepository.countTasks(dataType, status);

        CollectorTaskListResponse response = new CollectorTaskListResponse();
        response.setData(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
