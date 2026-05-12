package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.user.application.service.AdminLogAppService;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.cwowhappy.securityanalyze.user.domain.repository.LoginLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员日志查询应用服务实现。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLogAppServiceImpl implements AdminLogAppService {

    private final LoginLogRepository loginLogRepository;

    @Override
    public PageResult<LoginLog> queryLogs(String userId, String action, LocalDateTime startDate,
                                           LocalDateTime endDate, int page, int size) {
        List<LoginLog> list = loginLogRepository.findByConditions(userId, action, startDate, endDate, page, size);
        long total = loginLogRepository.countByConditions(userId, action, startDate, endDate);
        return PageResult.<LoginLog>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    @Override
    public List<LoginLog> exportLogs(String userId, String action, LocalDateTime startDate, LocalDateTime endDate) {
        return loginLogRepository.findAllByConditions(userId, action, startDate, endDate);
    }
}
