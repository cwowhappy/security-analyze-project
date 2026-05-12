package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.shared.exception.NotFoundException;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.UserAppService;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户应用服务实现。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAppServiceImpl implements UserAppService {

    private final UserRepository userRepository;

    @Override
    public UserDTO findById(String id) {
        User user = userRepository.findById(UserId.of(id))
                .orElseThrow(() -> new NotFoundException("User", id));
        return toDTO(user);
    }

    @Override
    public Optional<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toDTO);
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDTO);
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId().getValue())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarInitial(user.getAvatarInitial())
                .build();
    }
}
