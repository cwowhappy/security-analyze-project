package com.example.securityanalyze.user.infrastructure;

import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
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
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<User> ROW_MAPPER = new RowMapper<>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setRealName(rs.getString("real_name"));
            user.setStatus(UserStatus.valueOf(rs.getString("status")));
            user.setRole(Role.valueOf(rs.getString("role")));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                user.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            return user;
        }
    };

    @Override
    public User save(User user) {
        log.info("保存用户, username={}", user.getUsername());
        String sql = """
                INSERT INTO sys_user (username, password_hash, real_name, status, role, created_at, updated_at)
                VALUES (:username, :passwordHash, :realName, :status::user_status, :role::user_role, :createdAt, :updatedAt)
                ON CONFLICT (username) DO UPDATE SET
                    password_hash = EXCLUDED.password_hash,
                    real_name = EXCLUDED.real_name,
                    status = EXCLUDED.status,
                    role = EXCLUDED.role,
                    updated_at = EXCLUDED.updated_at
                RETURNING id, username, password_hash, real_name, status, role, created_at, updated_at
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", user.getUsername());
        params.addValue("passwordHash", user.getPasswordHash());
        params.addValue("realName", user.getRealName());
        params.addValue("status", user.getStatus().name());
        params.addValue("role", user.getRole().name());

        LocalDateTime now = LocalDateTime.now();
        params.addValue("createdAt", user.getCreatedAt() != null ? user.getCreatedAt() : now);
        params.addValue("updatedAt", now);

        return jdbcTemplate.queryForObject(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("根据ID查询用户, id={}", id);
        String sql = "SELECT * FROM sys_user WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        List<User> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("根据用户名查询用户, username={}", username);
        String sql = "SELECT * FROM sys_user WHERE username = :username";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", username);

        List<User> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean existsByUsername(String username) {
        log.debug("检查用户名是否存在, username={}", username);
        String sql = "SELECT COUNT(*) FROM sys_user WHERE username = :username";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", username);

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    @Override
    public List<User> findAll() {
        log.debug("查询所有用户");
        String sql = "SELECT * FROM sys_user ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        log.debug("根据状态查询用户, status={}", status);
        String sql = "SELECT * FROM sys_user WHERE status = :status ORDER BY created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("status", status.name());
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public void updateStatus(Long id, UserStatus status) {
        log.info("更新用户状态, id={}, status={}", id, status);
        String sql = "UPDATE sys_user SET status = :status::user_status, updated_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("status", status.name());
        jdbcTemplate.update(sql, params);
    }
}
