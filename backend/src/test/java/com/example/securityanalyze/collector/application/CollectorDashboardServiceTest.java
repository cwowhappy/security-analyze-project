package com.example.securityanalyze.collector.application;

import com.example.securityanalyze.collector.api.CollectorOverviewItem;
import com.example.securityanalyze.collector.api.CollectorOverviewResponse;
import com.example.securityanalyze.collector.api.CollectorTaskItem;
import com.example.securityanalyze.collector.api.CollectorTaskListResponse;
import com.example.securityanalyze.collector.infrastructure.CollectorDashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorDashboardServiceTest {

    @Mock
    private CollectorDashboardRepository collectorDashboardRepository;

    @InjectMocks
    private CollectorDashboardService collectorDashboardService;

    @Test
    void shouldGetOverview() {
        CollectorOverviewItem item = new CollectorOverviewItem();
        item.setDataType("company");
        item.setDataTypeLabel("公司基本信息");
        item.setTotalRows(5000);

        when(collectorDashboardRepository.findOverview()).thenReturn(List.of(item));

        CollectorOverviewResponse response = collectorDashboardService.getOverview();

        assertEquals(1, response.getData().size());
        assertEquals("company", response.getData().get(0).getDataType());
    }

    @Test
    void shouldListTasks() {
        CollectorTaskItem item = new CollectorTaskItem();
        item.setId(1L);
        item.setTaskName("公司信息采集");
        item.setStatus("SUCCESS");
        item.setStartedAt(LocalDateTime.now());

        when(collectorDashboardRepository.findTasks("company", "SUCCESS", 0, 20))
                .thenReturn(List.of(item));
        when(collectorDashboardRepository.countTasks("company", "SUCCESS")).thenReturn(1L);

        CollectorTaskListResponse response = collectorDashboardService.listTasks("company", "SUCCESS", 0, 20);

        assertEquals(1, response.getData().size());
        assertEquals(1L, response.getTotal());
        assertEquals(0, response.getPage());
    }

    @Test
    void shouldListTasksWithNullFilters() {
        when(collectorDashboardRepository.findTasks(null, null, 0, 20)).thenReturn(List.of());
        when(collectorDashboardRepository.countTasks(null, null)).thenReturn(0L);

        CollectorTaskListResponse response = collectorDashboardService.listTasks(null, null, 0, 20);

        assertTrue(response.getData().isEmpty());
    }
}
