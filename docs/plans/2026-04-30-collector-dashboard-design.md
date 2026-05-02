# 数据采集管理看板设计

## 1. 概述与方案选择

### 1.1 需求背景
构建一个数据采集管理看板，支持查看各项数据的采集情况，范围限定为**任务执行状态 + 数据量概览**，交互为**纯只读展示**。

### 1.2 架构方案对比

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| A：直接查库 | 采集器写入 PG 监控表，后端直接查询 | 无额外组件，与现有架构一致，成本最低 | 无缓存，高频查询有轻微 DB 压力 |
| B：采集器内部 API | Python 采集器内嵌 HTTP 服务 | 状态实时性最强 | 采集器变重，增加网络故障点 |
| C：监控表 + Redis | 方案 A 基础上增加 Redis 缓存 | 性能最好 | 引入 Redis，项目初期过度设计 |

**最终选择方案 A（直接查库）**。纯只读看板、刷新频率最低 30 秒、数据量极小，PostgreSQL 完全胜任。

### 1.3 核心设计决策
- **布局**：混合视图 — 顶部概览卡片 + 底部任务执行列表
- **刷新**：用户自选间隔（30 秒 / 1 分钟 / 5 分钟 / 手动）
- **任务列表**：默认展示最近 7 天数据
- **数据类型**：`company`（公司基本信息）、`security`（上市证券信息）、`finance_report`（财务报告）

---

## 2. 数据模型与存储设计

新增两张 PG 表，均由 Python 采集器维护写入。

### 2.1 collector_task_log — 采集任务执行日志

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGSERIAL PK | 自增主键 |
| `task_name` | VARCHAR(64) | 任务标识，如 `sync_company`、`sync_finance_report` |
| `task_type` | VARCHAR(32) | 数据类型：`company` / `security` / `finance_report` |
| `started_at` | TIMESTAMP | 任务开始时间 |
| `ended_at` | TIMESTAMP | 任务结束时间（运行中可为 NULL） |
| `status` | VARCHAR(16) | `running` / `success` / `failed` |
| `rows_affected` | INT | 影响行数 |
| `error_message` | TEXT | 失败原因（成功为 NULL） |
| `created_at` | TIMESTAMP DEFAULT now() | 记录创建时间 |

**索引**：`idx_task_log_started_at` (`started_at`)，`idx_task_log_task_type` (`task_type`)，`idx_task_log_status` (`status`)

### 2.2 collector_data_status — 各数据类型当前快照

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGSERIAL PK | 自增主键 |
| `data_type` | VARCHAR(32) UNIQUE | 数据类型 |
| `total_rows` | INT | 当前总条数 |
| `last_updated_at` | TIMESTAMP | 数据最后更新时间 |
| `last_task_id` | BIGINT | 关联最近一次任务日志 |
| `created_at` | TIMESTAMP DEFAULT now() | 记录创建时间 |
| `updated_at` | TIMESTAMP DEFAULT now() | 记录更新时间 |

**索引**：`idx_data_status_type` (`data_type`)

### 2.3 写入时序
采集器每次任务完成后：
1. 先写 `collector_task_log`
2. 再 `UPSERT` `collector_data_status`

两张表天然支持看板的"概览卡片 + 任务列表"混合视图，无需复杂聚合。

---

## 3. 后端 API 设计

新增 `CollectorDashboardController`，提供两个只读接口。

### 3.1 概览接口

```
GET /api/collector/dashboard/overview
```

返回顶部概览卡片数据，直接查询 `collector_data_status` 左关联最近一次任务日志。

**响应示例**：
```json
{
  "data": [
    {
      "dataType": "company",
      "dataTypeLabel": "公司基本信息",
      "totalRows": 5200,
      "lastUpdatedAt": "2026-04-30T03:00:00",
      "lastTaskStatus": "success",
      "lastTaskDurationSeconds": 125
    }
  ]
}
```

### 3.2 任务列表接口

```
GET /api/collector/dashboard/tasks?dataType=&status=&page=1&size=20
```

返回底部任务列表，默认过滤 `started_at >= now() - interval '7 days'`。支持按数据类型和状态筛选，分页返回。

**响应示例**：
```json
{
  "data": [
    {
      "id": 1,
      "taskName": "sync_company",
      "taskType": "company",
      "startedAt": "2026-04-30T03:00:00",
      "endedAt": "2026-04-30T03:02:05",
      "status": "success",
      "rowsAffected": 5200,
      "durationSeconds": 125
    }
  ],
  "total": 150,
  "page": 1,
  "size": 20
}
```

### 3.3 后端分层
遵循现有规范：
- `api` 层：定义 DTO 与 Controller
- `application` 层：编排查询逻辑
- `infrastructure` 层：编写 SQL（MyBatis / JPA）

由于纯只读，无事务与领域规则，无需 `domain` 层实体，直接用 POJO / Record 透传。

---

## 4. 前端布局与组件设计

新增路由 `/dashboard/collector`，页面组件 `CollectorDashboardView.vue`，整体采用 Element Plus 的 `el-card` 与 `el-table`。

### 4.1 顶部区域：刷新控制 + 概览卡片
- 页面标题右侧放置 `el-select`（刷新间隔：30秒 / 1分钟 / 5分钟 / 手动），默认 1 分钟
- 下方 `el-row :gutter="16"` 排列概览卡片，每种数据类型一张
- 每张卡片展示：中文名称（如"公司基本信息"）、总条数（大字号）、最后更新时间、最近一次任务状态（`el-tag`：绿色 success / 红色 failed / 蓝色 running）、耗时
- 卡片整体可点击，点击后自动筛选下方任务列表到该数据类型

### 4.2 底部区域：任务列表
- `el-table` 展示最近 7 天任务，列：任务名称、数据类型（中文映射）、开始时间、结束时间、状态 Tag、影响行数、耗时
- 表头上方放置筛选栏：`el-select`（数据类型）+ `el-select`（状态：全部/成功/失败/运行中）
- 表格下方 `el-pagination`，默认每页 20 条

### 4.3 组件拆分
| 组件 | 职责 |
|------|------|
| `CollectorDashboardView.vue` | 页面容器，管理轮询定时器与筛选状态联动 |
| `OverviewCards.vue` | 概览卡片纯展示组件 |
| `TaskList.vue` | 表格、筛选、分页 |

### 4.4 数据刷新机制
- 页面挂载时发起首次请求（概览 + 任务列表）
- 根据用户选择的间隔启动 `setInterval`，同时请求两个接口
- 组件 `onUnmounted` 时 `clearInterval`，切换标签页不中断

---

## 5. 数据流、采集器改造与错误处理

### 5.1 采集器改造
新增轻量监控模块 `collector/monitor.py`，提供两个钩子函数：
- `log_task_start(task_name, task_type)` — 向 `collector_task_log` 插入 running 记录，返回 task_id
- `log_task_end(task_id, status, rows_affected, error_message)` — 更新结束时间、状态、行数；同时 `UPSERT` `collector_data_status` 表

**关键原则**：监控写入必须用 `try/except` 包裹，任何异常只打印日志，绝不能阻断主采集流程。

### 5.2 前端数据流
```
前端(setInterval轮询) → 后端(直接查询PG) → 返回JSON
                              ↑
采集器(任务完成后写入) ────────┘
```

### 5.3 错误处理
- **后端查询失败**：正常返回 HTTP 错误，前端显示 `el-empty` 或错误提示
- **采集器写监控表失败**：`try/except` 包裹，失败只打日志不抛异常
- **前端轮询失败**：静默重试，连续失败 3 次后顶部弹出 `ElMessage.error("数据刷新异常，请检查网络")`
- **筛选条件变更**：立即重置分页并重新请求

### 5.4 数据库迁移
新增 `backend/src/main/resources/db/migration/V2__create_collector_monitor_tables.sql`，包含两张表及索引。

---

## 6. 后续可扩展点

- 增加数据质量告警（连续多日未更新、关键字段缺失率）
- 数据源健康度监控（akshare 接口可用性探测）
- 实时日志推送（WebSocket / SSE）
- 采集任务手动触发/重试（从只读扩展为运维控制台）
