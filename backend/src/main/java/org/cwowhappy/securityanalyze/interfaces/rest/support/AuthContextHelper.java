package org.cwowhappy.securityanalyze.interfaces.rest.support;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.shared.exception.UnauthorizedException;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.AuthAppService;
import org.springframework.stereotype.Component;

/**
 * 认证上下文辅助工具，从 HTTP 请求中解析当前登录用户。
 */
@Component
@RequiredArgsConstructor
public class AuthContextHelper {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthAppService authAppService;

    /**
     * 从请求中获取当前登录用户。
     *
     * @param request HTTP 请求
     * @return 当前用户 DTO
     * @throws UnauthorizedException 未认证或 Token 无效
     */
    public UserDTO getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少有效的认证令牌");
        }
        String token = authHeader.substring(7);
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        return authAppService.getCurrentUser(userId);
    }
}
