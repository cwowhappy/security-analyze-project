package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.interfaces.rest.support.AuthContextHelper;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.AdminUserAppService;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminUserController Web 层测试。
 */
@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserAppService adminUserAppService;

    @MockitoBean
    private AuthContextHelper authContextHelper;

    @Test
    void shouldReturnUsersWhenAdminAccess() throws Exception {
        UserDTO admin = UserDTO.builder().id("admin001").username("admin").role("admin").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(admin);

        User user = sampleUser();
        org.cwowhappy.securityanalyze.shared.dto.PageResult<User> pageResult =
                org.cwowhappy.securityanalyze.shared.dto.PageResult.<User>builder()
                        .list(List.of(user))
                        .total(1)
                        .page(1)
                        .size(20)
                        .build();
        when(adminUserAppService.listUsers(isNull(), isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list[0].username").value("testuser"));
    }

    @Test
    void shouldReturnUserDetailWhenAdminAccess() throws Exception {
        UserDTO admin = UserDTO.builder().id("admin001").username("admin").role("admin").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(admin);

        User user = sampleUser();
        when(adminUserAppService.getUserDetail("user001")).thenReturn(user);

        mockMvc.perform(get("/api/v1/admin/users/user001")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void shouldUpdateUserWhenAdminAccess() throws Exception {
        UserDTO admin = UserDTO.builder().id("admin001").username("admin").role("admin").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(admin);
        doNothing().when(adminUserAppService).updateUser(eq("user001"), eq("新昵称"), eq("analyst"));

        mockMvc.perform(put("/api/v1/admin/users/user001")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "新昵称", "role", "analyst"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldUnlockUserWhenAdminAccess() throws Exception {
        UserDTO admin = UserDTO.builder().id("admin001").username("admin").role("admin").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(admin);
        doNothing().when(adminUserAppService).unlockUser("user001");

        mockMvc.perform(post("/api/v1/admin/users/user001/unlock")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturnForbiddenWhenNonAdminAccess() throws Exception {
        UserDTO user = UserDTO.builder().id("user001").username("test").role("viewer").build();
        when(authContextHelper.getCurrentUser(any())).thenReturn(user);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private User sampleUser() {
        return User.builder()
                .id(UserId.of("user001"))
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .displayName("testuser")
                .role("viewer")
                .avatarInitial("T")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
