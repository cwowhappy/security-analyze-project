package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用户 JDBC RowMapper。
 */
@Component
public class UserRowMapper implements RowMapper<UserEntity> {

    @Override
    public UserEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserEntity entity = new UserEntity();
        entity.setId(rs.getString("id"));
        entity.setUsername(rs.getString("username"));
        entity.setEmail(rs.getString("email"));
        entity.setPasswordHash(rs.getString("password_hash"));
        entity.setDisplayName(rs.getString("display_name"));
        entity.setRole(rs.getString("role"));
        entity.setAvatarInitial(rs.getString("avatar_initial"));
        entity.setActive(rs.getBoolean("is_active"));
        entity.setEmailVerified(rs.getBoolean("email_verified"));
        entity.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        entity.setLockedUntil(rs.getTimestamp("locked_until") != null
                ? rs.getTimestamp("locked_until").toLocalDateTime()
                : null);
        entity.setLastLoginAt(rs.getTimestamp("last_login_at") != null
                ? rs.getTimestamp("last_login_at").toLocalDateTime()
                : null);
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
