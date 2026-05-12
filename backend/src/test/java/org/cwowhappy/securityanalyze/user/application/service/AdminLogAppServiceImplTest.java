package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.user.application.service.impl.AdminLogAppServiceImpl;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.cwowhappy.securityanalyze.user.domain.repository.LoginLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * AdminLogAppService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminLogAppServiceImplTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private AdminLogAppServiceImpl adminLogAppService;

    @Test
    void shouldReturnPageResultWhenQueryLogs() {
        LoginLog log = LoginLog.builder()
                .id(1L)
                .userId("user001")
                .username("test")
                .action("login_success")
                .build();
        when(loginLogRepository.findByConditions(null, null, null, null, 1, 20))
                .thenReturn(List.of(log));
        when(loginLogRepository.countByConditions(null, null, null, null))
                .thenReturn(1L);

        PageResult<LoginLog> result = adminLogAppService.queryLogs(null, null, null, null, 1, 20);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllLogsWhenExport() {
        LoginLog log = LoginLog.builder().id(1L).action("logout").build();
        when(loginLogRepository.findAllByConditions(eq("user001"), isNull(), isNull(), isNull()))
                .thenReturn(List.of(log));

        List<LoginLog> result = adminLogAppService.exportLogs("user001", null, null, null);

        assertThat(result).hasSize(1);
    }
}
