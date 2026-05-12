package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.shared.exception.NotFoundException;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.impl.UserAppServiceImpl;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 用户应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserAppServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAppServiceImpl userAppService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UserId.of("user001"))
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .displayName("Test User")
                .role("viewer")
                .avatarInitial("T")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldReturnUserDTOWhenFindByIdExists() {
        // Arrange
        when(userRepository.findById(UserId.of("user001"))).thenReturn(Optional.of(sampleUser));

        // Act
        UserDTO result = userAppService.findById("user001");

        // Assert
        assertThat(result.getId()).isEqualTo("user001");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getDisplayName()).isEqualTo("Test User");
        assertThat(result.getRole()).isEqualTo("viewer");
        assertThat(result.getAvatarInitial()).isEqualTo("T");
    }

    @Test
    void shouldThrowNotFoundWhenFindByIdNotExists() {
        // Arrange
        when(userRepository.findById(UserId.of("notexist"))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userAppService.findById("notexist"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("notexist");
    }

    @Test
    void shouldReturnUserDTOWhenFindByUsernameExists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        // Act
        Optional<UserDTO> result = userAppService.findByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldReturnEmptyWhenFindByUsernameNotExists() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act
        Optional<UserDTO> result = userAppService.findByUsername("unknown");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnUserDTOWhenFindByEmailExists() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        // Act
        Optional<UserDTO> result = userAppService.findByEmail("test@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnEmptyWhenFindByEmailNotExists() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<UserDTO> result = userAppService.findByEmail("unknown@example.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenUsernameAvailable() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // Act
        boolean result = userAppService.isUsernameAvailable("newuser");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUsernameTaken() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act
        boolean result = userAppService.isUsernameAvailable("testuser");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueWhenEmailAvailable() {
        // Arrange
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // Act
        boolean result = userAppService.isEmailAvailable("new@example.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailTaken() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act
        boolean result = userAppService.isEmailAvailable("test@example.com");

        // Assert
        assertThat(result).isFalse();
    }
}
