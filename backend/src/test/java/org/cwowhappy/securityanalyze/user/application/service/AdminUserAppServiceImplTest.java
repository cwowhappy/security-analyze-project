package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.shared.exception.NotFoundException;
import org.cwowhappy.securityanalyze.user.application.service.impl.AdminUserAppServiceImpl;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AdminUserAppService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminUserAppServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginLogService loginLogService;

    @InjectMocks
    private AdminUserAppServiceImpl adminUserAppService;

    @Test
    void shouldReturnPageResultWhenListUsers() {
        User user = sampleUser();
        when(userRepository.findAllWithConditions(null, null, null, null, 0, 20))
                .thenReturn(List.of(user));
        when(userRepository.countWithConditions(null, null, null, null))
                .thenReturn(1L);

        PageResult<User> result = adminUserAppService.listUsers(null, null, null, null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    void shouldReturnUserWhenGetDetail() {
        User user = sampleUser();
        when(userRepository.findById(UserId.of("user001"))).thenReturn(Optional.of(user));

        User result = adminUserAppService.getUserDetail("user001");

        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(UserId.of("none"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserAppService.getUserDetail("none"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldUnlockUser() {
        User user = sampleUser();
        when(userRepository.findById(UserId.of("user001"))).thenReturn(Optional.of(user));

        adminUserAppService.unlockUser("user001");

        verify(userRepository, times(1)).unlock(UserId.of("user001"));
    }

    @Test
    void shouldForcePasswordReset() {
        User user = sampleUser();
        when(userRepository.findById(UserId.of("user001"))).thenReturn(Optional.of(user));

        adminUserAppService.forcePasswordReset("user001", "安全策略");

        verify(userRepository, times(1)).updatePasswordExpiredAt(eq(UserId.of("user001")), any(LocalDateTime.class));
    }

    private User sampleUser() {
        return User.builder()
                .id(UserId.of("user001"))
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .displayName("testuser")
                .role("viewer")
                .avatarInitial("T")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
