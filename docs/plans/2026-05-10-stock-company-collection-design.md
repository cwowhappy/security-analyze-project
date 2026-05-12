# 设计计划：股票与公司基础信息采集模块 v2

> 计划日期：2026-05-11
> 模块范围：数据采集模块（Python）、后端（Java）、前端（Vue）
> 关联文档：
> - `docs/prd/feature-01-stock-company-info.md` — 功能规划（v2.0）
> - `docs/wiki/data-model-stock-company-collection.md` — 数据模型与表结构（v2.0）
> - `docs/api-contracts/stock-company-collection-api-contract.md` — 前后端交互契约（v2.0）

---

## 一、需求概述

构建"股票与公司基础信息"功能模块，覆盖三端：

1. **数据采集模块**：以 AKShare 为主数据源，Tushare 作为独立补充脚本，顺序执行，无实时降级逻辑。
2. **后端模块**：提供股票查看、公司查看、任务管理 REST API，无定时规则管理接口。
3. **前端模块**：股票列表/详情、公司列表/详情、采集任务管理页面，严格对齐 `prototype-stock-company.html`。

---

## 二、关键设计决策

### 2.1 数据范围

| 类别 | 范围 |
|------|------|
| 股票基础信息 | 股票代码、名称、全称、市场、交易所、上市日期、行业、地域、总股本、流通股本 |
| 公司基础信息 | 公司全称、统一社会信用代码、法人代表、管理层、注册资本、成立日期、省份/城市、注册/办公地址、官网、行业、主营业务、经营范围、公司简介、员工人数、实控人信息 |
| 不包含 | 实时行情、联系方式（zip/phone/fax/email）、主营构成（P2）、同行对比（P2） |

### 2.2 数据源策略

| 数据类别 | 主数据源 | 补充脚本 | 说明 |
|----------|---------|---------|------|
| 股票基础信息 | AKShare | Tushare 字段补充脚本 | Tushare 补充 `area`、`ts_code`、实控人 |
| 公司基础信息 | AKShare `stock_profile_cninfo` | Tushare 字段补充脚本 | Tushare 补充 董事长/总经理/董秘、员工人数、统一社会信用代码、省份/城市 |

- AKShare 全量采集失败时，任务标记 `failed`，不自动降级到 Tushare
- Tushare 补充脚本独立运行，失败不影响主数据完整性

### 2.3 模型简化

| v1.0（废弃） | v2.0（当前） | 理由 |
|-------------|-------------|------|
| `tb_relation_stock_company` 关联表 | 废除，改为 `tb_stock_basic.company_id` 外键 | A股1:1映射是常态，降低查询复杂度 |
| `tb_collection_task_schedule` 定时规则表 | 废除，APScheduler 配置在采集器内部 | 前端不展示规则管理，减少维护面 |
| 双数据源适配器层 + 实时降级 | 顺序脚本执行，失败即停 | 降低代码复杂度，避免过度工程化 |

### 2.4 采集架构

```
采集器（Python 独立进程）
├── APScheduler BackgroundScheduler
│   ├── 每日 02:00  →  stock_full（AKShare）
│   ├── 每周日 03:00 → company_full（AKShare）
│   └── 每周一 04:00 → field_supplement（Tushare）
├── 手动触发入口
│   ├── 轮询 tb_collection_task 表中 status=pending 的记录
│   └── 或暴露极简 HTTP POST /trigger（可选）
└── 执行结果写入 tb_collection_task
```

APScheduler 配置：
- `max_workers = 3`（降低并发，避免限流）
- `coalesce = True`
- `max_instances = 1`
- `misfire_grace_time = 3600`

---

## 三、模块设计摘要

### 3.1 采集器（Python）

- **调度引擎**：APScheduler `BackgroundScheduler`，配置硬编码或读取环境变量
- **采集脚本**：
  - `scripts/stock_full.py` — AKShare 全量股票采集
  - `scripts/company_full.py` — AKShare 全量公司采集
  - `scripts/field_supplement.py` — Tushare 字段补充
- **状态写入**：任务开始/结束时直接更新 `tb_collection_task` 表
- **手动触发**：采集器进程每 30 秒轮询 `tb_collection_task` 表中 `status='pending'` 的记录并执行

### 3.2 后端（Java）

新增/扩展三个领域模块：

| 模块 | 核心职责 | REST 前缀 |
|------|---------|----------|
| `stock` | 股票列表、详情（含关联公司） | `/api/v1/stocks` |
| `company` | 公司列表、详情（含关联股票列表） | `/api/v1/companies` |
| `collection` | 任务历史查询、即时任务创建 | `/api/v1/collection/tasks` |

**注意**：无 `schedule` 模块，无定时规则管理接口。

### 3.3 前端（Vue 3）

页面严格对齐原型：

| 页面 | 路由 | 功能 |
|------|------|------|
| 首页概览 | `/` | 统计卡片 + 功能入口 |
| 股票列表 | `/stocks` | 列表 + 市场/行业/地域筛选 + 关键词搜索 |
| 股票详情 | `/stocks/:stockCode` | 基础信息 + 上市信息 + 股本信息 + 关联公司卡片 |
| 公司列表 | `/companies` | 列表 + 行业/省份/企业性质筛选 + 关键词搜索 |
| 公司详情 | `/companies/:uscCode` | 工商信息 + 管理层 + 业务信息 + 关联股票列表 |
| 采集任务 | `/collection/tasks` | 任务列表 + 统计卡片 + 手动触发 + 重新执行 |

---

## 四、异常与监控

**异常层次（简化）：**

```
采集异常
├── 网络/限流异常 → 记录 fail_count，延迟后继续（单条级）
└── 数据格式异常 → 记录 fail_count，跳过本条
```

> 废弃 v1.0 中的 SourceUnavailableError / SourceRateLimitError / SourceDataError 异常分层与自动降级逻辑。

**监控维度：**
- 任务成功率 → `tb_collection_task`
- 数据规模 → `tb_stock_basic` / `tb_company_basic` COUNT
- 系统指标 → Spring Boot Actuator `/actuator/metrics`

---

## 五、实施里程碑

### Phase 1：数据库与后端骨架

| 序号 | 内容 | 交付物 |
|------|------|--------|
| 1.1 | Flyway 迁移脚本（3张表） | `V2__create_stock_company_collection_tables.sql` |
| 1.2 | Java 实体类（Stock / Company / CollectionTask） | `domain/entity/` |
| 1.3 | Repository 层（Spring Data JDBC） | `domain/repository/` |
| 1.4 | Service + Controller（股票/公司/任务 API） | `application/`、`web/` |
| 1.5 | API 自测通过 | Postman / HTTP 测试 |

### Phase 2：采集器

| 序号 | 内容 | 交付物 |
|------|------|--------|
| 2.1 | AKShare 股票全量采集脚本 | `collector/scripts/stock_full.py` |
| 2.2 | AKShare 公司全量采集脚本 | `collector/scripts/company_full.py` |
| 2.3 | APScheduler 调度配置 | `collector/scheduler.py` |
| 2.4 | 任务状态写入逻辑 | 集成到采集脚本 |
| 2.5 | 采集器可独立运行 | `poetry run python -m stock_collector` |

### Phase 3：前端页面

| 序号 | 内容 | 交付物 |
|------|------|--------|
| 3.1 | 首页概览页面 | `views/HomeView.vue` |
| 3.2 | 股票列表 + 详情页面 | `views/StockListView.vue`、`StockDetailView.vue` |
| 3.3 | 公司列表 + 详情页面 | `views/CompanyListView.vue`、`CompanyDetailView.vue` |
| 3.4 | 采集任务页面 | `views/CollectionTaskView.vue` |
| 3.5 | API 对接联调 | 端到端可用 |

### Phase 4：补充与优化（P1）

| 序号 | 内容 | 交付物 |
|------|------|--------|
| 4.1 | Tushare 字段补充脚本 | `collector/scripts/field_supplement.py` |
| 4.2 | 单只股票/公司手动刷新 | 前端按钮 + 后端 API |
| 4.3 | 采集任务执行时长计算 | 后端 Service 层 |
| 4.4 | 首页统计接口 | `DashboardController` |

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-11 | 简化架构：废除关联表与定时规则表、顺序脚本执行、聚焦原型页面 |
| v1.0 | 2026-05-10 | 初始计划（已废弃） |
