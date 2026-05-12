package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.cwowhappy.securityanalyze.user.domain.repository.LoginLogRepository;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.LoginLogEntity;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper.LoginLogRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志仓库 JDBC 实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcLoginLogRepository implements LoginLogRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final LoginLogRowMapper rowMapper;

    @Override
    public void save(LoginLog loginLog) {
        String sql = """
                INSERT INTO tb_login_log (
                    user_id, username, action, ip, user_agent, details, created_at
                ) VALUES (
                    :userId, :username, :action, :ip, :userAgent, :details, :createdAt
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", loginLog.getUserId());
        params.addValue("username", loginLog.getUsername());
        params.addValue("action", loginLog.getAction());
        params.addValue("ip", loginLog.getIp());
        params.addValue("userAgent", loginLog.getUserAgent());
        params.addValue("details", loginLog.getDetails());
        params.addValue("createdAt", loginLog.getCreatedAt());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public List<LoginLog> findByConditions(String userId, String action, LocalDateTime startDate,
                                            LocalDateTime endDate, int page, int size) {
        QueryBuilder qb = buildQuery(userId, action, startDate, endDate);
        String sql = qb.sql + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        qb.params.addValue("limit", size);
        qb.params.addValue("offset", (page - 1) * size);
        List<LoginLogEntity> results = jdbcTemplate.query(sql, qb.params, rowMapper);
        return results.stream().map(this::toDomain).toList();
    }

    @Override
    public long countByConditions(String userId, String action, LocalDateTime startDate, LocalDateTime endDate) {
        QueryBuilder qb = buildQuery(userId, action, startDate, endDate);
        String sql = "SELECT COUNT(*) FROM tb_login_log WHERE 1=1" + qb.whereClause;
        Long count = jdbcTemplate.queryForObject(sql, qb.params, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public List<LoginLog> findAllByConditions(String userId, String action, LocalDateTime startDate,
                                               LocalDateTime endDate) {
        QueryBuilder qb = buildQuery(userId, action, startDate, endDate);
        String sql = qb.sql + " ORDER BY created_at DESC";
        List<LoginLogEntity> results = jdbcTemplate.query(sql, qb.params, rowMapper);
        return results.stream().map(this::toDomain).toList();
    }

    private QueryBuilder buildQuery(String userId, String action, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder where = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (userId != null && !userId.isBlank()) {
            where.append(" AND user_id = :userId");
            params.addValue("userId", userId);
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND action = :action");
            params.addValue("action", action);
        }
        if (startDate != null) {
            where.append(" AND created_at >= :startDate");
            params.addValue("startDate", startDate);
        }
        if (endDate != null) {
            where.append(" AND created_at <= :endDate");
            params.addValue("endDate", endDate);
        }

        String sql = "SELECT * FROM tb_login_log WHERE 1=1" + where;
        return new QueryBuilder(sql, where.toString(), params);
    }

    private record QueryBuilder(String sql, String whereClause, MapSqlParameterSource params) {}

    private LoginLog toDomain(LoginLogEntity entity) {
        return LoginLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .action(entity.getAction())
                .ip(entity.getIp())
                .userAgent(entity.getUserAgent())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
