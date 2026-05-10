package org.cwowhappy.securityanalyze.company.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * 公司ID值对象。
 */
@Getter
@EqualsAndHashCode
public final class CompanyId {

    private final String value;

    private CompanyId(String value) {
        this.value = Objects.requireNonNull(value, "CompanyId cannot be null");
    }

    public static CompanyId of(String value) {
        return new CompanyId(value);
    }

    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
