package com.example.securityanalyze.config;

import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void shouldCreateDefaultAdminWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataInitializer.run(args);

        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("admin") &&
                user.getRole() == Role.ADMIN &&
                user.getStatus() == UserStatus.APPROVED
        ));
    }

    @Test
    void shouldNotCreateAdminWhenUsersExist() {
        User existing = new User();
        existing.setUsername("user");
        when(userRepository.findAll()).thenReturn(List.of(existing));

        dataInitializer.run(args);

        verify(userRepository, never()).save(any());
    }
}
