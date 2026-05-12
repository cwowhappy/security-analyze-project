package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.LoginLogEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LoginLogRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class LoginLogRowMapperTest {

    private final LoginLogRowMapper mapper = new LoginLogRowMapper();

    @Test
    void shouldMapResultSetToEntity() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("user_id")).thenReturn("user001");
        when(rs.getString("username")).thenReturn("testuser");
        when(rs.getString("action")).thenReturn("login_success");
        when(rs.getString("ip")).thenReturn("127.0.0.1");
        when(rs.getString("user_agent")).thenReturn("Mozilla/5.0");
        when(rs.getString("details")).thenReturn("{\"device\":\"desktop\"}");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));

        LoginLogEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo("user001");
        assertThat(entity.getUsername()).isEqualTo("testuser");
        assertThat(entity.getAction()).isEqualTo("login_success");
        assertThat(entity.getIp()).isEqualTo("127.0.0.1");
        assertThat(entity.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(entity.getDetails()).isEqualTo("{\"device\":\"desktop\"}");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldHandleNullCreatedAt() throws Exception {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getLong("id")).thenReturn(2L);
        when(rs.getString("user_id")).thenReturn("user002");
        when(rs.getString("username")).thenReturn("user2");
        when(rs.getString("action")).thenReturn("logout");
        when(rs.getString("ip")).thenReturn("192.168.1.1");
        when(rs.getString("user_agent")).thenReturn(null);
        when(rs.getString("details")).thenReturn(null);
        when(rs.getTimestamp("created_at")).thenReturn(null);

        LoginLogEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUserAgent()).isNull();
        assertThat(entity.getDetails()).isNull();
    }
}
