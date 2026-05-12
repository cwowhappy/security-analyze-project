package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.user.domain.repository.TokenSessionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Token 会话仓库 JDBC 实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcTokenSessionRepository implements TokenSessionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void save(String userId, String tokenHash, LocalDateTime expiresAt) {
        String sql = """
                INSERT INTO tb_user_session (user_id, token_hash, expires_at, created_at)
                VALUES (:userId, :tokenHash, :expiresAt, NOW())
                ON CONFLICT (token_hash) DO UPDATE SET
                    expires_at = EXCLUDED.expires_at,
                    created_at = EXCLUDED.created_at
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("tokenHash", tokenHash);
        params.addValue("expiresAt", expiresAt);
        jdbcTemplate.update(sql, params);
    }

    public boolean existsByTokenHash(String tokenHash) {
        String sql = """
                SELECT COUNT(*) FROM tb_user_session
                WHERE token_hash = :tokenHash AND expires_at > NOW()
                """;
        Long count = jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource("tokenHash", tokenHash), Long.class);
        return count != null && count > 0;
    }

    public int deleteByTokenHash(String tokenHash) {
        String sql = "DELETE FROM tb_user_session WHERE token_hash = :tokenHash";
        return jdbcTemplate.update(sql, new MapSqlParameterSource("tokenHash", tokenHash));
    }

    public int deleteExpiredSessions() {
        String sql = "DELETE FROM tb_user_session WHERE expires_at <= NOW()";
        return jdbcTemplate.update(sql, new MapSqlParameterSource());
    }
}
