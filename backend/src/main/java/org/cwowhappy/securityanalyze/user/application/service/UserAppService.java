package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;

import java.util.Optional;

/**
 * 用户应用服务接口。
 */
public interface UserAppService {

    UserDTO findById(String id);

    Optional<UserDTO> findByUsername(String username);

    Optional<UserDTO> findByEmail(String email);

    boolean isUsernameAvailable(String username);

    boolean isEmailAvailable(String email);
}
