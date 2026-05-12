package org.cwowhappy.securityanalyze.user.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 用户 ID 值对象。
 */
@Getter
@EqualsAndHashCode
public final class UserId {

    private final String value;

    private UserId(String value) {
        this.value = value;
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
