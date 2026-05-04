package com.example.securityanalyze.admin.api;

import com.example.securityanalyze.admin.application.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void shouldListUsers() throws Exception {
        UserListItem item = new UserListItem();
        item.setId(1L);
        item.setUsername("user1");
        item.setRealName("用户1");
        item.setRole("USER");
        item.setStatus("APPROVED");
        item.setCreatedAt(LocalDateTime.now());

        when(adminUserService.listAllUsers()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user1"));
    }

    @Test
    void shouldApproveUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/approve"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDisableUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/disable"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldEnableUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/enable"))
                .andExpect(status().isOk());
    }
}
