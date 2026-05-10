package org.cwowhappy.securityanalyze.shared.exception;

/**
 * 领域层业务异常，表示业务规则被违反。
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
