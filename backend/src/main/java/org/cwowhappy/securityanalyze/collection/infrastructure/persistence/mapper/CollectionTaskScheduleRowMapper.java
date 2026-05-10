package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity.CollectionTaskScheduleEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 采集任务调度 JDBC RowMapper。
 */
@Component
public class CollectionTaskScheduleRowMapper implements RowMapper<CollectionTaskScheduleEntity> {

    @Override
    public CollectionTaskScheduleEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        CollectionTaskScheduleEntity entity = new CollectionTaskScheduleEntity();
        entity.setId(rs.getString("id"));
        entity.setName(rs.getString("name"));
        entity.setTaskType(rs.getString("task_type"));
        entity.setTaskParams(rs.getString("task_params"));
        entity.setDataSource(rs.getString("data_source"));
        entity.setCronExpression(rs.getString("cron_expression"));
        entity.setEnabled(rs.getObject("is_enabled", Boolean.class));
        entity.setLastTriggeredAt(toLocalDateTime(rs.getTimestamp("last_triggered_at")));
        entity.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return entity;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
