package com.example.securityanalyze.collector.application;

import com.example.securityanalyze.collector.api.CollectorOverviewItem;
import com.example.securityanalyze.collector.api.CollectorOverviewResponse;
import com.example.securityanalyze.collector.api.CollectorTaskItem;
import com.example.securityanalyze.collector.api.CollectorTaskListResponse;
import com.example.securityanalyze.collector.domain.CollectorDashboardRepository;
import com.example.securityanalyze.collector.domain.CollectorOverview;
import com.example.securityanalyze.collector.domain.CollectorTask;
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
        List<CollectorOverview> items = collectorDashboardRepository.findOverview();
        CollectorOverviewResponse response = new CollectorOverviewResponse();
        response.setData(items.stream().map(this::toOverviewItem).toList());
        return response;
    }

    public CollectorTaskListResponse listTasks(String dataType, String status, int page, int size) {
        log.debug("查询采集任务列表, dataType={}, status={}, page={}, size={}", dataType, status, page, size);
        int offset = page * size;

        List<CollectorTask> items = collectorDashboardRepository.findTasks(dataType, status, offset, size);
        long total = collectorDashboardRepository.countTasks(dataType, status);

        CollectorTaskListResponse response = new CollectorTaskListResponse();
        response.setData(items.stream().map(this::toTaskItem).toList());
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private CollectorOverviewItem toOverviewItem(CollectorOverview overview) {
        CollectorOverviewItem item = new CollectorOverviewItem();
        item.setDataType(overview.getDataType());
        item.setDataTypeLabel(overview.getDataTypeLabel());
        item.setTotalRows(overview.getTotalRows());
        item.setLastUpdatedAt(overview.getLastUpdatedAt());
        item.setLastTaskStatus(overview.getLastTaskStatus());
        item.setLastTaskDurationSeconds(overview.getLastTaskDurationSeconds());
        return item;
    }

    private CollectorTaskItem toTaskItem(CollectorTask task) {
        CollectorTaskItem item = new CollectorTaskItem();
        item.setId(task.getId());
        item.setTaskName(task.getTaskName());
        item.setTaskType(task.getTaskType());
        item.setStartedAt(task.getStartedAt());
        item.setEndedAt(task.getEndedAt());
        item.setStatus(task.getStatus());
        item.setRowsAffected(task.getRowsAffected());
        item.setDurationSeconds(task.getDurationSeconds());
        return item;
    }
}
