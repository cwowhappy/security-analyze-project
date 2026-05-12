package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.LoginLogEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 登录日志 JDBC RowMapper。
 */
@Component
public class LoginLogRowMapper implements RowMapper<LoginLogEntity> {

    @Override
    public LoginLogEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setId(rs.getLong("id"));
        entity.setUserId(rs.getString("user_id"));
        entity.setUsername(rs.getString("username"));
        entity.setAction(rs.getString("action"));
        entity.setIp(rs.getString("ip"));
        entity.setUserAgent(rs.getString("user_agent"));
        entity.setDetails(rs.getString("details"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        return entity;
    }
}
