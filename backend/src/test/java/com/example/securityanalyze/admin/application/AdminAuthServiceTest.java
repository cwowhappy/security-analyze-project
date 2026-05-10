package com.example.securityanalyze.admin.application;

import com.example.securityanalyze.user.application.AuthenticationService;
import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    void shouldLoginAdmin() {
        AdminLoginCommand command = new AdminLoginCommand();
        command.setUsername("admin");
        command.setPassword("admin123");

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash("hashed");
        admin.setRole(Role.ADMIN);

        when(authenticationService.authenticate("admin", "admin123")).thenReturn(admin);
        when(authenticationService.generateToken(admin)).thenReturn("admintoken");

        var response = adminAuthService.login(command);
        assertEquals("admintoken", response.getToken());
    }

    @Test
    void shouldThrowWhenNotAdmin() {
        AdminLoginCommand command = new AdminLoginCommand();
        command.setUsername("user");
        command.setPassword("pass");

        User user = new User();
        user.setUsername("user");
        user.setPasswordHash("hashed");
        user.setRole(Role.USER);

        when(authenticationService.authenticate("user", "pass")).thenReturn(user);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> adminAuthService.login(command));
        assertEquals("您不是管理员，无法登录管理后台", ex.getMessage());
    }

    @Test
    void shouldRegisterAdmin() {
        AdminRegisterCommand command = new AdminRegisterCommand();
        command.setUsername("newadmin");
        command.setPassword("pass123");
        command.setRealName("新管理员");

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> adminAuthService.registerAdmin(command));
        verify(userRepository).save(any(User.class));
    }
}
