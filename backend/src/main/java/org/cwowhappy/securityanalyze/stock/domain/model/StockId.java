package org.cwowhappy.securityanalyze.stock.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * 股票ID值对象。
 */
@Getter
@EqualsAndHashCode
public final class StockId {

    private final String value;

    private StockId(String value) {
        this.value = Objects.requireNonNull(value, "StockId cannot be null");
    }

    public static StockId of(String value) {
        return new StockId(value);
    }

    public static StockId generate() {
        return new StockId(UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
