package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.PasswordResetEntity;
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
 * PasswordResetRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetRowMapperTest {

    private final PasswordResetRowMapper mapper = new PasswordResetRowMapper();

    @Test
    void shouldMapResultSetToEntity() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("user_id")).thenReturn("user001");
        when(rs.getString("reset_token")).thenReturn("token-abc");
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(now.plusHours(1)));
        when(rs.getBoolean("used")).thenReturn(false);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));

        PasswordResetEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo("user001");
        assertThat(entity.getResetToken()).isEqualTo("token-abc");
        assertThat(entity.getExpiresAt()).isEqualTo(now.plusHours(1));
        assertThat(entity.isUsed()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldMapUsedToken() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getLong("id")).thenReturn(2L);
        when(rs.getString("user_id")).thenReturn("user002");
        when(rs.getString("reset_token")).thenReturn("token-used");
        when(rs.getTimestamp("expires_at")).thenReturn(null);
        when(rs.getBoolean("used")).thenReturn(true);
        when(rs.getTimestamp("created_at")).thenReturn(null);

        PasswordResetEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.isUsed()).isTrue();
        assertThat(entity.getExpiresAt()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }
}
