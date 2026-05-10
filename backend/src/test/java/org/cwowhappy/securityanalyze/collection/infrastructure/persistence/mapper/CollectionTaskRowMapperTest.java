package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity.CollectionTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CollectionTaskRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CollectionTaskRowMapperTest {

    private final CollectionTaskRowMapper mapper = new CollectionTaskRowMapper();

    @Test
    void shouldMapResultSetToCollectionTaskEntity() throws Exception {
        // 准备 Mock 数据
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 10, 14, 30, 0);
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 10, 14, 35, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 10, 14, 40, 0);

        when(rs.getString("id")).thenReturn("task789");
        when(rs.getString("task_type")).thenReturn("stock_daily");
        when(rs.getString("task_params")).thenReturn("{\"start_date\":\"2026-05-01\",\"end_date\":\"2026-05-10\"}");
        when(rs.getString("status")).thenReturn("completed");
        when(rs.getString("data_source")).thenReturn("eastmoney");
        when(rs.getObject("total_count", Integer.class)).thenReturn(5000);
        when(rs.getObject("success_count", Integer.class)).thenReturn(4980);
        when(rs.getObject("fail_count", Integer.class)).thenReturn(20);
        when(rs.getTimestamp("scheduled_at")).thenReturn(Timestamp.valueOf(scheduledAt));
        when(rs.getString("error_message")).thenReturn("部分请求超时");
        when(rs.getTimestamp("started_at")).thenReturn(Timestamp.valueOf(startedAt));
        when(rs.getTimestamp("completed_at")).thenReturn(Timestamp.valueOf(completedAt));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));

        // 执行映射
        CollectionTaskEntity entity = mapper.mapRow(rs, 1);

        // 验证所有字段
        assertThat(entity.getId()).isEqualTo("task789");
        assertThat(entity.getTaskType()).isEqualTo("stock_daily");
        assertThat(entity.getTaskParams()).isEqualTo("{\"start_date\":\"2026-05-01\",\"end_date\":\"2026-05-10\"}");
        assertThat(entity.getStatus()).isEqualTo("completed");
        assertThat(entity.getDataSource()).isEqualTo("eastmoney");
        assertThat(entity.getTotalCount()).isEqualTo(5000);
        assertThat(entity.getSuccessCount()).isEqualTo(4980);
        assertThat(entity.getFailCount()).isEqualTo(20);
        assertThat(entity.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(entity.getErrorMessage()).isEqualTo("部分请求超时");
        assertThat(entity.getStartedAt()).isEqualTo(startedAt);
        assertThat(entity.getCompletedAt()).isEqualTo(completedAt);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }
}
