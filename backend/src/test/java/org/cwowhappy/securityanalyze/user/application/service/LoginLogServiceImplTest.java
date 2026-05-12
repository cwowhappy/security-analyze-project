package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.user.application.service.impl.LoginLogServiceImpl;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.cwowhappy.securityanalyze.user.domain.repository.LoginLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * LoginLogService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class LoginLogServiceImplTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private LoginLogServiceImpl loginLogService;

    @Test
    void shouldSaveLogWhenRecord() {
        loginLogService.record("user001", "testuser", "login_success", "127.0.0.1", "Mozilla", "登录成功");
        verify(loginLogRepository, times(1)).save(any(LoginLog.class));
    }
}
