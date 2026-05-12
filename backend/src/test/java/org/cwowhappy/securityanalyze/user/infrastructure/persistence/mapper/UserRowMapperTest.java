package org.cwowhappy.securityanalyze.user.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity.UserEntity;
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
 * UserRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserRowMapperTest {

    private final UserRowMapper mapper = new UserRowMapper();

    @Test
    void shouldMapResultSetToUserEntity() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getString("id")).thenReturn("user001");
        when(rs.getString("username")).thenReturn("testuser");
        when(rs.getString("email")).thenReturn("test@example.com");
        when(rs.getString("password_hash")).thenReturn("hash123");
        when(rs.getString("display_name")).thenReturn("Test User");
        when(rs.getString("role")).thenReturn("viewer");
        when(rs.getString("avatar_initial")).thenReturn("T");
        when(rs.getBoolean("is_active")).thenReturn(true);
        when(rs.getBoolean("email_verified")).thenReturn(true);
        when(rs.getInt("failed_login_attempts")).thenReturn(0);
        when(rs.getTimestamp("locked_until")).thenReturn(null);
        when(rs.getTimestamp("last_login_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));

        UserEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.getId()).isEqualTo("user001");
        assertThat(entity.getUsername()).isEqualTo("testuser");
        assertThat(entity.getEmail()).isEqualTo("test@example.com");
        assertThat(entity.getPasswordHash()).isEqualTo("hash123");
        assertThat(entity.getDisplayName()).isEqualTo("Test User");
        assertThat(entity.getRole()).isEqualTo("viewer");
        assertThat(entity.getAvatarInitial()).isEqualTo("T");
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isEmailVerified()).isTrue();
        assertThat(entity.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(entity.getLockedUntil()).isNull();
        assertThat(entity.getLastLoginAt()).isEqualTo(now);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void shouldHandleNullTimestamps() throws Exception {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getString("id")).thenReturn("user002");
        when(rs.getString("username")).thenReturn("user2");
        when(rs.getString("email")).thenReturn("u2@example.com");
        when(rs.getString("password_hash")).thenReturn("hash");
        when(rs.getString("display_name")).thenReturn("User 2");
        when(rs.getString("role")).thenReturn("analyst");
        when(rs.getString("avatar_initial")).thenReturn("U");
        when(rs.getBoolean("is_active")).thenReturn(false);
        when(rs.getBoolean("email_verified")).thenReturn(false);
        when(rs.getInt("failed_login_attempts")).thenReturn(3);
        when(rs.getTimestamp("locked_until")).thenReturn(null);
        when(rs.getTimestamp("last_login_at")).thenReturn(null);
        when(rs.getTimestamp("created_at")).thenReturn(null);
        when(rs.getTimestamp("updated_at")).thenReturn(null);

        UserEntity entity = mapper.mapRow(rs, 1);

        assertThat(entity.getLockedUntil()).isNull();
        assertThat(entity.getLastLoginAt()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getFailedLoginAttempts()).isEqualTo(3);
    }
}
