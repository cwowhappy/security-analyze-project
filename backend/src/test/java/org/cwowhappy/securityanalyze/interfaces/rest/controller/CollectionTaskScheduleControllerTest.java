package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskScheduleDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskScheduleAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCollectionTaskScheduleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CollectionTaskScheduleController Web 层测试（@WebMvcTest，只加载 Controller 层）。
 */
@WebMvcTest(CollectionTaskScheduleController.class)
class CollectionTaskScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectionTaskScheduleAppService scheduleAppService;

    @Test
    void shouldReturnSchedulesWhenListSchedules() throws Exception {
        // Arrange
        CollectionTaskScheduleDTO dto = CollectionTaskScheduleDTO.builder()
                .id("sch001")
                .name("每日全量采集")
                .taskType("stock_full")
                .cronExpression("0 0 2 * * ?")
                .enabled(true)
                .build();

        when(scheduleAppService.findAll()).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/collection/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sch001"))
                .andExpect(jsonPath("$.data[0].name").value("每日全量采集"));
    }

    @Test
    void shouldCreateScheduleWhenRequestValid() throws Exception {
        // Arrange
        CreateCollectionTaskScheduleRequest request = new CreateCollectionTaskScheduleRequest();
        request.setName("每小时增量采集");
        request.setTaskType("stock_incremental");
        request.setCronExpression("0 0 * * * ?");
        request.setDataSource("akshare");

        when(scheduleAppService.create(any(CollectionTaskScheduleDTO.class))).thenReturn("sch002");

        // Act & Assert
        mockMvc.perform(post("/api/collection/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("sch002"));
    }

    @Test
    void shouldUpdateScheduleWhenRequestValid() throws Exception {
        // Arrange
        CreateCollectionTaskScheduleRequest request = new CreateCollectionTaskScheduleRequest();
        request.setName("每日全量采集（已更新）");
        request.setTaskType("stock_full");
        request.setCronExpression("0 30 2 * * ?");
        request.setDataSource("akshare");

        // Act & Assert
        mockMvc.perform(put("/api/collection/schedules/sch001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }
}
