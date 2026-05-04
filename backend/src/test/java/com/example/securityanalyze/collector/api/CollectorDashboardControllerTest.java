package com.example.securityanalyze.collector.api;

import com.example.securityanalyze.collector.application.CollectorDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class CollectorDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectorDashboardService collectorDashboardService;

    @Test
    void shouldGetOverview() throws Exception {
        CollectorOverviewItem item = new CollectorOverviewItem();
        item.setDataType("company");
        item.setDataTypeLabel("公司基本信息");
        item.setTotalRows(5000);

        CollectorOverviewResponse response = new CollectorOverviewResponse();
        response.setData(List.of(item));

        when(collectorDashboardService.getOverview()).thenReturn(response);

        mockMvc.perform(get("/api/collector/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dataType").value("company"));
    }

    @Test
    void shouldListTasks() throws Exception {
        CollectorTaskItem item = new CollectorTaskItem();
        item.setId(1L);
        item.setTaskName("公司信息采集");
        item.setStatus("SUCCESS");
        item.setStartedAt(LocalDateTime.now());

        CollectorTaskListResponse response = new CollectorTaskListResponse();
        response.setData(List.of(item));
        response.setTotal(1L);
        response.setPage(0);
        response.setSize(20);

        when(collectorDashboardService.listTasks(null, null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/collector/dashboard/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskName").value("公司信息采集"));
    }
}
