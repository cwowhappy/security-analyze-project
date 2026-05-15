package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorBaselineDTO;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorOverviewDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCollectionMonitorController.class)
class AdminCollectionMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionTaskAppService taskAppService;

    @Test
    void shouldReturnOverview() throws Exception {
        when(taskAppService.getMonitorOverview()).thenReturn(List.of(
                CollectionMonitorOverviewDTO.builder()
                        .taskType("stock_basic")
                        .totalCount(5200L)
                        .recentSuccessCount(5100L)
                        .recentExpiredCount(80L)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/admin/collection/monitor/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskType").value("stock_basic"))
                .andExpect(jsonPath("$.data[0].totalCount").value(5200))
                .andExpect(jsonPath("$.data[0].recentSuccessCount").value(5100))
                .andExpect(jsonPath("$.data[0].recentExpiredCount").value(80));
    }

    @Test
    void shouldReturnBaseline() throws Exception {
        when(taskAppService.getMonitorBaseline()).thenReturn(
                CollectionMonitorBaselineDTO.builder().totalStocks(5200L).build()
        );

        mockMvc.perform(get("/api/v1/admin/collection/monitor/baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalStocks").value(5200));
    }
}
