package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTask;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskRepository;
import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity.CollectionTaskEntity;
import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.mapper.CollectionTaskRowMapper;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 采集任务仓库 JDBC 实现（Adapter）。
 */
@Repository
@RequiredArgsConstructor
public class JdbcCollectionTaskRepository implements CollectionTaskRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CollectionTaskRowMapper rowMapper;

    @Override
    public Optional<CollectionTask> findById(CollectionTaskId id) {
        String sql = "SELECT * FROM tb_collection_task WHERE id = :id";
        List<CollectionTaskEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public PageResult<CollectionTask> findByPage(PageQuery pageQuery, String status, String taskType) {
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM tb_collection_task WHERE 1=1");
        StringBuilder querySql = new StringBuilder("SELECT * FROM tb_collection_task WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(status)) {
            countSql.append(" AND status = :status");
            querySql.append(" AND status = :status");
            params.addValue("status", status);
        }
        if (StringUtils.hasText(taskType)) {
            countSql.append(" AND task_type = :taskType");
            querySql.append(" AND task_type = :taskType");
            params.addValue("taskType", taskType);
        }

        Long total = jdbcTemplate.queryForObject(countSql.toString(), params, Long.class);
        if (total == null) {
            total = 0L;
        }

        querySql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", pageQuery.getSize());
        params.addValue("offset", (pageQuery.getPage() - 1) * pageQuery.getSize());

        List<CollectionTask> list = jdbcTemplate.query(querySql.toString(), params, rowMapper)
                .stream().map(this::toDomain).toList();

        return PageResult.<CollectionTask>builder()
                .list(list)
                .total(total)
                .page(pageQuery.getPage())
                .size(pageQuery.getSize())
                .build();
    }

    @Override
    public List<CollectionTask> findByStatus(String status) {
        String sql = "SELECT * FROM tb_collection_task WHERE status = :status ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("status", status), rowMapper)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public CollectionTaskId save(CollectionTask task) {
        String sql = """
                INSERT INTO tb_collection_task (
                    id, task_type, task_params, status, data_source,
                    total_count, success_count, fail_count, scheduled_at,
                    error_message, started_at, completed_at, created_at
                ) VALUES (
                    :id, :taskType, :taskParams::jsonb, :status, :dataSource,
                    :totalCount, :successCount, :failCount, :scheduledAt,
                    :errorMessage, :startedAt, :completedAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    task_type = EXCLUDED.task_type,
                    task_params = EXCLUDED.task_params,
                    status = EXCLUDED.status,
                    data_source = EXCLUDED.data_source,
                    total_count = EXCLUDED.total_count,
                    success_count = EXCLUDED.success_count,
                    fail_count = EXCLUDED.fail_count,
                    scheduled_at = EXCLUDED.scheduled_at,
                    error_message = EXCLUDED.error_message,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at
                """;
        CollectionTaskEntity entity = toEntity(task);
        SqlParameterSource params = new BeanPropertySqlParameterSource(entity);
        jdbcTemplate.update(sql, params);
        return task.getId();
    }

    private CollectionTask toDomain(CollectionTaskEntity entity) {
        return CollectionTask.builder()
                .id(CollectionTaskId.of(entity.getId()))
                .taskType(entity.getTaskType())
                .taskParams(entity.getTaskParams())
                .status(entity.getStatus())
                .dataSource(entity.getDataSource())
                .totalCount(entity.getTotalCount())
                .successCount(entity.getSuccessCount())
                .failCount(entity.getFailCount())
                .scheduledAt(entity.getScheduledAt())
                .errorMessage(entity.getErrorMessage())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private CollectionTaskEntity toEntity(CollectionTask task) {
        CollectionTaskEntity entity = new CollectionTaskEntity();
        entity.setId(task.getId().getValue());
        entity.setTaskType(task.getTaskType());
        entity.setTaskParams(task.getTaskParams());
        entity.setStatus(task.getStatus());
        entity.setDataSource(task.getDataSource());
        entity.setTotalCount(task.getTotalCount());
        entity.setSuccessCount(task.getSuccessCount());
        entity.setFailCount(task.getFailCount());
        entity.setScheduledAt(task.getScheduledAt());
        entity.setErrorMessage(task.getErrorMessage());
        entity.setStartedAt(task.getStartedAt());
        entity.setCompletedAt(task.getCompletedAt());
        entity.setCreatedAt(task.getCreatedAt());
        return entity;
    }
}
