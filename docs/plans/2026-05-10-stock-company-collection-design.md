# 设计计划：股票与公司基础信息采集模块

> 计划日期：2026-05-10
> 模块范围：数据采集模块（Python）、后端（Java）、前端（Vue）
> 关联文档：
> - `docs/wiki/data-model-stock-company-collection.md` — 数据模型与表结构
> - `docs/wiki/collector-architecture.md` — 采集器架构设计
> - `docs/wiki/backend-module-design.md` — 后端模块设计
> - `docs/wiki/frontend-module-design.md` — 前端模块设计
> - `docs/api-contracts/stock-company-collection-api-contract.md` — 前后端交互契约

---

## 一、需求概述

构建"股票与公司基础信息"功能模块，覆盖三端：

1. **数据采集模块**：支持 akshare + tushare 双数据源，管理采集任务与定时调度，监控采集结果
2. **后端模块**：提供股票查看、公司查看、任务管理 REST API
3. **前端模块**：股票列表/详情、公司列表/详情、采集任务管理页面

---

## 二、关键设计决策

### 2.1 数据范围

| 类别 | 范围 |
|------|------|
| 股票基础信息 | 股票代码、名称、全称、市场、交易所、上市日期、行业、地域、总股本、流通股本 |
| 公司基础信息 | 公司全称、统一社会信用代码、法人代表、管理层、注册资本、成立日期、省份/城市、注册/办公地址、官网、行业、主营业务、经营范围、公司简介、员工人数、实控人信息 |
| 不包含 | 实时行情（current_price / change_percent 从 stock 表移除）、联系方式（zip_code / phone / fax / email 从 company 表移除） |

### 2.2 数据源策略

| 数据类别 | 主数据源 | 备用数据源 | 互补字段 |
|----------|---------|-----------|---------|
| 股票基础信息 | akshare | tushare | tushare 补充 area、market、实控人 |
| 公司基础信息 | akshare `stock_profile_cninfo` | tushare `stock_company` | tushare 补充 董事长/总经理/董秘、员工人数、统一社会信用代码 |

- akshare 优先（免费、无积分限制）
- tushare 降级补充（有积分限制，数据更规范）
- 单条失败不中断全量批次

### 2.3 事件驱动方式（修订）

原方案为"数据库轮询"。经调研 Python 定时调度框架后，调整为：

> **APScheduler `BackgroundScheduler` 作为采集器核心调度引擎**
>
> - 定时任务：从 `tb_collection_task_schedule` 加载 Cron 规则，到期自动执行
> - 即时任务：后端通过 HTTP API 调用采集器，APScheduler 立即调度执行
> - `tb_collection_task` 仅用于记录任务执行状态（running / success / failed），供后端监控查询

**APScheduler 关键配置：**
- `max_workers=5`（控制并发，避免触发数据源限流）
- `coalesce=True`（错过执行合并为一次）
- `max_instances=1`（同一任务不并发）
- `misfire_grace_time=3600`（离线1小时内补偿执行）

### 2.4 任务粒度

**混合模式：**
- **全量采集**：按表批量执行（`stock_full` / `company_full`），一次性采集全部股票/公司
- **精准触发**：支持单只股票/公司的手工更新（`stock_single` / `company_single`）

### 2.5 数据库表结构

共 5 张表，表名统一 `tb_` 前缀：

| 表名 | 说明 |
|------|------|
| `tb_stock_basic` | 股票基础信息 |
| `tb_company_basic` | 公司基础信息 |
| `tb_relation_stock_company` | 股票与公司关联关系（stock_code + company_usc_code） |
| `tb_collection_task` | 采集任务执行状态记录 |
| `tb_collection_task_schedule` | 采集定时规则（Cron 表达式） |

**关键设计：**
- `tb_stock_basic` 使用 `stock_code`（如 000001）和 `ts_code`（如 000001.SZ）双标识
- `tb_stock_basic` 移除 `company_id`、`current_price`、`change_percent`
- 股票与公司通过 `tb_relation_stock_company` 关联，而非外键

详见 `docs/wiki/data-model-stock-company-collection.md`。

---

## 三、模块设计摘要

### 3.1 采集器（Python）

- **调度引擎**：APScheduler `BackgroundScheduler`
- **数据源适配器**：`AkshareDataSource`（主）、`TushareDataSource`（备）
- **任务执行器**：按 `task_type` 路由到对应采集逻辑
- **HTTP API**：FastAPI/Flask 暴露 `POST /tasks`（即时触发）、`GET /tasks`（状态查询）
- **状态记录**：APScheduler Event Listener 自动写入 `tb_collection_task`

### 3.2 后端（Java DDD）

新增三个领域模块：

| 模块 | 核心职责 | REST 前缀 |
|------|---------|----------|
| `stock`（扩展） | 股票查询、股票详情（含关联公司） | `/api/stocks` |
| `company`（新建） | 公司查询、公司详情（含关联股票列表） | `/api/companies` |
| `collection`（新建） | 任务历史查询、即时任务创建、定时规则管理 | `/api/collection` |

### 3.3 前端（Vue 3）

新增页面：

| 页面 | 路由 | 功能 |
|------|------|------|
| 股票详情 | `/stocks/:stockCode` | 股票基础信息 + 关联公司卡片 |
| 公司列表 | `/companies` | 列表 + 行业/地域筛选 |
| 公司详情 | `/companies/:uscCode` | 工商信息 + 关联股票列表 |
| 采集任务 | `/collection/tasks` | 任务列表、手工触发、状态监控 |
| 定时规则 | `/collection/schedules` | 规则增删改查、启停 |

---

## 四、异常与监控

**异常层次：**
```
DataSourceError
├── SourceUnavailableError    → 降级备用源
├── SourceRateLimitError      → 延迟重试/降级
└── SourceDataError           → 记录跳过
```

**监控维度：**
- 任务成功率、执行时长 → `tb_collection_task`
- 数据规模 → `tb_stock_basic` / `tb_company_basic` COUNT
- 系统指标 → Spring Boot Actuator `/actuator/metrics`

---

## 五、实施里程碑

| 阶段 | 内容 | 交付物 |
|------|------|--------|
| Phase 1 | 数据库迁移脚本 + 后端 DDD 骨架（stock/company/collection 领域） | Flyway SQL + Java 实体/仓库/服务/控制器 |
| Phase 2 | 采集器数据源适配（akshare/tushare）+ APScheduler 调度 | Python 采集器可独立运行 |
| Phase 3 | 前端页面（股票/公司/采集任务） | Vue 页面 + API 对接 |
| Phase 4 | 联调测试 + 定时规则配置 + 监控面板 | 端到端可用 |

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-05-10 | 初始计划，确认数据范围、APScheduler架构、表结构 |
