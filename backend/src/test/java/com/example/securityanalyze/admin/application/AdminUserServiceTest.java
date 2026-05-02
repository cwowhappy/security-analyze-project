package com.example.securityanalyze.admin.application;

import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void shouldListAllUsers() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setRole(Role.USER);
        user1.setStatus(UserStatus.APPROVED);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setRole(Role.ADMIN);
        user2.setStatus(UserStatus.PENDING);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        var result = adminUserService.listAllUsers();
        assertEquals(2, result.size());
    }

    @Test
    void shouldApproveUser() {
        adminUserService.approveUser(1L);
        verify(userRepository).updateStatus(1L, UserStatus.APPROVED);
    }

    @Test
    void shouldDisableUser() {
        adminUserService.disableUser(1L);
        verify(userRepository).updateStatus(1L, UserStatus.DISABLED);
    }

    @Test
    void shouldEnableUser() {
        adminUserService.enableUser(1L);
        verify(userRepository).updateStatus(1L, UserStatus.APPROVED);
    }
}
