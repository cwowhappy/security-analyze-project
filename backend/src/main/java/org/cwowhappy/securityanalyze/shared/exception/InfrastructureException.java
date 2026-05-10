package org.cwowhappy.securityanalyze.shared.exception;

/**
 * 基础设施层异常，表示数据库、网络、外部服务等技术故障。
 */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message) {
        super(message);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
