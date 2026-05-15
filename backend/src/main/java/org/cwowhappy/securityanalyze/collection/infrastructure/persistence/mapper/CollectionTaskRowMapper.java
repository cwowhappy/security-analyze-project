package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity.CollectionTaskEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 采集任务 JDBC RowMapper。
 */
@Component
public class CollectionTaskRowMapper implements RowMapper<CollectionTaskEntity> {

    @Override
    public CollectionTaskEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        CollectionTaskEntity entity = new CollectionTaskEntity();
        entity.setId(rs.getString("id"));
        entity.setTaskType(rs.getString("task_type"));
        entity.setTaskParams(rs.getString("task_params"));
        entity.setStatus(rs.getString("status"));
        entity.setDataSource(rs.getString("data_source"));
        entity.setTotalCount(rs.getObject("total_count", Integer.class));
        entity.setSuccessCount(rs.getObject("success_count", Integer.class));
        entity.setFailCount(rs.getObject("fail_count", Integer.class));
        entity.setErrorMessage(rs.getString("error_message"));
        entity.setStartedAt(toLocalDateTime(rs.getTimestamp("started_at")));
        entity.setCompletedAt(toLocalDateTime(rs.getTimestamp("completed_at")));
        entity.setMode(rs.getString("mode"));
        entity.setSourcePriority(rs.getString("source_priority"));
        entity.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return entity;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
