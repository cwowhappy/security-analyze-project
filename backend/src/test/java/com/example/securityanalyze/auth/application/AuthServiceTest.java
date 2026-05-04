package com.example.securityanalyze.auth.application;

import com.example.securityanalyze.auth.api.LoginRequest;
import com.example.securityanalyze.auth.api.RegisterRequest;
import com.example.securityanalyze.user.application.AuthenticationService;
import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRealName("张三");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenPasswordsNotMatch() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("pass1");
        request.setConfirmPassword("pass2");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertEquals("两次输入的密码不一致", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("pass");
        request.setConfirmPassword("pass");
        request.setRealName("李四");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    void shouldLoginApprovedUser() {
        LoginRequest request = new LoginRequest();
        request.setUsername("user");
        request.setPassword("pass");

        User user = new User();
        user.setUsername("user");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.APPROVED);
        user.setRole(Role.USER);

        when(authenticationService.authenticate("user", "pass")).thenReturn(user);
        when(authenticationService.generateToken(user)).thenReturn("token123");

        var response = authService.login(request);
        assertEquals("token123", response.getToken());
    }

    @Test
    void shouldThrowWhenUserPending() {
        LoginRequest request = new LoginRequest();
        request.setUsername("pending");
        request.setPassword("pass");

        User user = new User();
        user.setUsername("pending");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.PENDING);
        user.setRole(Role.USER);

        when(authenticationService.authenticate("pending", "pass")).thenReturn(user);

        PendingApprovalException ex = assertThrows(PendingApprovalException.class,
                () -> authService.login(request));
        assertEquals("账号待审批，请联系管理员", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUserDisabled() {
        LoginRequest request = new LoginRequest();
        request.setUsername("disabled");
        request.setPassword("pass");

        User user = new User();
        user.setUsername("disabled");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.DISABLED);
        user.setRole(Role.USER);

        when(authenticationService.authenticate("disabled", "pass")).thenReturn(user);

        AccountDisabledException ex = assertThrows(AccountDisabledException.class,
                () -> authService.login(request));
        assertEquals("账号已禁用，请联系管理员", ex.getMessage());
    }
}
