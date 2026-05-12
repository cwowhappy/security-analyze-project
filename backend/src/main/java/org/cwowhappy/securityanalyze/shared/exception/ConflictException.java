package org.cwowhappy.securityanalyze.shared.exception;

import lombok.Getter;

import java.util.Map;

/**
 * 资源冲突异常，表示唯一性约束被违反（如用户名/邮箱已存在）。
 */
@Getter
public class ConflictException extends RuntimeException {

    private final Map<String, String> errors;

    public ConflictException(String message) {
        super(message);
        this.errors = null;
    }

    public ConflictException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
