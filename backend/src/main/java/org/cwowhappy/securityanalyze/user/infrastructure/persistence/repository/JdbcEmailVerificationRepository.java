package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.user.domain.model.EmailVerification;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.EmailVerificationRepository;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.EmailVerificationEntity;
import org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper.EmailVerificationRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 邮箱验证码仓库 JDBC 实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcEmailVerificationRepository implements EmailVerificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EmailVerificationRowMapper rowMapper;

    @Override
    public void save(EmailVerification verification) {
        String sql = """
                INSERT INTO tb_email_verification (
                    user_id, verification_code, expires_at, verified_at, created_at, used
                ) VALUES (
                    :userId, :verificationCode, :expiresAt, :verifiedAt, :createdAt, :used
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", verification.getUserId());
        params.addValue("verificationCode", verification.getVerificationCode());
        params.addValue("expiresAt", verification.getExpiresAt());
        params.addValue("verifiedAt", verification.getVerifiedAt());
        params.addValue("createdAt", verification.getCreatedAt());
        params.addValue("used", verification.isUsed());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public Optional<EmailVerification> findLatestByUserId(UserId userId) {
        String sql = """
                SELECT * FROM tb_email_verification
                WHERE user_id = :userId AND used = false
                ORDER BY created_at DESC
                LIMIT 1
                """;
        List<EmailVerificationEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("userId", userId.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public void markAsUsed(long id) {
        String sql = "UPDATE tb_email_verification SET used = true, verified_at = NOW() WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    private EmailVerification toDomain(EmailVerificationEntity entity) {
        return EmailVerification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .verificationCode(entity.getVerificationCode())
                .expiresAt(entity.getExpiresAt())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .used(entity.isUsed())
                .build();
    }
}
