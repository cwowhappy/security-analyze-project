package org.cwowhappy.securityanalyze.shared.exception;

/**
 * 应用层异常，表示参数错误、状态不符等应用级问题。
 */
public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
