package org.cwowhappy.securityanalyze.shared.exception;

/**
 * 资源不存在异常，用于返回 HTTP 404。
 */
public class NotFoundException extends ApplicationException {

    public NotFoundException(String resourceType, Object identifier) {
        super(String.format("%s not found by identifier: %s", resourceType, identifier));
    }
}
