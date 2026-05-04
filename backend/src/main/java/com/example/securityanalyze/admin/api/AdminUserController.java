package com.example.securityanalyze.admin.api;

import com.example.securityanalyze.admin.application.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<UserListItem>> listUsers() {
        log.debug("查询用户列表");
        return ResponseEntity.ok(adminUserService.listAllUsers());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveUser(@PathVariable Long id) {
        log.info("审批用户通过, userId={}", id);
        adminUserService.approveUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        log.info("禁用用户, userId={}", id);
        adminUserService.disableUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        log.info("启用用户, userId={}", id);
        adminUserService.enableUser(id);
        return ResponseEntity.ok().build();
    }
}
