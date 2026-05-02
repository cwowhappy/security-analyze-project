package com.example.securityanalyze.user.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "test-secret-key-for-jwt-token-provider-123");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken("testuser", "USER");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtTokenProvider.generateToken("testuser", "USER");
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    void shouldExtractRoleFromToken() {
        String token = jwtTokenProvider.generateToken("testuser", "ADMIN");
        String role = jwtTokenProvider.getRoleFromToken(token);
        assertEquals("ADMIN", role);
    }

    @Test
    void shouldInvalidateMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }
}
