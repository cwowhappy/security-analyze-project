package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.EmailVerificationEntity;
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
 * EmailVerificationRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationRowMapperTest {

    private final EmailVerificationRowMapper mapper = new EmailVerificationRowMapper();

    @Test
    void shouldMapResultSetToEntity() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("user_id")).thenReturn("user001");
        when(rs.getString("verification_code")).thenReturn("123456");
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(now.plusMinutes(30)));
        when(rs.getTimestamp("verified_at")).thenReturn(null);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getBoolean("used")).thenReturn(false);

        EmailVerificationEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo("user001");
        assertThat(entity.getVerificationCode()).isEqualTo("123456");
        assertThat(entity.getExpiresAt()).isEqualTo(now.plusMinutes(30));
        assertThat(entity.getVerifiedAt()).isNull();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.isUsed()).isFalse();
    }

    @Test
    void shouldMapVerifiedEntity() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getLong("id")).thenReturn(2L);
        when(rs.getString("user_id")).thenReturn("user002");
        when(rs.getString("verification_code")).thenReturn("654321");
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("verified_at")).thenReturn(Timestamp.valueOf(now.plusMinutes(5)));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getBoolean("used")).thenReturn(true);

        EmailVerificationEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.isUsed()).isTrue();
        assertThat(entity.getVerifiedAt()).isEqualTo(now.plusMinutes(5));
    }
}
