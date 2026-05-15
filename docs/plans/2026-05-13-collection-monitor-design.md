# 采集监控模块重构设计

> 日期：2026-05-13  
> 范围：前端 (`frontend/`) + 后端 (`backend/`)  
> 目标：基于 v3.0 通用采集管道，为 Admin 用户构建数据采集监控中心

---

## 1. 背景与目标

`collector` 模块已重构为 v3.0 通用采集管道，新增 `tb_collection_stock_state` 表记录每只股票在每个数据类型下的采集状态。本次重构旨在为管理员提供可视化的采集监控能力：

- **数据类型覆盖度看板**：按 `task_type` 跨任务去重统计（去重字段 `stock_code`）
- **全量股票基线**：展示当前系统管理的股票总数
- **任务执行列表**：复用现有分页，扩展 `mode` 和 `source_priority` 展示

---

## 2. 架构总览

```
前端 (Vue 3)
  └─ /admin/collection-monitor
      ├─ 统计看板区（覆盖度卡片 + 基线卡片）
      └─ 任务执行列表（复用现有分页）
              ↓
后端 (Spring Boot + JDBC)
  ├─ GET /api/v1/admin/collection/monitor/overview
  ├─ GET /api/v1/admin/collection/monitor/baseline
  └─ GET /api/v1/collection/tasks (现有，复用)
              ↓
PostgreSQL
  ├─ tb_collection_stock_state (覆盖度统计来源)
  └─ tb_stock_basic (基线统计来源)
```

**权限**：沿用现有 Admin 路由守卫（`requiresAdmin: true`），未登录或非管理员自动拦截。

---

## 3. 后端 API 设计

### 3.1 采集覆盖度概览

```http
GET /api/v1/admin/collection/monitor/overview
```

**响应示例：**

```json
{
  "data": [
    {
      "taskType": "stock_basic",
      "totalCount": 5200,
      "recentSuccessCount": 5100,
      "recentExpiredCount": 80
    },
    {
      "taskType": "financial_income",
      "totalCount": 4800,
      "recentSuccessCount": 4500,
      "recentExpiredCount": 200
    }
  ]
}
```

**3 个指标定义：**

| 指标 | 定义 |
|------|------|
| `totalCount` | 该 `task_type` 下所有 `stock_code` 去重后的总数（无论状态） |
| `recentSuccessCount` | 该 `task_type` 下，每只股票的最新记录 `status='success'` 且 `updated_at > NOW() - ttl_hours` |
| `recentExpiredCount` | 该 `task_type` 下，每只股票的最新记录 `status='success'` 且 `updated_at <= NOW() - ttl_hours` |

**SQL 实现：**

```sql
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
    COUNT(*) FILTER (WHERE status = 'success' AND updated_at > NOW() - INTERVAL '24 hours') AS recent_success_count,
    COUNT(*) FILTER (WHERE status = 'success' AND updated_at <= NOW() - INTERVAL '24 hours') AS recent_expired_count
FROM latest_per_stock
GROUP BY task_type;
```

> TTL 默认 24 小时，由 `COLLECTION_TTL_HOURS` 配置传入。按任务类型差异化配置时，取 YAML 中的 `ttl_hours`。

### 3.2 数据基线

```http
GET /api/v1/admin/collection/monitor/baseline
```

**响应示例：**

```json
{
  "totalStocks": 5200
}
```

**SQL：**

```sql
SELECT COUNT(*) AS total_stocks FROM tb_stock_basic;
```

### 3.3 任务列表（复用现有）

```http
GET /api/v1/collection/tasks?page={page}&size={size}&status={status}&taskType={taskType}
```

响应格式保持不变，但需扩展返回字段：`mode`、`sourcePriority`。

---

## 4. 数据库变更

无需新增表。现有表已满足需求：

- `tb_collection_stock_state` — 覆盖度统计来源
- `tb_stock_basic` — 基线统计来源

**索引优化建议**（若数据量大时）：

```sql
-- 已存在，确认覆盖
-- CREATE INDEX idx_collection_stock_state_lookup ON tb_collection_stock_state(task_id, stock_code, task_type);
-- CREATE INDEX idx_collection_stock_state_updated ON tb_collection_stock_state(updated_at);
```

---

## 5. 前端页面设计

### 5.1 路由

```typescript
{
  path: '/admin/collection-monitor',
  name: 'collection-monitor',
  component: () => import('@/views/admin/collection/CollectionMonitorView.vue'),
  meta: { requiresAdmin: true }
}
```

Admin 侧边栏新增导航项："采集监控"。

### 5.2 页面布局

上半部分 — **统计看板区**：

- **数据基线卡**
  - 标题："数据基线"
  - 大数字：`totalStocks`
  - 副标题："当前系统管理的股票总数"

- **采集覆盖度卡**（每个 `task_type` 一张卡片，横向排列）
  - 标题：`task_type` 中文名（如"股票基础信息"、"利润表"）
  - 三列指标：
    - 总量（灰色数字）
    - 成功未过期（绿色数字）
    - 成功已过期（橙色数字）
  - 底部进度条：`recentSuccessCount / totalCount` 作为覆盖率百分比

下半部分 — **任务执行列表**：

- 表格列：`taskType`、`mode`（full/single 标签）、`sourcePriority`（逗号分隔字符串）、`status`（状态徽章）、`dataSource`、`totalCount`、`successCount`、`failCount`、`startedAt`
- 分页：复用现有分页组件
- 每行"详情"链接跳转至 `/collection/tasks/:id`

### 5.3 数据流

1. `onMounted`：并行请求 `/overview` 和 `/baseline`
2. 看板渲染完成后，自动加载任务列表第一页
3. 可选：每 30 秒自动刷新看板数据

---

## 6. 错误处理

| 场景 | 处理策略 |
|------|----------|
| `tb_collection_stock_state` 为空 | `/overview` 返回 `[]`，前端显示"暂无采集记录"占位 |
| 大量记录导致 SQL 慢 | 依赖现有索引；若仍慢，增加 JVM 层 5 分钟缓存 |
| Admin 权限不足 | 现有路由守卫拦截，返回 403 |

---

## 7. 测试策略

- **后端单元测试**：`JdbcCollectionTaskRepositoryTest` 新增 `findMonitorOverview()` 测试；`JdbcStockRepositoryTest` 新增 `countAll()` 测试
- **后端集成测试**：`CollectionTaskControllerTest` 新增 MockMvc 测试，验证 `/overview`、`/baseline` 响应格式和权限拦截
- **前端组件测试**：`CollectionMonitorView` Vitest 测试，mock API 响应，验证卡片渲染、进度条计算、空数据占位

---

## 8. 实施阶段建议

### 阶段一：后端 API（0.5 天）
- DTO：`CollectionMonitorOverviewDTO`、`CollectionMonitorBaselineDTO`
- Repository：新增 `findMonitorOverview()`、`countAllStocks()`
- Controller：`AdminCollectionMonitorController`
- 单元测试

### 阶段二：前端页面（0.5 天）
- 新增 `CollectionMonitorView.vue`
- Admin 侧边栏添加导航
- API 模块新增 `adminCollectionMonitor.ts`
- 组件测试

### 阶段三：联调与验收（0.5 天）
- 本地 PostgreSQL 插入测试数据验证 SQL
- 前后端联调
- Admin 权限验证
