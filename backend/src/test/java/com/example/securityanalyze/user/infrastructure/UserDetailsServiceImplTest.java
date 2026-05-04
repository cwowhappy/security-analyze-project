package com.example.securityanalyze.user.infrastructure;

import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldLoadUserByUsername() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.APPROVED);
        user.setRole(Role.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void shouldLoadDisabledUser() {
        User user = new User();
        user.setUsername("user");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.DISABLED);
        user.setRole(Role.USER);

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("user");

        assertFalse(details.isEnabled());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));
        assertTrue(ex.getMessage().contains("unknown"));
    }
}
