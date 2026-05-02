package com.example.securityanalyze.admin.application;

import com.example.securityanalyze.admin.api.UserListItem;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserListItem> listAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toListItem)
                .toList();
    }

    public List<UserListItem> listPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public void approveUser(Long id) {
        userRepository.updateStatus(id, UserStatus.APPROVED);
    }

    @Transactional
    public void disableUser(Long id) {
        userRepository.updateStatus(id, UserStatus.DISABLED);
    }

    @Transactional
    public void enableUser(Long id) {
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
