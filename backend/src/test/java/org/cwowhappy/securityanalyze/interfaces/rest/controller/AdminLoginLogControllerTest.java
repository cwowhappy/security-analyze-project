package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.interfaces.rest.support.AuthContextHelper;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.AdminLogAppService;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminLoginLogController Web 层测试。
 */
@WebMvcTest(AdminLoginLogController.class)
class AdminLoginLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLogAppService adminLogAppService;

    @MockitoBean
    private AuthContextHelper authContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldReturnLogsWhenAdminAccess() throws Exception {
        UserDTO admin = UserDTO.builder().id("admin001").username("admin").role("admin").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(admin);

        LoginLog log = LoginLog.builder()
                .id(1L)
                .userId("user001")
                .username("test")
                .action("login_success")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .details("登录成功")
                .createdAt(LocalDateTime.now())
                .build();
        PageResult<LoginLog> pageResult = PageResult.<LoginLog>builder()
                .list(List.of(log))
                .total(1)
                .page(1)
                .size(20)
                .build();
        when(adminLogAppService.queryLogs(isNull(), isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/login-logs")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list[0].action").value("login_success"));
    }

    @Test
    void shouldReturnForbiddenWhenNonAdminAccess() throws Exception {
        UserDTO user = UserDTO.builder().id("user001").username("test").role("viewer").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(user);

        mockMvc.perform(get("/api/v1/admin/login-logs")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldExportCsvWhenAdminAccess() throws Exception {
        UserDTO admin = UserDTO.builder().id("admin001").username("admin").role("admin").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(admin);

        LoginLog log = LoginLog.builder()
                .id(1L)
                .userId("user001")
                .username("test")
                .action("login_success")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .createdAt(LocalDateTime.now())
                .build();
        when(adminLogAppService.exportLogs(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/admin/login-logs/export")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentDisposition = result.getResponse().getHeader("Content-Disposition");
                    assert contentDisposition != null && contentDisposition.contains("login-logs.csv");
                });
    }
}
