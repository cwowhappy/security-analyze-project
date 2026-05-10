package org.cwowhappy.securityanalyze.collection.application.service;

import org.cwowhappy.securityanalyze.collection.application.dto.CollectionTaskScheduleDTO;
import org.cwowhappy.securityanalyze.collection.application.service.impl.CollectionTaskScheduleAppServiceImpl;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskSchedule;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskScheduleId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 采集任务调度应用服务单元测试（纯 Mockito，不启动 Spring 上下文）。
 */
@ExtendWith(MockitoExtension.class)
class CollectionTaskScheduleAppServiceTest {

    @Mock
    private CollectionTaskScheduleRepository scheduleRepository;

    @InjectMocks
    private CollectionTaskScheduleAppServiceImpl scheduleAppService;

    private CollectionTaskSchedule sampleSchedule;

    @BeforeEach
    void setUp() {
        sampleSchedule = CollectionTaskSchedule.builder()
                .id(CollectionTaskScheduleId.of("sch001"))
                .name("每日全量采集")
                .taskType("stock_full")
                .cronExpression("0 0 2 * * ?")
                .dataSource("akshare")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldReturnAllSchedules() {
        // Arrange
        when(scheduleRepository.findAll()).thenReturn(List.of(sampleSchedule));

        // Act
        List<CollectionTaskScheduleDTO> result = scheduleAppService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("sch001");
        assertThat(result.get(0).getName()).isEqualTo("每日全量采集");
        assertThat(result.get(0).getCronExpression()).isEqualTo("0 0 2 * * ?");
        verify(scheduleRepository, times(1)).findAll();
    }

    @Test
    void shouldCreateScheduleAndReturnDto() {
        // Arrange
        CollectionTaskScheduleDTO dto = CollectionTaskScheduleDTO.builder()
                .name("每小时增量采集")
                .taskType("stock_incremental")
                .cronExpression("0 0 * * * ?")
                .dataSource("akshare")
                .build();

        when(scheduleRepository.save(any(CollectionTaskSchedule.class))).thenReturn(CollectionTaskScheduleId.of("sch002"));

        // Act
        String id = scheduleAppService.create(dto);

        // Assert
        assertThat(id).isEqualTo("sch002");
        verify(scheduleRepository, times(1)).save(any(CollectionTaskSchedule.class));
    }

    @Test
    void shouldUpdateSchedule() {
        // Arrange
        CollectionTaskScheduleDTO dto = CollectionTaskScheduleDTO.builder()
                .name("每日全量采集（已更新）")
                .taskType("stock_full")
                .cronExpression("0 30 2 * * ?")
                .dataSource("akshare")
                .enabled(false)
                .build();

        when(scheduleRepository.findById(CollectionTaskScheduleId.of("sch001"))).thenReturn(Optional.of(sampleSchedule));
        when(scheduleRepository.save(any(CollectionTaskSchedule.class))).thenReturn(CollectionTaskScheduleId.of("sch001"));

        // Act
        scheduleAppService.update("sch001", dto);

        // Assert
        verify(scheduleRepository, times(1)).findById(CollectionTaskScheduleId.of("sch001"));
        verify(scheduleRepository, times(1)).save(any(CollectionTaskSchedule.class));
    }
}
