package org.cwowhappy.securityanalyze.collection.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * 采集任务调度ID值对象。
 */
@Getter
@EqualsAndHashCode
public final class CollectionTaskScheduleId {

    private final String value;

    private CollectionTaskScheduleId(String value) {
        this.value = Objects.requireNonNull(value, "CollectionTaskScheduleId cannot be null");
    }

    public static CollectionTaskScheduleId of(String value) {
        return new CollectionTaskScheduleId(value);
    }

    public static CollectionTaskScheduleId generate() {
        return new CollectionTaskScheduleId(UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
