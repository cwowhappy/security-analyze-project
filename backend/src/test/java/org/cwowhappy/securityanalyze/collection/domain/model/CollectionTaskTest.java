package org.cwowhappy.securityanalyze.collection.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionTaskTest {
    @Test
    void shouldCreateTaskWithModeAndSourcePriority() {
        CollectionTask task = CollectionTask.builder()
                .id(CollectionTaskId.generate())
                .taskType("stock_basic")
                .mode("full")
                .sourcePriority("[\"akshare\",\"tushare\"]")
                .status("pending")
                .build();
        assertThat(task.getMode()).isEqualTo("full");
        assertThat(task.getSourcePriority()).isEqualTo("[\"akshare\",\"tushare\"]");
    }
}
