package com.example.securityanalyze.admin.application;

import com.example.securityanalyze.admin.api.UserListItem;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserListItem> listAllUsers() {
        log.debug("查询所有用户列表");
        return userRepository.findAll().stream()
                .map(this::toListItem)
                .toList();
    }

    public List<UserListItem> listPendingUsers() {
        log.debug("查询待审批用户列表");
        return userRepository.findByStatus(UserStatus.PENDING).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public void approveUser(Long id) {
        log.info("审批用户通过, userId={}", id);
        userRepository.updateStatus(id, UserStatus.APPROVED);
    }

    @Transactional
    public void disableUser(Long id) {
        log.info("禁用用户, userId={}", id);
        userRepository.updateStatus(id, UserStatus.DISABLED);
    }

    @Transactional
    public void enableUser(Long id) {
        log.info("启用用户, userId={}", id);
        userRepository.updateStatus(id, UserStatus.APPROVED);
    }

    private UserListItem toListItem(User user) {
        UserListItem item = new UserListItem();
        item.setId(user.getId());
        item.setUsername(user.getUsername());
        item.setRealName(user.getRealName());
        item.setRole(user.getRole().name());
        item.setStatus(user.getStatus().name());
        item.setCreatedAt(user.getCreatedAt());
        return item;
    }
}
