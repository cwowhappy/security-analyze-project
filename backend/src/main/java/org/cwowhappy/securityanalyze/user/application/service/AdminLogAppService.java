package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员日志查询应用服务接口。
 */
public interface AdminLogAppService {

    PageResult<LoginLog> queryLogs(String userId, String action, LocalDateTime startDate,
                                    LocalDateTime endDate, int page, int size);

    List<LoginLog> exportLogs(String userId, String action, LocalDateTime startDate, LocalDateTime endDate);
}
