package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.user.application.service.LoginLogService;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.cwowhappy.securityanalyze.user.domain.repository.LoginLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录日志应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String userId, String username, String action, String ip, String userAgent, String details) {
        LoginLog loginLog = LoginLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .ip(ip)
                .userAgent(userAgent)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        loginLogRepository.save(loginLog);
        log.debug("登录日志已记录: action={}, userId={}", action, userId);
    }
}
