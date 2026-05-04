package com.example.securityanalyze.user.application;

import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.infrastructure.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 验证用户名密码，返回认证后的用户对象
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 认证通过的用户
     * @throws BadCredentialsException 用户名或密码错误
     */
    public User authenticate(String username, String password) {
        log.debug("执行用户认证, username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("认证失败, 用户名不存在, username={}", username);
                    return new BadCredentialsException("用户名或密码错误");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("认证失败, 密码错误, username={}", username);
            throw new BadCredentialsException("用户名或密码错误");
        }

        log.debug("用户认证成功, username={}", username);
        return user;
    }

    /**
     * 生成 JWT Token
     *
     * @param user 用户
     * @return JWT Token
     */
    public String generateToken(User user) {
        return jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
    }
}
