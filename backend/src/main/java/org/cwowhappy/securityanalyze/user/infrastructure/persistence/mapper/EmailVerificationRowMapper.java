package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.EmailVerificationEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 邮箱验证码 JDBC RowMapper。
 */
@Component
public class EmailVerificationRowMapper implements RowMapper<EmailVerificationEntity> {

    @Override
    public EmailVerificationEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        EmailVerificationEntity entity = new EmailVerificationEntity();
        entity.setId(rs.getLong("id"));
        entity.setUserId(rs.getString("user_id"));
        entity.setVerificationCode(rs.getString("verification_code"));
        entity.setExpiresAt(rs.getTimestamp("expires_at") != null
                ? rs.getTimestamp("expires_at").toLocalDateTime()
                : null);
        entity.setVerifiedAt(rs.getTimestamp("verified_at") != null
                ? rs.getTimestamp("verified_at").toLocalDateTime()
                : null);
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUsed(rs.getBoolean("used"));
        return entity;
    }
}
