package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.user.domain.model.User;

/**
 * 管理员用户管理应用服务接口。
 */
public interface AdminUserAppService {

    PageResult<User> listUsers(String keyword, String role, Boolean emailVerified,
                                Boolean locked, int page, int size);

    User getUserDetail(String userId);

    void updateUser(String userId, String displayName, String role);

    void unlockUser(String userId);

    void forcePasswordReset(String userId, String reason);
}
