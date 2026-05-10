package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskSchedule;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskScheduleId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskScheduleRepository;
import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.entity.CollectionTaskScheduleEntity;
import org.cwowhappy.securityanalyze.collection.infrastructure.persistence.mapper.CollectionTaskScheduleRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 采集任务调度仓库 JDBC 实现（Adapter）。
 */
@Repository
@RequiredArgsConstructor
public class JdbcCollectionTaskScheduleRepository implements CollectionTaskScheduleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CollectionTaskScheduleRowMapper rowMapper;

    @Override
    public Optional<CollectionTaskSchedule> findById(CollectionTaskScheduleId id) {
        String sql = "SELECT * FROM tb_collection_task_schedule WHERE id = :id";
        List<CollectionTaskScheduleEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<CollectionTaskSchedule> findAll() {
        String sql = "SELECT * FROM tb_collection_task_schedule ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CollectionTaskSchedule> findEnabled() {
        String sql = "SELECT * FROM tb_collection_task_schedule WHERE is_enabled = true ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public CollectionTaskScheduleId save(CollectionTaskSchedule schedule) {
        String sql = """
                INSERT INTO tb_collection_task_schedule (
                    id, name, task_type, task_params, data_source,
                    cron_expression, is_enabled, last_triggered_at, created_at
                ) VALUES (
                    :id, :name, :taskType, :taskParams::jsonb, :dataSource,
                    :cronExpression, :enabled, :lastTriggeredAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    task_type = EXCLUDED.task_type,
                    task_params = EXCLUDED.task_params,
                    data_source = EXCLUDED.data_source,
                    cron_expression = EXCLUDED.cron_expression,
                    is_enabled = EXCLUDED.is_enabled,
                    last_triggered_at = EXCLUDED.last_triggered_at
                """;
        CollectionTaskScheduleEntity entity = toEntity(schedule);
        SqlParameterSource params = new BeanPropertySqlParameterSource(entity);
        jdbcTemplate.update(sql, params);
        return schedule.getId();
    }

    @Override
    public void deleteById(CollectionTaskScheduleId id) {
        String sql = "DELETE FROM tb_collection_task_schedule WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id.getValue()));
    }

    private CollectionTaskSchedule toDomain(CollectionTaskScheduleEntity entity) {
        return CollectionTaskSchedule.builder()
                .id(CollectionTaskScheduleId.of(entity.getId()))
                .name(entity.getName())
                .taskType(entity.getTaskType())
                .taskParams(entity.getTaskParams())
                .dataSource(entity.getDataSource())
                .cronExpression(entity.getCronExpression())
                .enabled(entity.getEnabled())
                .lastTriggeredAt(entity.getLastTriggeredAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private CollectionTaskScheduleEntity toEntity(CollectionTaskSchedule schedule) {
        CollectionTaskScheduleEntity entity = new CollectionTaskScheduleEntity();
        entity.setId(schedule.getId().getValue());
        entity.setName(schedule.getName());
        entity.setTaskType(schedule.getTaskType());
        entity.setTaskParams(schedule.getTaskParams());
        entity.setDataSource(schedule.getDataSource());
        entity.setCronExpression(schedule.getCronExpression());
        entity.setEnabled(schedule.getEnabled());
        entity.setLastTriggeredAt(schedule.getLastTriggeredAt());
        entity.setCreatedAt(schedule.getCreatedAt());
        return entity;
    }
}
