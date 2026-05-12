package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.user.domain.model.PasswordReset;
import org.cwowhappy.securityanalyze.user.domain.repository.PasswordResetRepository;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.PasswordResetEntity;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper.PasswordResetRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 密码重置令牌仓库 JDBC 实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcPasswordResetRepository implements PasswordResetRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PasswordResetRowMapper rowMapper;

    @Override
    public void save(PasswordReset passwordReset) {
        String sql = """
                INSERT INTO tb_password_reset (
                    user_id, reset_token, expires_at, used, created_at
                ) VALUES (
                    :userId, :resetToken, :expiresAt, :used, :createdAt
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", passwordReset.getUserId());
        params.addValue("resetToken", passwordReset.getResetToken());
        params.addValue("expiresAt", passwordReset.getExpiresAt());
        params.addValue("used", passwordReset.isUsed());
        params.addValue("createdAt", passwordReset.getCreatedAt());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public Optional<PasswordReset> findByToken(String token) {
        String sql = "SELECT * FROM tb_password_reset WHERE reset_token = :token";
        List<PasswordResetEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("token", token), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public void markAsUsed(Long id) {
        String sql = "UPDATE tb_password_reset SET used = true WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    private PasswordReset toDomain(PasswordResetEntity entity) {
        return PasswordReset.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .resetToken(entity.getResetToken())
                .expiresAt(entity.getExpiresAt())
                .used(entity.isUsed())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
