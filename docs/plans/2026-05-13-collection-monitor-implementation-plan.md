# Collection Monitor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 Admin 用户构建数据采集监控中心，支持数据类型覆盖度看板、全量股票基线、任务执行列表。

**Architecture:** 后端新增 2 个 Admin API（`/overview` 和 `/baseline`），前端新增监控页面组件，权限沿用现有 Admin 守卫。数据库 V12 迁移已添加 `mode`/`source_priority` 字段，但 Java 代码尚未同步，需作为前置任务补齐。

**Tech Stack:** Java 21 + Spring Boot 3.5 + Spring Data JDBC, Vue 3.5 + TypeScript + Vite, PostgreSQL, Testcontainers

---

## 前置任务：同步 mode + source_priority 到后端 Java 代码

数据库 V12 已添加 `mode` 和 `source_priority` 字段，但 Java 层的 Domain Model、Entity、DTO、RowMapper、Repository 均未包含这两个字段。必须先补齐，否则任务列表无法正确返回新字段。

### Task 1: 更新 CollectionTask Domain Model

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/domain/model/CollectionTask.java`
- Test: `backend/src/test/java/org/cwowhappy/securityanalyze/collection/domain/model/CollectionTaskTest.java`（若不存在则创建）

**Step 1: Write the failing test**

创建或修改测试：

```java
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
```

**Step 2: Run test to verify it fails**

```bash
cd backend && ./gradlew test --tests "org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskTest"
```

Expected: FAIL with `error: cannot find symbol` (mode/sourcePriority getters not found)

**Step 3: Write minimal implementation**

在 `CollectionTask.java` 中添加字段：

```java
    private String mode;
    private String sourcePriority;
```

**Step 4: Run test to verify it passes**

```bash
cd backend && ./gradlew test --tests "org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskTest"
```

Expected: PASS

**Step 5: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/domain/model/CollectionTask.java backend/src/test/java/org/cwowhappy/securityanalyze/collection/domain/model/CollectionTaskTest.java
git commit -m "feat(domain): add mode and sourcePriority to CollectionTask"
```

---

### Task 2: 更新 CollectionTaskEntity

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/entity/CollectionTaskEntity.java`

**Step 1: Add fields**

在 `CollectionTaskEntity.java` 中添加：

```java
    private String mode;
    private String sourcePriority;
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/entity/CollectionTaskEntity.java
git commit -m "feat(entity): add mode and sourcePriority to CollectionTaskEntity"
```

---

### Task 3: 更新 CollectionTaskRowMapper

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/mapper/CollectionTaskRowMapper.java`

**Step 1: Add mapping**

在 `mapRow` 方法中，在 `setCreatedAt` 之前添加：

```java
        entity.setMode(rs.getString("mode"));
        entity.setSourcePriority(rs.getString("source_priority"));
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/mapper/CollectionTaskRowMapper.java
git commit -m "feat(mapper): map mode and source_priority columns"
```

---

### Task 4: 更新 CollectionTaskDTO

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/dto/CollectionTaskDTO.java`

**Step 1: Add fields**

```java
    private String mode;
    private String sourcePriority;
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/dto/CollectionTaskDTO.java
git commit -m "feat(dto): add mode and sourcePriority to CollectionTaskDTO"
```

---

### Task 5: 更新 JdbcCollectionTaskRepository

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`

**Step 1: Update save SQL**

修改 `save` 方法中的 INSERT SQL，添加 `mode` 和 `source_priority`：

```java
        String sql = """
                INSERT INTO tb_collection_task (
                    id, task_type, mode, source_priority, task_params, status, data_source,
                    total_count, success_count, fail_count,
                    error_message, started_at, completed_at, created_at
                ) VALUES (
                    :id, :taskType, :mode, :sourcePriority::jsonb, :taskParams::jsonb, :status, :dataSource,
                    :totalCount, :successCount, :failCount,
                    :errorMessage, :startedAt, :completedAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    task_type = EXCLUDED.task_type,
                    mode = EXCLUDED.mode,
                    source_priority = EXCLUDED.source_priority,
                    task_params = EXCLUDED.task_params,
                    status = EXCLUDED.status,
                    data_source = EXCLUDED.data_source,
                    total_count = EXCLUDED.total_count,
                    success_count = EXCLUDED.success_count,
                    fail_count = EXCLUDED.fail_count,
                    error_message = EXCLUDED.error_message,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at
                """;
```

修改 `toEntity` 方法：

```java
    private CollectionTaskEntity toEntity(CollectionTask task) {
        CollectionTaskEntity entity = new CollectionTaskEntity();
        entity.setId(task.getId().getValue());
        entity.setTaskType(task.getTaskType());
        entity.setMode(task.getMode());
        entity.setSourcePriority(task.getSourcePriority());
        // ... rest unchanged
        return entity;
    }
```

修改 `toDomain` 方法：

```java
    private CollectionTask toDomain(CollectionTaskEntity entity) {
        return CollectionTask.builder()
                .id(CollectionTaskId.of(entity.getId()))
                .taskType(entity.getTaskType())
                .mode(entity.getMode())
                .sourcePriority(entity.getSourcePriority())
                // ... rest unchanged
                .build();
    }
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java
git commit -m "feat(repo): sync mode and source_priority in save/query"
```

---

### Task 6: 更新 CollectionTaskAppServiceImpl (toDTO)

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java`

**Step 1: Update toDTO**

在 `toDTO` 方法中添加：

```java
        return CollectionTaskDTO.builder()
                // ... existing fields
                .mode(task.getMode())
                .sourcePriority(task.getSourcePriority())
                .build();
```

**Step 2: Run existing tests**

```bash
cd backend && ./gradlew test --tests "org.cwowhappy.securityanalyze.collection.infrastructure.persistence.repository.JdbcCollectionTaskRepositoryTest"
```

Expected: PASS（可能需要更新测试中的 `buildTask` helper 方法以包含 mode 默认值）

检查 `JdbcCollectionTaskRepositoryTest` 中的 `buildTask` 方法，确保它设置 mode 默认值：

```java
    private CollectionTask buildTask(String taskType, String status, String dataSource) {
        return CollectionTask.builder()
                .id(CollectionTaskId.generate())
                .taskType(taskType)
                .mode("full")
                .status(status)
                .dataSource(dataSource)
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }
```

**Step 3: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java
git commit -m "feat(service): include mode and sourcePriority in DTO mapping"
```

---

## 后端监控 API

### Task 7: 创建 CollectionMonitorOverviewDTO

**Files:**
- Create: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/dto/CollectionMonitorOverviewDTO.java`

**Step 1: Create DTO**

```java
package org.cwowhappy.securityanalyze.collection.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 采集监控覆盖度概览 DTO。
 */
@Data
@Builder
public class CollectionMonitorOverviewDTO {

    private String taskType;
    private Long totalCount;
    private Long recentSuccessCount;
    private Long recentExpiredCount;
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/dto/CollectionMonitorOverviewDTO.java
git commit -m "feat(dto): add CollectionMonitorOverviewDTO"
```

---

### Task 8: 创建 CollectionMonitorBaselineDTO

**Files:**
- Create: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/dto/CollectionMonitorBaselineDTO.java`

**Step 1: Create DTO**

```java
package org.cwowhappy.securityanalyze.collection.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 数据基线 DTO。
 */
@Data
@Builder
public class CollectionMonitorBaselineDTO {

    private Long totalStocks;
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/dto/CollectionMonitorBaselineDTO.java
git commit -m "feat(dto): add CollectionMonitorBaselineDTO"
```

---

### Task 9: 更新 CollectionTaskRepository 接口

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/domain/repository/CollectionTaskRepository.java`

**Step 1: Add methods**

```java
    /**
     * 查询各数据类型的采集覆盖度概览。
     */
    List<CollectionTaskOverview> findMonitorOverview(int ttlHours);

    /**
     * 查询全量股票基线总数。
     */
    Long countAllStocks();
```

创建嵌套接口或 POJO 用于返回监控数据：

```java
    /**
     * 监控概览行数据。
     */
    @Data
    class CollectionTaskOverview {
        private String taskType;
        private Long totalCount;
        private Long recentSuccessCount;
        private Long recentExpiredCount;
    }
```

> 注：若项目不允许在接口中定义 `@Data` 类，则创建一个独立的 `CollectionTaskOverview` 类。

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/domain/repository/CollectionTaskRepository.java
git commit -m "feat(repo): add monitor overview and baseline methods to interface"
```

---

### Task 10: 实现 JdbcCollectionTaskRepository 监控查询

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`

**Step 1: Implement findMonitorOverview**

在 `JdbcCollectionTaskRepository` 中添加：

```java
    @Override
    public List<CollectionTaskOverview> findMonitorOverview(int ttlHours) {
        String sql = """
                WITH latest_per_stock AS (
                    SELECT DISTINCT ON (task_type, stock_code)
                        task_type,
                        stock_code,
                        status,
                        updated_at
                    FROM tb_collection_stock_state
                    ORDER BY task_type, stock_code, updated_at DESC
                )
                SELECT
                    task_type,
                    COUNT(*) AS total_count,
                    COUNT(*) FILTER (WHERE status = 'success' AND updated_at > NOW() - INTERVAL '1 hours' * :ttlHours) AS recent_success_count,
                    COUNT(*) FILTER (WHERE status = 'success' AND updated_at <= NOW() - INTERVAL '1 hours' * :ttlHours) AS recent_expired_count
                FROM latest_per_stock
                GROUP BY task_type
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("ttlHours", ttlHours);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            CollectionTaskOverview overview = new CollectionTaskOverview();
            overview.setTaskType(rs.getString("task_type"));
            overview.setTotalCount(rs.getLong("total_count"));
            overview.setRecentSuccessCount(rs.getLong("recent_success_count"));
            overview.setRecentExpiredCount(rs.getLong("recent_expired_count"));
            return overview;
        });
    }

    @Override
    public Long countAllStocks() {
        String sql = "SELECT COUNT(*) FROM tb_stock_basic";
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return count != null ? count : 0L;
    }
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java
git commit -m "feat(repo): implement findMonitorOverview and countAllStocks"
```

---

### Task 11: 创建 CollectionTaskOverview 独立类

如果 Task 9 中无法在接口内定义 `@Data` 类，需要创建独立类：

**Files:**
- Create: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/domain/repository/CollectionTaskOverview.java`

```java
package org.cwowhappy.securityanalyze.collection.domain.repository;

import lombok.Data;

@Data
public class CollectionTaskOverview {
    private String taskType;
    private Long totalCount;
    private Long recentSuccessCount;
    private Long recentExpiredCount;
}
```

然后更新 `CollectionTaskRepository.java` 使用这个类替代内部类。

**Commit:**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/domain/repository/CollectionTaskOverview.java
git commit -m "feat(repo): add CollectionTaskOverview POJO"
```

---

### Task 12: 更新 CollectionTaskAppService 接口

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/CollectionTaskAppService.java`

**Step 1: Add imports and methods**

```java
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorBaselineDTO;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorOverviewDTO;
import java.util.List;

public interface CollectionTaskAppService {
    // ... existing methods

    List<CollectionMonitorOverviewDTO> getMonitorOverview();

    CollectionMonitorBaselineDTO getMonitorBaseline();
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/CollectionTaskAppService.java
git commit -m "feat(service): add monitor methods to app service interface"
```

---

### Task 13: 实现 CollectionTaskAppServiceImpl 监控方法

**Files:**
- Modify: `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java`

**Step 1: Implement methods**

```java
    @Override
    public List<CollectionMonitorOverviewDTO> getMonitorOverview() {
        int ttlHours = 24; // 可从配置注入，这里先硬编码默认值
        return taskRepository.findMonitorOverview(ttlHours).stream()
                .map(o -> CollectionMonitorOverviewDTO.builder()
                        .taskType(o.getTaskType())
                        .totalCount(o.getTotalCount())
                        .recentSuccessCount(o.getRecentSuccessCount())
                        .recentExpiredCount(o.getRecentExpiredCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public CollectionMonitorBaselineDTO getMonitorBaseline() {
        return CollectionMonitorBaselineDTO.builder()
                .totalStocks(taskRepository.countAllStocks())
                .build();
    }
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java
git commit -m "feat(service): implement getMonitorOverview and getMonitorBaseline"
```

---

### Task 14: 创建 AdminCollectionMonitorController

**Files:**
- Create: `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminCollectionMonitorController.java`

**Step 1: Create controller**

```java
package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorBaselineDTO;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorOverviewDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采集监控 Admin REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/collection/monitor")
@RequiredArgsConstructor
public class AdminCollectionMonitorController {

    private final CollectionTaskAppService taskAppService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<CollectionMonitorOverviewDTO>>> getOverview() {
        log.debug("查询采集监控覆盖度概览");
        List<CollectionMonitorOverviewDTO> result = taskAppService.getMonitorOverview();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/baseline")
    public ResponseEntity<ApiResponse<CollectionMonitorBaselineDTO>> getBaseline() {
        log.debug("查询数据基线");
        CollectionMonitorBaselineDTO result = taskAppService.getMonitorBaseline();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminCollectionMonitorController.java
git commit -m "feat(controller): add AdminCollectionMonitorController"
```

---

### Task 15: Repository 集成测试

**Files:**
- Modify: `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`

**Step 1: Add test**

在现有测试类中追加：

```java
    @Test
    @Transactional
    void shouldReturnMonitorOverview() {
        // 插入测试数据到 tb_collection_stock_state
        String insertSql = """
                INSERT INTO tb_collection_stock_state (id, task_id, stock_code, task_type, status, error_message, updated_at)
                VALUES 
                    (:id1, 'task-1', '000001', 'stock_basic', 'success', null, NOW()),
                    (:id2, 'task-1', '000002', 'stock_basic', 'success', null, NOW() - INTERVAL '25 hours'),
                    (:id3, 'task-1', '000003', 'stock_basic', 'failed', null, NOW()),
                    (:id4, 'task-1', '000001', 'company_info', 'success', null, NOW())
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id1", java.util.UUID.randomUUID().toString());
        params.addValue("id2", java.util.UUID.randomUUID().toString());
        params.addValue("id3", java.util.UUID.randomUUID().toString());
        params.addValue("id4", java.util.UUID.randomUUID().toString());
        jdbcTemplate.update(insertSql, params);

        List<CollectionTaskRepository.CollectionTaskOverview> result = collectionTaskRepository.findMonitorOverview(24);

        assertThat(result).hasSize(2);
        var stockBasic = result.stream().filter(r -> r.getTaskType().equals("stock_basic")).findFirst().orElseThrow();
        assertThat(stockBasic.getTotalCount()).isEqualTo(3);
        assertThat(stockBasic.getRecentSuccessCount()).isEqualTo(1);
        assertThat(stockBasic.getRecentExpiredCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldReturnBaselineCount() {
        Long count = collectionTaskRepository.countAllStocks();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
```

> 注意：测试需要注入 `NamedParameterJdbcTemplate` 来插入 stock_state 数据，或者通过已有的 Flyway 数据。

**Step 2: Run test**

```bash
cd backend && ./gradlew test --tests "org.cwowhappy.securityanalyze.collection.infrastructure.persistence.repository.JdbcCollectionTaskRepositoryTest"
```

Expected: PASS

**Step 3: Commit**

```bash
git add backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java
git commit -m "test(repo): add monitor overview and baseline tests"
```

---

### Task 16: Controller MockMvc 测试

**Files:**
- Create: `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminCollectionMonitorControllerTest.java`

**Step 1: Write test**

```java
package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorBaselineDTO;
import org.cwowhappy.securityanalyze.collection.application.dto.CollectionMonitorOverviewDTO;
import org.cwowhappy.securityanalyze.collection.application.service.CollectionTaskAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCollectionMonitorController.class)
class AdminCollectionMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionTaskAppService taskAppService;

    @Test
    void shouldReturnOverview() throws Exception {
        when(taskAppService.getMonitorOverview()).thenReturn(List.of(
                CollectionMonitorOverviewDTO.builder()
                        .taskType("stock_basic")
                        .totalCount(5200L)
                        .recentSuccessCount(5100L)
                        .recentExpiredCount(80L)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/admin/collection/monitor/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskType").value("stock_basic"))
                .andExpect(jsonPath("$.data[0].totalCount").value(5200))
                .andExpect(jsonPath("$.data[0].recentSuccessCount").value(5100))
                .andExpect(jsonPath("$.data[0].recentExpiredCount").value(80));
    }

    @Test
    void shouldReturnBaseline() throws Exception {
        when(taskAppService.getMonitorBaseline()).thenReturn(
                CollectionMonitorBaselineDTO.builder().totalStocks(5200L).build()
        );

        mockMvc.perform(get("/api/v1/admin/collection/monitor/baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalStocks").value(5200));
    }
}
```

**Step 2: Run test**

```bash
cd backend && ./gradlew test --tests "org.cwowhappy.securityanalyze.interfaces.rest.controller.AdminCollectionMonitorControllerTest"
```

Expected: PASS

**Step 3: Commit**

```bash
git add backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminCollectionMonitorControllerTest.java
git commit -m "test(controller): add AdminCollectionMonitorController tests"
```

---

## 前端部分

### Task 17: 更新 CollectionTask 类型

**Files:**
- Modify: `frontend/src/types/collection.ts`

**Step 1: Add fields**

```typescript
export interface CollectionTask {
  id: string
  taskType: string
  mode: 'full' | 'single' | null
  sourcePriority: string | null
  taskParams: Record<string, unknown> | null
  status: 'pending' | 'running' | 'success' | 'failed'
  dataSource: string | null
  totalCount: number
  successCount: number
  failCount: number
  errorMessage: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}
```

**Step 2: Commit**

```bash
git add frontend/src/types/collection.ts
git commit -m "feat(types): add mode and sourcePriority to CollectionTask"
```

---

### Task 18: 创建监控相关类型

**Files:**
- Create: `frontend/src/types/monitor.ts`

**Step 1: Create types**

```typescript
export interface CollectionMonitorOverview {
  taskType: string
  totalCount: number
  recentSuccessCount: number
  recentExpiredCount: number
}

export interface CollectionMonitorBaseline {
  totalStocks: number
}
```

**Step 2: Commit**

```bash
git add frontend/src/types/monitor.ts
git commit -m "feat(types): add monitor overview and baseline types"
```

---

### Task 19: 创建 adminCollectionMonitor API 模块

**Files:**
- Create: `frontend/src/api/modules/adminCollectionMonitor.ts`

**Step 1: Create API module**

```typescript
import { http } from '@/utils/request'
import type { CollectionMonitorOverview, CollectionMonitorBaseline } from '@/types/monitor'

const PREFIX = '/api/v1/admin/collection/monitor'

export const adminCollectionMonitorApi = {
  /** 查询采集覆盖度概览 */
  getOverview: () => http.get<CollectionMonitorOverview[]>(`${PREFIX}/overview`),

  /** 查询数据基线 */
  getBaseline: () => http.get<CollectionMonitorBaseline>(`${PREFIX}/baseline`),
}
```

**Step 2: Commit**

```bash
git add frontend/src/api/modules/adminCollectionMonitor.ts
git commit -m "feat(api): add admin collection monitor API module"
```

---

### Task 20: 创建 CollectionMonitorView 组件

**Files:**
- Create: `frontend/src/views/admin/collection/CollectionMonitorView.vue`

由于组件代码较长（约 300 行），此处给出核心骨架，实施时补全样式：

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminCollectionMonitorApi } from '@/api/modules/adminCollectionMonitor'
import { collectionTaskApi } from '@/api/modules/collection'
import type { CollectionMonitorOverview, CollectionMonitorBaseline } from '@/types/monitor'
import type { CollectionTask } from '@/types/collection'

const overview = ref<CollectionMonitorOverview[]>([])
const baseline = ref<CollectionMonitorBaseline | null>(null)
const tasks = ref<CollectionTask[]>([])
const taskTotal = ref(0)
const taskPage = ref(1)
const taskSize = ref(10)
const loading = ref(false)

const taskTypeLabelMap: Record<string, string> = {
  stock_basic: '股票基础信息',
  company_info: '公司信息',
  financial_income: '利润表',
  financial_balance: '资产负债表',
  financial_cashflow: '现金流量表',
  financial_indicator: '财务指标',
  financial_full: '财务全量',
}

function coveragePct(item: CollectionMonitorOverview) {
  if (!item.totalCount) return 0
  return Math.round((item.recentSuccessCount / item.totalCount) * 100)
}

function statusText(s: string) {
  const map: Record<string, string> = {
    success: '成功', running: '执行中', pending: '待执行', failed: '失败',
  }
  return map[s] || s
}

function statusClass(s: string) {
  const map: Record<string, string> = {
    success: 'tag-success',
    running: 'tag-running',
    pending: 'tag-pending',
    failed: 'tag-failed',
  }
  return map[s] || 'tag-pending'
}

function formatTime(t: string | null) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function fetchOverview() {
  try {
    overview.value = await adminCollectionMonitorApi.getOverview()
    baseline.value = await adminCollectionMonitorApi.getBaseline()
  } catch (e) {
    console.error('加载监控数据失败', e)
  }
}

async function fetchTasks() {
  loading.value = true
  try {
    const result = await collectionTaskApi.list({ page: taskPage.value, size: taskSize.value })
    tasks.value = result.list ?? []
    taskTotal.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (taskPage.value > 1) {
    taskPage.value--
    fetchTasks()
  }
}

function nextPage() {
  if (tasks.value.length === taskSize.value) {
    taskPage.value++
    fetchTasks()
  }
}

onMounted(() => {
  fetchOverview()
  fetchTasks()
})
</script>

<template>
  <div>
    <div class="pg-hd">
      <h1 class="pg-t">采集监控</h1>
      <p class="pg-d">查看数据采集覆盖度、任务执行状态与历史记录</p>
    </div>

    <!-- 数据基线卡 -->
    <div class="stat-row">
      <div class="stat-card baseline">
        <div class="stat-n">{{ baseline?.totalStocks ?? '-' }}</div>
        <div class="stat-l">数据基线 · 系统股票总数</div>
      </div>
    </div>

    <!-- 采集覆盖度卡 -->
    <div class="overview-grid">
      <div v-for="item in overview" :key="item.taskType" class="overview-card">
        <div class="oc-title">{{ taskTypeLabelMap[item.taskType] || item.taskType }}</div>
        <div class="oc-metrics">
          <div class="oc-m">
            <div class="oc-v">{{ item.totalCount.toLocaleString() }}</div>
            <div class="oc-l">总量</div>
          </div>
          <div class="oc-m">
            <div class="oc-v" style="color:var(--success)">{{ item.recentSuccessCount.toLocaleString() }}</div>
            <div class="oc-l">成功未过期</div>
          </div>
          <div class="oc-m">
            <div class="oc-v" style="color:var(--warning)">{{ item.recentExpiredCount.toLocaleString() }}</div>
            <div class="oc-l">成功已过期</div>
          </div>
        </div>
        <div class="oc-bar">
          <div class="oc-fill" :style="{ width: coveragePct(item) + '%' }"></div>
        </div>
        <div class="oc-pct">覆盖率 {{ coveragePct(item) }}%</div>
      </div>
      <div v-if="!overview.length" class="empty">暂无采集记录</div>
    </div>

    <!-- 任务执行列表 -->
    <div class="section-title">任务执行列表</div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="task-table">
      <div class="task-row head">
        <span>任务类型</span>
        <span>模式</span>
        <span>数据源优先级</span>
        <span>状态</span>
        <span>总数</span>
        <span>成功</span>
        <span>失败</span>
        <span>开始时间</span>
      </div>
      <div v-for="task in tasks" :key="task.id" class="task-row">
        <span>{{ taskTypeLabelMap[task.taskType] || task.taskType }}</span>
        <span>
          <span :class="['mode-tag', task.mode === 'single' ? 'mode-single' : 'mode-full']">
            {{ task.mode === 'single' ? '单条' : '全量' }}
          </span>
        </span>
        <span :title="task.sourcePriority ?? '-'">{{ task.sourcePriority ? JSON.parse(task.sourcePriority).join(' > ') : '-' }}</span>
        <span><span :class="['tag', statusClass(task.status)]">{{ statusText(task.status) }}</span></span>
        <span>{{ task.totalCount }}</span>
        <span style="color:var(--success)">{{ task.successCount }}</span>
        <span style="color:var(--danger)">{{ task.failCount }}</span>
        <span class="time">{{ formatTime(task.startedAt) }}</span>
      </div>
      <div v-if="!tasks.length" class="empty">无任务记录</div>
    </div>

    <div class="pagination" v-if="tasks.length">
      <button :disabled="taskPage === 1" @click="prevPage">上一页</button>
      <span>第 {{ taskPage }} 页</span>
      <button :disabled="tasks.length < taskSize" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.pg-hd { margin-bottom: 24px; }
.pg-t { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.pg-d { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.stat-row { margin-bottom: 20px; }
.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  display: inline-block;
  min-width: 200px;
}
.stat-n { font-size: 28px; font-weight: 700; color: var(--primary); }
.stat-l { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.overview-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: var(--shadow-sm);
}
.oc-title { font-size: 14px; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; }
.oc-metrics { display: flex; gap: 16px; margin-bottom: 12px; }
.oc-m { flex: 1; text-align: center; }
.oc-v { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.oc-l { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.oc-bar { height: 6px; background: var(--border); border-radius: 3px; overflow: hidden; }
.oc-fill { height: 100%; background: var(--success); border-radius: 3px; transition: width 0.3s; }
.oc-pct { font-size: 11px; color: var(--text-muted); margin-top: 6px; text-align: right; }

.section-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; }

.task-table { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.task-row { display: grid; grid-template-columns: 1.5fr 0.8fr 1.5fr 0.8fr 0.6fr 0.6fr 0.6fr 1.2fr; gap: 8px; padding: 10px 14px; align-items: center; font-size: 13px; }
.task-row.head { background: var(--bg); font-weight: 700; color: var(--text-secondary); border-bottom: 1px solid var(--border); }
.task-row:not(.head) { border-bottom: 1px solid var(--border); }
.task-row:not(.head):hover { background: var(--surface-hover); }

.tag { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700; }
.tag-success { background: rgba(0,217,36,0.1); color: var(--success); }
.tag-running { background: rgba(99,91,255,0.1); color: var(--primary); }
.tag-pending { background: var(--surface-hover); color: var(--text-muted); border: 1px solid var(--border); }
.tag-failed { background: rgba(255,59,48,0.1); color: var(--danger); }

.mode-tag { display: inline-flex; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700; }
.mode-full { background: var(--surface-hover); color: var(--text-secondary); border: 1px solid var(--border); }
.mode-single { background: rgba(99,91,255,0.1); color: var(--primary); }

.time { color: var(--text-muted); font-size: 12px; }
.empty { text-align: center; padding: 40px; color: var(--text-muted); }
.loading { text-align: center; padding: 40px; color: var(--text-muted); }
.pagination { display: flex; justify-content: center; align-items: center; gap: 6px; margin-top: 16px; }
.pagination button { padding: 6px 14px; border: 1px solid var(--border); background: var(--surface); color: var(--text-secondary); border-radius: var(--radius-md); cursor: pointer; font-size: 13px; }
.pagination button:hover:not(:disabled) { background: var(--surface-hover); }
.pagination button:disabled { opacity: 0.3; cursor: not-allowed; }
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/views/admin/collection/CollectionMonitorView.vue
git commit -m "feat(ui): add CollectionMonitorView component"
```

---

### Task 21: 更新路由

**Files:**
- Modify: `frontend/src/router/index.ts`

**Step 1: Add route**

在 Admin children 中添加：

```typescript
      {
        path: 'collection-monitor',
        name: 'AdminCollectionMonitor',
        component: () => import('@/views/admin/collection/CollectionMonitorView.vue'),
        meta: { title: '采集监控' },
      },
```

**Step 2: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "feat(router): add /admin/collection-monitor route"
```

---

### Task 22: 更新 AdminLayout 导航

**Files:**
- Modify: `frontend/src/views/admin/AdminLayout.vue`

**Step 1: Add menu item**

在 `menuItems` 数组中添加：

```typescript
const menuItems = [
  { path: '/admin/users', label: '👥 用户管理', name: 'AdminUsers' },
  { path: '/admin/login-logs', label: '📝 登录日志', name: 'AdminLoginLogs' },
  { path: '/admin/collection-monitor', label: '📊 采集监控', name: 'AdminCollectionMonitor' },
]
```

**Step 2: Commit**

```bash
git add frontend/src/views/admin/AdminLayout.vue
git commit -m "feat(ui): add collection monitor to admin sidebar"
```

---

### Task 23: 更新 CollectionTaskListView

**Files:**
- Modify: `frontend/src/views/collection/CollectionTaskListView.vue`

**Step 1: Update taskTypeOptions**

替换现有选项为新的 task_type：

```typescript
const taskTypeOptions = [
  { label: '股票基础信息', value: 'stock_basic' },
  { label: '公司信息', value: 'company_info' },
  { label: '利润表', value: 'financial_income' },
  { label: '资产负债表', value: 'financial_balance' },
  { label: '现金流量表', value: 'financial_cashflow' },
  { label: '财务指标', value: 'financial_indicator' },
  { label: '财务全量', value: 'financial_full' },
]
```

**Step 2: Update typeText function**

```typescript
function typeText(t: string) {
  const map: Record<string, string> = {
    stock_basic: '股票基础信息',
    company_info: '公司信息',
    financial_income: '利润表',
    financial_balance: '资产负债表',
    financial_cashflow: '现金流量表',
    financial_indicator: '财务指标',
    financial_full: '财务全量',
  }
  return map[t] || t
}
```

**Step 3: Update task card to show mode and sourcePriority**

在模板中的 `tmeta` div 内添加 mode 和 sourcePriority 显示。

**Step 4: Commit**

```bash
git add frontend/src/views/collection/CollectionTaskListView.vue
git commit -m "feat(ui): update task type labels and display mode/sourcePriority"
```

---

### Task 24: 运行全部测试并验证

**Step 1: Run backend tests**

```bash
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL

**Step 2: Run frontend tests**

```bash
cd frontend && npm run test
```

Expected: 所有测试通过

**Step 3: Verify build**

```bash
cd backend && ./gradlew build
cd frontend && npm run build
```

Expected: Both builds succeed

**Step 4: Commit**

```bash
git add -A
git commit -m "test: verify all tests pass after collection monitor implementation"
```

---

## 后续可扩展项（本次不实现）

- 按任务类型筛选的覆盖度趋势图（echarts）
- 自动刷新看板数据（WebSocket 或轮询）
- 任务详情页展示 stock 级明细（需要新增 `/tasks/{id}/stock-states` API）
