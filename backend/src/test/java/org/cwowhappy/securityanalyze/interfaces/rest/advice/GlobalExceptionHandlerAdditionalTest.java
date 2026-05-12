package org.cwowhappy.securityanalyze.interfaces.rest.advice;

import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.shared.exception.ConflictException;
import org.cwowhappy.securityanalyze.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler 补充单元测试。
 */
class GlobalExceptionHandlerAdditionalTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnUnauthorizedWhenUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("登录已过期");
        ResponseEntity<?> response = handler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        @SuppressWarnings("unchecked")
        ApiResponse<Void> body = (ApiResponse<Void>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("登录已过期");
    }

    @Test
    void shouldReturnConflictWhenConflictException() {
        ConflictException ex = new ConflictException("用户名已存在", Map.of("username", "用户名已存在"));

        ResponseEntity<?> response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        @SuppressWarnings("unchecked")
        ApiResponse<Map<String, String>> body = (ApiResponse<Map<String, String>>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(409);
        assertThat(body.getMessage()).isEqualTo("用户名已存在");
        assertThat(body.getData()).containsEntry("username", "用户名已存在");
    }

    @Test
    void shouldReturnConflictWithoutErrors() {
        ConflictException ex = new ConflictException("资源冲突", Map.of());

        ResponseEntity<?> response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        @SuppressWarnings("unchecked")
        ApiResponse<Map<String, String>> body = (ApiResponse<Map<String, String>>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("资源冲突");
        assertThat(body.getData()).isEmpty();
    }
}
