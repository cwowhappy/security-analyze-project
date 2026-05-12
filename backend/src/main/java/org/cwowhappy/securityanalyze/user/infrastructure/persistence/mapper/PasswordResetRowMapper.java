package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.PasswordResetEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 密码重置令牌 JDBC RowMapper。
 */
@Component
public class PasswordResetRowMapper implements RowMapper<PasswordResetEntity> {

    @Override
    public PasswordResetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        PasswordResetEntity entity = new PasswordResetEntity();
        entity.setId(rs.getLong("id"));
        entity.setUserId(rs.getString("user_id"));
        entity.setResetToken(rs.getString("reset_token"));
        entity.setExpiresAt(rs.getTimestamp("expires_at") != null
                ? rs.getTimestamp("expires_at").toLocalDateTime()
                : null);
        entity.setUsed(rs.getBoolean("used"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        return entity;
    }
}
