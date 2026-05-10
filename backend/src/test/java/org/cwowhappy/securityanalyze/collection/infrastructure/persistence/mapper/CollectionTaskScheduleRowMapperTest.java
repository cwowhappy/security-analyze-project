package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity.CollectionTaskScheduleEntity;
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
 * CollectionTaskScheduleRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CollectionTaskScheduleRowMapperTest {

    private final CollectionTaskScheduleRowMapper mapper = new CollectionTaskScheduleRowMapper();

    @Test
    void shouldMapResultSetToCollectionTaskScheduleEntity() throws Exception {
        // 准备 Mock 数据
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);
        LocalDateTime lastTriggeredAt = LocalDateTime.of(2026, 5, 10, 9, 0, 0);

        when(rs.getString("id")).thenReturn("sched101");
        when(rs.getString("name")).thenReturn("每日股票行情采集");
        when(rs.getString("task_type")).thenReturn("stock_daily");
        when(rs.getString("task_params")).thenReturn("{\"trade_date\":\"latest\"}");
        when(rs.getString("data_source")).thenReturn("eastmoney");
        when(rs.getString("cron_expression")).thenReturn("0 30 9 * * ?");
        when(rs.getObject("is_enabled", Boolean.class)).thenReturn(true);
        when(rs.getTimestamp("last_triggered_at")).thenReturn(Timestamp.valueOf(lastTriggeredAt));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));

        // 执行映射
        CollectionTaskScheduleEntity entity = mapper.mapRow(rs, 1);

        // 验证所有字段
        assertThat(entity.getId()).isEqualTo("sched101");
        assertThat(entity.getName()).isEqualTo("每日股票行情采集");
        assertThat(entity.getTaskType()).isEqualTo("stock_daily");
        assertThat(entity.getTaskParams()).isEqualTo("{\"trade_date\":\"latest\"}");
        assertThat(entity.getDataSource()).isEqualTo("eastmoney");
        assertThat(entity.getCronExpression()).isEqualTo("0 30 9 * * ?");
        assertThat(entity.getEnabled()).isTrue();
        assertThat(entity.getLastTriggeredAt()).isEqualTo(lastTriggeredAt);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }
}
