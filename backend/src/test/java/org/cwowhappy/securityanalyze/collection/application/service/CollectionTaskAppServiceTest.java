package org.cwowhappy.securityanalyze.collection.application.service;

import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskDTO;
import org.cwowhappy.securityanalyze.collection.application.service.impl.CollectionTaskAppServiceImpl;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTask;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskRepository;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 采集任务应用服务单元测试（纯 Mockito，不启动 Spring 上下文）。
 */
@ExtendWith(MockitoExtension.class)
class CollectionTaskAppServiceTest {

    @Mock
    private CollectionTaskRepository taskRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CollectionTaskAppServiceImpl taskAppService;

    private CollectionTask sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = CollectionTask.builder()
                .id(CollectionTaskId.of("task001"))
                .taskType("stock_full")
                .status("success")
                .dataSource("akshare")
                .totalCount(5200)
                .successCount(5180)
                .failCount(20)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldReturnTaskWhenFoundById() {
        // Arrange
        when(taskRepository.findById(CollectionTaskId.of("task001"))).thenReturn(Optional.of(sampleTask));

        // Act
        Optional<CollectionTaskDTO> result = taskAppService.findById("task001");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("task001");
        assertThat(result.get().getTaskType()).isEqualTo("stock_full");
        assertThat(result.get().getStatus()).isEqualTo("success");
        verify(taskRepository, times(1)).findById(CollectionTaskId.of("task001"));
    }

    @Test
    void shouldReturnEmptyWhenTaskNotFound() {
        // Arrange
        when(taskRepository.findById(CollectionTaskId.of("notexist"))).thenReturn(Optional.empty());

        // Act
        Optional<CollectionTaskDTO> result = taskAppService.findById("notexist");

        // Assert
        assertThat(result).isEmpty();
        verify(taskRepository, times(1)).findById(CollectionTaskId.of("notexist"));
    }

    @Test
    void shouldReturnPageResultWhenFindByPage() {
        // Arrange
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(1);
        pageQuery.setSize(20);

        PageResult<CollectionTask> domainResult = PageResult.<CollectionTask>builder()
                .list(List.of(sampleTask))
                .total(1)
                .page(1)
                .size(20)
                .build();

        when(taskRepository.findByPage(pageQuery, "success", "stock_full")).thenReturn(domainResult);

        // Act
        PageResult<CollectionTaskDTO> result = taskAppService.findByPage(pageQuery, "success", "stock_full");

        // Assert
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getList().get(0).getTaskType()).isEqualTo("stock_full");
        verify(taskRepository, times(1)).findByPage(pageQuery, "success", "stock_full");
    }

    @Test
    void shouldCreateTaskAndReturnId() {
        // Arrange
        CollectionTaskDTO dto = CollectionTaskDTO.builder()
                .taskType("stock_daily")
                .dataSource("akshare")
                .build();

        when(taskRepository.save(any(CollectionTask.class))).thenReturn(CollectionTaskId.of("task002"));

        // Act
        String id = taskAppService.createTask(dto);

        // Assert
        assertThat(id).isEqualTo("task002");
        verify(taskRepository, times(1)).save(any(CollectionTask.class));
    }
}
