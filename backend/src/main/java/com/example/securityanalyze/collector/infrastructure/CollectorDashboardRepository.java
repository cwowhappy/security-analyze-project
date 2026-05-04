package com.example.securityanalyze.collector.infrastructure;

import com.example.securityanalyze.collector.api.CollectorOverviewItem;
import com.example.securityanalyze.collector.api.CollectorTaskItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CollectorDashboardRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<CollectorOverviewItem> OVERVIEW_ROW_MAPPER = new RowMapper<>() {
        @Override
        public CollectorOverviewItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            CollectorOverviewItem item = new CollectorOverviewItem();
            item.setDataType(rs.getString("data_type"));
            item.setDataTypeLabel(toLabel(rs.getString("data_type")));
            item.setTotalRows(rs.getInt("total_rows"));

            Timestamp lastUpdated = rs.getTimestamp("last_updated_at");
            if (lastUpdated != null) {
                item.setLastUpdatedAt(lastUpdated.toLocalDateTime());
            }

            item.setLastTaskStatus(rs.getString("last_task_status"));
            item.setLastTaskDurationSeconds(rs.getObject("last_task_duration_seconds", Long.class));
            return item;
        }
    };

    private static final RowMapper<CollectorTaskItem> TASK_ROW_MAPPER = new RowMapper<>() {
        @Override
        public CollectorTaskItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            CollectorTaskItem item = new CollectorTaskItem();
            item.setId(rs.getLong("id"));
            item.setTaskName(rs.getString("task_name"));
            item.setTaskType(rs.getString("task_type"));

            Timestamp startedAt = rs.getTimestamp("started_at");
            if (startedAt != null) {
                item.setStartedAt(startedAt.toLocalDateTime());
            }

            Timestamp endedAt = rs.getTimestamp("ended_at");
            if (endedAt != null) {
                item.setEndedAt(endedAt.toLocalDateTime());
            }

            item.setStatus(rs.getString("status"));
            item.setRowsAffected(rs.getInt("rows_affected"));
            item.setDurationSeconds(rs.getObject("duration_seconds", Long.class));
            return item;
        }
    };

    public List<CollectorOverviewItem> findOverview() {
        log.debug("查询采集概览");
        String sql = """
                WITH table_stats AS (
                    SELECT 'company' AS data_type, COUNT(*) AS total_rows, MAX(updated_at) AS last_updated_at FROM company
                    UNION ALL
                    SELECT 'security', COUNT(*), MAX(updated_at) FROM company_security
                    UNION ALL
                    SELECT 'finance_report', COUNT(*), MAX(updated_at) FROM financial_report
                ),
                latest_tasks AS (
                    SELECT DISTINCT ON (task_type) task_type, status, started_at, ended_at,
                        EXTRACT(EPOCH FROM (ended_at - started_at))::bigint AS duration_seconds
                    FROM collector_task_log
                    ORDER BY task_type, started_at DESC
                )
                SELECT
                    s.data_type,
                    s.total_rows,
                    s.last_updated_at,
                    t.status AS last_task_status,
                    t.duration_seconds AS last_task_duration_seconds
                FROM table_stats s
                LEFT JOIN latest_tasks t ON t.task_type = s.data_type
                ORDER BY s.data_type
                """;
        return jdbcTemplate.query(sql, OVERVIEW_ROW_MAPPER);
    }

    public List<CollectorTaskItem> findTasks(String dataType, String status, int offset, int limit) {
        log.debug("查询采集任务, dataType={}, status={}, offset={}, limit={}", dataType, status, offset, limit);
        StringBuilder sql = new StringBuilder("""
                SELECT id, task_name, task_type, started_at, ended_at, status, rows_affected,
                    EXTRACT(EPOCH FROM (ended_at - started_at))::bigint AS duration_seconds
                FROM collector_task_log
                WHERE started_at >= NOW() - INTERVAL '7 days'
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        if (dataType != null && !dataType.isBlank()) {
            sql.append(" AND task_type = :dataType");
            params.addValue("dataType", dataType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY started_at DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, TASK_ROW_MAPPER);
    }

    public long countTasks(String dataType, String status) {
        log.debug("统计采集任务数量, dataType={}, status={}", dataType, status);
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM collector_task_log
                WHERE started_at >= NOW() - INTERVAL '7 days'
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (dataType != null && !dataType.isBlank()) {
            sql.append(" AND task_type = :dataType");
            params.addValue("dataType", dataType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    private static String toLabel(String dataType) {
        return switch (dataType) {
            case "company" -> "公司基本信息";
            case "security" -> "上市证券信息";
            case "finance_report" -> "财务报告";
            default -> dataType;
        };
    }
}
