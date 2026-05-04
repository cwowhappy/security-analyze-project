package com.example.securityanalyze.exception;

import com.example.securityanalyze.auth.application.AccountDisabledException;
import com.example.securityanalyze.auth.application.PendingApprovalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void shouldHandleBadCredentials() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("密码错误"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("密码错误", response.getBody().message());
    }

    @Test
    void shouldHandlePendingApproval() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handlePendingApproval(new PendingApprovalException("待审批"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("待审批", response.getBody().message());
    }

    @Test
    void shouldHandleAccountDisabled() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccountDisabled(new AccountDisabledException("已禁用"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("已禁用", response.getBody().message());
    }

    @Test
    void shouldHandleAccessDenied() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("拒绝"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().message().contains("权限不足"));
    }

    @Test
    void shouldHandleIllegalArgument() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("参数错误"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("参数错误", response.getBody().message());
    }

    @Test
    void shouldHandleValidationError() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "username", "用户名不能为空"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("username"));
    }

    @Test
    void shouldHandleGenericException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGeneric(new RuntimeException("系统错误"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().message().contains("系统错误"));
    }
}
