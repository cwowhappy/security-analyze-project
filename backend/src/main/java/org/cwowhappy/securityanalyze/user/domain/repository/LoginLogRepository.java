package org.cwowhappy.securityanalyze.user.domain.repository;

import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志领域仓库接口。
 */
public interface LoginLogRepository {

    void save(LoginLog loginLog);

    List<LoginLog> findByConditions(String userId, String action, LocalDateTime startDate,
                                     LocalDateTime endDate, int page, int size);

    long countByConditions(String userId, String action, LocalDateTime startDate, LocalDateTime endDate);

    List<LoginLog> findAllByConditions(String userId, String action, LocalDateTime startDate,
                                        LocalDateTime endDate);
}
