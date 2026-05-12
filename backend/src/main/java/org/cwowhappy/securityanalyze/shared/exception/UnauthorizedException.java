package org.cwowhappy.securityanalyze.shared.exception;

/**
 * 未授权异常，表示认证失败或 Token 无效。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
