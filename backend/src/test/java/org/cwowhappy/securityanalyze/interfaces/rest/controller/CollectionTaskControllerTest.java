package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCollectionTaskRequest;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CollectionTaskController Web 层测试（@WebMvcTest，只加载 Controller 层）。
 */
@WebMvcTest(CollectionTaskController.class)
class CollectionTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectionTaskAppService taskAppService;

    @Test
    void shouldReturnTasksWhenListTasks() throws Exception {
        // Arrange
        CollectionTaskDTO dto = CollectionTaskDTO.builder()
                .id("task001")
                .taskType("stock_full")
                .status("success")
                .dataSource("akshare")
                .build();

        PageResult<CollectionTaskDTO> pageResult = PageResult.<CollectionTaskDTO>builder()
                .list(List.of(dto))
                .total(1)
                .page(1)
                .size(20)
                .build();

        when(taskAppService.findByPage(any(PageQuery.class), eq("success"), eq("stock_full"))).thenReturn(pageResult);

        // Act & Assert
        mockMvc.perform(get("/api/v1/collection/tasks")
                        .param("status", "success")
                        .param("taskType", "stock_full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list[0].id").value("task001"))
                .andExpect(jsonPath("$.data.list[0].taskType").value("stock_full"));
    }

    @Test
    void shouldReturnTaskWhenFoundById() throws Exception {
        // Arrange
        CollectionTaskDTO dto = CollectionTaskDTO.builder()
                .id("task001")
                .taskType("stock_full")
                .status("success")
                .dataSource("akshare")
                .build();

        when(taskAppService.findById("task001")).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/collection/tasks/task001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("task001"))
                .andExpect(jsonPath("$.data.taskType").value("stock_full"));
    }

    @Test
    void shouldCreateTaskWhenRequestValid() throws Exception {
        // Arrange
        CreateCollectionTaskRequest request = new CreateCollectionTaskRequest();
        request.setTaskType("stock_daily");
        request.setDataSource("akshare");

        CollectionTaskDTO createdDto = CollectionTaskDTO.builder()
                .id("task002")
                .taskType("stock_daily")
                .status("pending")
                .dataSource("akshare")
                .build();

        when(taskAppService.createTask(any(CollectionTaskDTO.class))).thenReturn("task002");
        when(taskAppService.findById("task002")).thenReturn(Optional.of(createdDto));

        // Act & Assert
        mockMvc.perform(post("/api/v1/collection/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("task002"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }
}
