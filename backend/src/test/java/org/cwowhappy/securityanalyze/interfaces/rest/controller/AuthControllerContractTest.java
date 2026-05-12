package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.interfaces.rest.request.LoginRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.RegisterRequest;
import org.cwowhappy.securityanalyze.user.application.dto.LoginResult;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.AuthAppService;
import org.cwowhappy.securityanalyze.user.application.service.TokenBlacklistService;
import org.cwowhappy.securityanalyze.user.application.service.UserAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController Contract 测试（验证请求/响应序列化与基本路由）。
 */
@WebMvcTest(AuthController.class)
class AuthControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthAppService authAppService;

    @MockitoBean
    private UserAppService userAppService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void shouldReturnLoginResponseWhenLoginSuccess() throws Exception {
        UserDTO user = UserDTO.builder()
                .id("user001")
                .username("testuser")
                .email("test@example.com")
                .role("viewer")
                .displayName("Test")
                .avatarInitial("T")
                .build();
        LoginResult result = LoginResult.builder()
                .accessToken("jwt-token-123")
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(user)
                .build();
        when(authAppService.login(anyString(), anyString(), any(), any())).thenReturn(result);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token-123"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));
    }

    @Test
    void shouldReturnUserInfoWhenTokenValid() throws Exception {
        UserDTO user = UserDTO.builder()
                .id("user001")
                .username("testuser")
                .email("test@example.com")
                .role("viewer")
                .displayName("Test")
                .avatarInitial("T")
                .build();
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn("user001");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(java.util.Optional.of(mock(com.auth0.jwt.interfaces.DecodedJWT.class)));
        when(tokenBlacklistService.isTokenValid("valid-token")).thenReturn(true);
        when(authAppService.getCurrentUser("user001")).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnAvailableWhenUsernameNotExists() throws Exception {
        when(userAppService.isUsernameAvailable("newuser")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterPasswordMismatch() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("different");
        request.setRole("viewer");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private com.auth0.jwt.interfaces.DecodedJWT mock(Class<com.auth0.jwt.interfaces.DecodedJWT> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
