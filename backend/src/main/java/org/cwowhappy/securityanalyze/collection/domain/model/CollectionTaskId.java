package org.cwowhappy.securityanalyze.collection.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * 采集任务ID值对象。
 */
@Getter
@EqualsAndHashCode
public final class CollectionTaskId {

    private final String value;

    private CollectionTaskId(String value) {
        this.value = Objects.requireNonNull(value, "CollectionTaskId cannot be null");
    }

    public static CollectionTaskId of(String value) {
        return new CollectionTaskId(value);
    }

    public static CollectionTaskId generate() {
        return new CollectionTaskId(UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
