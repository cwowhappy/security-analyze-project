package org.cwowhappy.securityanalyze.interfaces.rest.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginRequest 单元测试。
 */
class LoginRequestTest {

    @Test
    void shouldCreateLoginRequestWithDefaults() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        assertThat(request.getUsername()).isEqualTo("testuser");
        assertThat(request.getPassword()).isEqualTo("password123");
        assertThat(request.getRememberMe()).isFalse();
    }

    @Test
    void shouldSetRememberMeToTrue() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRememberMe(true);

        assertThat(request.getRememberMe()).isTrue();
    }
}
