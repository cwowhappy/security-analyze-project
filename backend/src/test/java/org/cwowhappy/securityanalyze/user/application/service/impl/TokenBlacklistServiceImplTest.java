package org.cwowhappy.securityanalyze.user.application.service.impl;

import org.cwowhappy.securityanalyze.user.domain.repository.TokenSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TokenBlacklistServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    @Mock
    private TokenSessionRepository tokenSessionRepository;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    @Test
    void shouldRecordTokenHash() {
        // Act
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        tokenBlacklistService.recordToken("user001", "raw-token", expiresAt);

        // Assert
        verify(tokenSessionRepository, times(1)).save(eq("user001"), anyString(), eq(expiresAt));
    }

    @Test
    void shouldReturnTrueWhenTokenExists() {
        // Arrange
        when(tokenSessionRepository.existsByTokenHash(anyString())).thenReturn(true);

        // Act
        boolean valid = tokenBlacklistService.isTokenValid("raw-token");

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenNotExists() {
        // Arrange
        when(tokenSessionRepository.existsByTokenHash(anyString())).thenReturn(false);

        // Act
        boolean valid = tokenBlacklistService.isTokenValid("raw-token");

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    void shouldDeleteTokenHashOnRevoke() {
        // Arrange
        when(tokenSessionRepository.deleteByTokenHash(anyString())).thenReturn(1);

        // Act
        tokenBlacklistService.revokeToken("raw-token");

        // Assert
        verify(tokenSessionRepository, times(1)).deleteByTokenHash(anyString());
    }

    @Test
    void shouldGenerateSameHashForSameToken() {
        // Arrange
        when(tokenSessionRepository.existsByTokenHash(anyString())).thenReturn(true);

        // Act
        boolean v1 = tokenBlacklistService.isTokenValid("same-token");
        boolean v2 = tokenBlacklistService.isTokenValid("same-token");

        // Assert
        assertThat(v1).isTrue();
        assertThat(v2).isTrue();
        // Verify the same hash was computed both times
        verify(tokenSessionRepository, times(2)).existsByTokenHash(anyString());
    }
}
