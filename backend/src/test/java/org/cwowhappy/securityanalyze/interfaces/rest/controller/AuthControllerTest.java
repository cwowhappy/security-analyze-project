package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.interfaces.rest.request.ForgotPasswordRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.LoginRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.RegisterRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.ResetPasswordRequest;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController Web 层测试。
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

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
    void shouldReturnTokenWhenLoginSuccess() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRememberMe(false);

        UserDTO userDTO = UserDTO.builder()
                .id("user001")
                .username("testuser")
                .email("test@example.com")
                .role("viewer")
                .displayName("testuser")
                .avatarInitial("T")
                .build();

        LoginResult result = LoginResult.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(userDTO)
                .build();

        when(authAppService.login(eq("testuser"), eq("password123"), any(), any())).thenReturn(result);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }

    @Test
    void shouldReturnSuccessWhenRegisterValid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole("viewer");

        UserDTO userDTO = UserDTO.builder()
                .id("user002")
                .username("newuser")
                .email("new@example.com")
                .role("viewer")
                .displayName("newuser")
                .avatarInitial("N")
                .build();

        when(authAppService.register(any())).thenReturn(userDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterPasswordMismatch() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("different");
        request.setRole("viewer");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnSuccessWhenForgotPassword() throws Exception {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        doNothing().when(authAppService).forgotPassword(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("重置链接已发送至您的邮箱"));
    }

    @Test
    void shouldReturnSuccessWhenVerifyResetTokenValid() throws Exception {
        // Arrange
        doNothing().when(authAppService).verifyResetToken("valid-token");

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/verify-reset-token")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturnSuccessWhenResetPassword() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword123");
        request.setConfirmPassword("newpassword123");

        doNothing().when(authAppService).resetPassword(anyString(), anyString(), any(), any());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码重置成功"));
    }

    @Test
    void shouldReturnBadRequestWhenResetPasswordMismatch() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword123");
        request.setConfirmPassword("different");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnAvailableTrueWhenUsernameNotExists() throws Exception {
        // Arrange
        when(userAppService.isUsernameAvailable("newuser")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void shouldReturnAvailableFalseWhenUsernameExists() throws Exception {
        // Arrange
        when(userAppService.isUsernameAvailable("existing")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "existing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void shouldReturnAvailableTrueWhenEmailNotExists() throws Exception {
        // Arrange
        when(userAppService.isEmailAvailable("new@example.com")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/check-email")
                        .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void shouldReturnCurrentUserWhenTokenValid() throws Exception {
        // Arrange
        UserDTO userDTO = UserDTO.builder()
                .id("user001")
                .username("testuser")
                .email("test@example.com")
                .role("viewer")
                .displayName("testuser")
                .avatarInitial("T")
                .build();

        when(jwtTokenProvider.getUserIdFromToken("valid-jwt")).thenReturn("user001");
        when(jwtTokenProvider.validateToken("valid-jwt")).thenReturn(Optional.of(mock(DecodedJWT.class)));
        when(tokenBlacklistService.isTokenValid("valid-jwt")).thenReturn(true);
        when(authAppService.getCurrentUser("user001")).thenReturn(userDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer valid-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }
}
