package com.example.securityanalyze.user.application;

import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.infrastructure.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldAuthenticateUser() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("hashed");
        user.setRole(Role.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        User result = authenticationService.authenticate("admin", "password");

        assertEquals("admin", result.getUsername());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate("unknown", "pass"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void shouldThrowWhenPasswordMismatch() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate("admin", "wrong"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void shouldGenerateToken() {
        User user = new User();
        user.setUsername("admin");
        user.setRole(Role.ADMIN);

        when(jwtTokenProvider.generateToken("admin", "ADMIN")).thenReturn("token123");

        String token = authenticationService.generateToken(user);

        assertEquals("token123", token);
    }
}
