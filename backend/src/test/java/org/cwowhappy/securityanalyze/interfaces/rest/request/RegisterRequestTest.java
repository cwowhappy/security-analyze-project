package org.cwowhappy.securityanalyze.interfaces.rest.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RegisterRequest 单元测试。
 */
class RegisterRequestTest {

    @Test
    void shouldCreateRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole("viewer");

        assertThat(request.getUsername()).isEqualTo("newuser");
        assertThat(request.getEmail()).isEqualTo("new@example.com");
        assertThat(request.getPassword()).isEqualTo("password123");
        assertThat(request.getConfirmPassword()).isEqualTo("password123");
        assertThat(request.getRole()).isEqualTo("viewer");
    }
}
