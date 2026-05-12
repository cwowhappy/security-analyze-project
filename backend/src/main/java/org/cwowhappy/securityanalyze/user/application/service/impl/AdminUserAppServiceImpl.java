package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.shared.exception.NotFoundException;
import org.cwowhappy.securityanalyze.user.application.service.AdminUserAppService;
import org.cwowhappy.securityanalyze.user.application.service.LoginLogService;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户管理应用服务实现。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserAppServiceImpl implements AdminUserAppService {

    private final UserRepository userRepository;
    private final LoginLogService loginLogService;

    @Override
    public PageResult<User> listUsers(String keyword, String role, Boolean emailVerified,
                                       Boolean locked, int page, int size) {
        List<User> list = userRepository.findAllWithConditions(keyword, role, emailVerified, locked,
                (page - 1) * size, size);
        long total = userRepository.countWithConditions(keyword, role, emailVerified, locked);
        return PageResult.<User>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    @Override
    public User getUserDetail(String userId) {
        return userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    @Override
    @Transactional
    public void updateUser(String userId, String displayName, String role) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("User", userId));
        userRepository.updateDisplayNameAndRole(UserId.of(userId), displayName, role);
        loginLogService.record(userId, user.getUsername(), "force_password_reset", null, null,
                "管理员修改用户信息: displayName=" + displayName + ", role=" + role);
    }

    @Override
    @Transactional
    public void unlockUser(String userId) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("User", userId));
        userRepository.unlock(UserId.of(userId));
        loginLogService.record(userId, user.getUsername(), "login_success", null, null,
                "管理员手动解锁账户");
    }

    @Override
    @Transactional
    public void forcePasswordReset(String userId, String reason) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("User", userId));
        userRepository.updatePasswordExpiredAt(UserId.of(userId), LocalDateTime.now().minusSeconds(1));
        loginLogService.record(userId, user.getUsername(), "force_password_reset", null, null,
                "管理员强制改密: " + reason);
    }
}
