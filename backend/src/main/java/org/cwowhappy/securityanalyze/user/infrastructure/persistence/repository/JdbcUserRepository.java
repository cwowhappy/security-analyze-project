package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.UserEntity;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper.UserRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户仓库 JDBC 实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcUserRepository implements UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserRowMapper rowMapper;

    @Override
    public Optional<User> findById(UserId id) {
        String sql = "SELECT * FROM tb_user WHERE id = :id";
        List<UserEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM tb_user WHERE username = :username";
        List<UserEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("username", username), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM tb_user WHERE email = :email";
        List<UserEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("email", email), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public UserId save(User user) {
        String sql = """
                INSERT INTO tb_user (
                    id, username, email, password_hash, display_name, role,
                    avatar_initial, is_active, email_verified, failed_login_attempts,
                    locked_until, last_login_at, created_at, updated_at
                ) VALUES (
                    :id, :username, :email, :passwordHash, :displayName, :role,
                    :avatarInitial, :active, :emailVerified, :failedLoginAttempts,
                    :lockedUntil, :lastLoginAt, :createdAt, :updatedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.username,
                    email = EXCLUDED.email,
                    password_hash = EXCLUDED.password_hash,
                    display_name = EXCLUDED.display_name,
                    role = EXCLUDED.role,
                    avatar_initial = EXCLUDED.avatar_initial,
                    is_active = EXCLUDED.is_active,
                    email_verified = EXCLUDED.email_verified,
                    failed_login_attempts = EXCLUDED.failed_login_attempts,
                    locked_until = EXCLUDED.locked_until,
                    last_login_at = EXCLUDED.last_login_at,
                    updated_at = EXCLUDED.updated_at
                """;
        UserEntity entity = toEntity(user);
        SqlParameterSource params = new BeanPropertySqlParameterSource(entity);
        jdbcTemplate.update(sql, params);
        return user.getId();
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM tb_user WHERE username = :username";
        Long count = jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource("username", username), Long.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM tb_user WHERE email = :email";
        Long count = jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource("email", email), Long.class);
        return count != null && count > 0;
    }

    @Override
    public void updateLastLoginAt(UserId id) {
        String sql = "UPDATE tb_user SET last_login_at = :lastLoginAt WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("lastLoginAt", LocalDateTime.now());
        params.addValue("id", id.getValue());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public int incrementFailedAttempts(UserId id) {
        String updateSql = """
                UPDATE tb_user
                SET failed_login_attempts = failed_login_attempts + 1
                WHERE id = :id
                """;
        jdbcTemplate.update(updateSql, new MapSqlParameterSource("id", id.getValue()));

        String selectSql = "SELECT failed_login_attempts FROM tb_user WHERE id = :id";
        Integer attempts = jdbcTemplate.queryForObject(selectSql,
                new MapSqlParameterSource("id", id.getValue()), Integer.class);
        return attempts != null ? attempts : 0;
    }

    @Override
    public void resetFailedAttempts(UserId id) {
        String sql = "UPDATE tb_user SET failed_login_attempts = 0 WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id.getValue()));
    }

    @Override
    public void lockUser(UserId id, LocalDateTime until) {
        String sql = "UPDATE tb_user SET locked_until = :until WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("until", until);
        params.addValue("id", id.getValue());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void updateEmailVerified(UserId id, boolean verified) {
        String sql = "UPDATE tb_user SET email_verified = :verified WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("verified", verified);
        params.addValue("id", id.getValue());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public List<User> findAllWithConditions(String keyword, String role, Boolean emailVerified,
                                             Boolean locked, int offset, int limit) {
        QueryBuilder qb = buildQuery(keyword, role, emailVerified, locked);
        String sql = qb.sql + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        qb.params.addValue("limit", limit);
        qb.params.addValue("offset", offset);
        List<UserEntity> results = jdbcTemplate.query(sql, qb.params, rowMapper);
        return results.stream().map(this::toDomain).toList();
    }

    @Override
    public long countWithConditions(String keyword, String role, Boolean emailVerified, Boolean locked) {
        QueryBuilder qb = buildQuery(keyword, role, emailVerified, locked);
        String sql = "SELECT COUNT(*) FROM tb_user WHERE 1=1" + qb.whereClause;
        Long count = jdbcTemplate.queryForObject(sql, qb.params, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public void updateDisplayNameAndRole(UserId id, String displayName, String role) {
        String sql = "UPDATE tb_user SET display_name = :displayName, role = :role, updated_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("displayName", displayName);
        params.addValue("role", role);
        params.addValue("id", id.getValue());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void unlock(UserId id) {
        String sql = "UPDATE tb_user SET locked_until = NULL, failed_login_attempts = 0, updated_at = NOW() WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id.getValue()));
    }

    @Override
    public void updatePasswordExpiredAt(UserId id, LocalDateTime expiredAt) {
        String sql = "UPDATE tb_user SET password_expired_at = :expiredAt, updated_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("expiredAt", expiredAt);
        params.addValue("id", id.getValue());
        jdbcTemplate.update(sql, params);
    }

    private QueryBuilder buildQuery(String keyword, String role, Boolean emailVerified, Boolean locked) {
        StringBuilder where = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (username ILIKE :keyword OR email ILIKE :keyword OR display_name ILIKE :keyword)");
            params.addValue("keyword", "%" + keyword + "%");
        }
        if (role != null && !role.isBlank()) {
            where.append(" AND role = :role");
            params.addValue("role", role);
        }
        if (emailVerified != null) {
            where.append(" AND email_verified = :emailVerified");
            params.addValue("emailVerified", emailVerified);
        }
        if (locked != null) {
            if (locked) {
                where.append(" AND locked_until IS NOT NULL AND locked_until > NOW()");
            } else {
                where.append(" AND (locked_until IS NULL OR locked_until <= NOW())");
            }
        }

        String sql = "SELECT * FROM tb_user WHERE 1=1" + where;
        return new QueryBuilder(sql, where.toString(), params);
    }

    private record QueryBuilder(String sql, String whereClause, MapSqlParameterSource params) {}

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(UserId.of(entity.getId()))
                .username(entity.getUsername())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .displayName(entity.getDisplayName())
                .role(entity.getRole())
                .avatarInitial(entity.getAvatarInitial())
                .active(entity.isActive())
                .emailVerified(entity.isEmailVerified())
                .failedLoginAttempts(entity.getFailedLoginAttempts())
                .lockedUntil(entity.getLockedUntil())
                .lastLoginAt(entity.getLastLoginAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().getValue());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setDisplayName(user.getDisplayName());
        entity.setRole(user.getRole());
        entity.setAvatarInitial(user.getAvatarInitial());
        entity.setActive(user.isActive());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockedUntil(user.getLockedUntil());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
