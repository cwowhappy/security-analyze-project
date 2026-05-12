# 功能规划：基础股票与公司信息（含数据采集）v2

> 版本：v2.0 | 日期：2026年5月11日
>
> 本文档基于原型页面 `prototype-stock-company.html` 重新设计，废弃 v1.0 中过度工程化的采集架构与关联表设计，采用极简数据模型与聚焦前端的 API 设计。

---

## 目录

- [一、设计原则](#一设计原则)
- [二、页面功能拆解](#二页面功能拆解)
  - [2.1 首页概览](#21-首页概览)
  - [2.2 股票列表](#22-股票列表)
  - [2.3 股票详情](#23-股票详情)
  - [2.4 公司列表](#24-公司列表)
  - [2.5 公司详情](#25-公司详情)
  - [2.6 采集任务](#26-采集任务)
- [三、数据模型设计](#三数据模型设计)
- [四、API 接口规划](#四api-接口规划)
- [五、数据采集体系](#五数据采集体系)
- [六、实施计划](#六实施计划)

---

## 一、设计原则

1. **原型驱动**：所有功能、字段、筛选项严格对齐 `prototype-stock-company.html` 已展示的内容，不做超前进度设计。
2. **模型极简**：A 股股票与公司本质为 1:1 映射，废弃关联表，直接在股票表外键关联公司，降低查询复杂度。
3. **采集解耦**：采集器作为独立 Python 进程运行，后端仅提供"任务状态查询"与"手动触发"能力，不介入调度细节。
4. **单源优先**：全量采集以 AKShare 为主数据源，Tushare 仅作为字段补充脚本（非实时降级），避免双源适配的代码膨胀。

---

## 二、页面功能拆解

### 2.1 首页概览

**路径**：`/`  
**功能**：展示系统核心数据指标与功能入口。

| 区块 | 内容 | 数据来源 |
|------|------|---------|
| 欢迎卡片 | 用户问候语 + 市场状态 | 静态 / 后端配置 |
| 统计卡片-1 | A股上市公司总数 | `SELECT COUNT(*) FROM tb_company_basic` |
| 统计卡片-2 | 数据覆盖股票数 | `SELECT COUNT(*) FROM tb_stock_basic` |
| 统计卡片-3 | 下次定时采集时间 | 采集器心跳写入 Redis / 配置表 |
| 统计卡片-4 | 最近采集成功率 | `tb_collection_task` 最新一条全量任务成功率 |
| 功能入口 | 股票列表 / 公司列表 / 采集任务 快捷卡片 | 静态路由 |

### 2.2 股票列表

**路径**：`/stocks`  
**功能**：全市场 A 股基础信息一览，支持多维筛选与关键词搜索。

**筛选器**：

| 筛选维度 | 字段 | 类型 | 说明 |
|---------|------|------|------|
| 关键词搜索 | `keyword` | input | 匹配 `stock_code`（精确前缀）或 `name`（模糊） |
| 市场 | `market` | select | 全部 / 主板 / 创业板 / 科创板 / 北交所 |
| 行业 | `industry` | select | 全部 + 动态加载现有行业列表 |
| 地域 | `area` | select | 全部 + 动态加载现有地域列表 |

**表格字段**：

| 字段 | 对应 DB 字段 | 说明 |
|------|-------------|------|
| 股票代码 | `stock_code` | 带样式高亮，点击跳转详情 |
| 股票简称 | `name` | — |
| 交易所 | `exchange` | SH / SZ / BJ，带徽章样式 |
| 市场类型 | `market` | 主板 / 创业板 / 科创板 / 北交所 |
| 行业 | `industry` | — |
| 地域 | `area` | — |
| 总股本(亿股) | `total_shares` | 格式化：÷100000000，保留2位小数 |
| 流通股本(亿股) | `float_shares` | 同上 |
| 上市日期 | `list_date` | YYYY-MM-DD |
| 操作 | — | "详情"按钮跳转 |

**分页**：后端分页，默认 `page=1&size=20`。

### 2.3 股票详情

**路径**：`/stocks/:stockCode`  
**功能**：单只股票全量基础信息展示，并一键跳转关联公司。

**页面结构**：

| 区块 | 字段 | 来源 |
|------|------|------|
| 头部信息 | 股票代码 + 股票简称 + 交易所徽章 + 市场类型标签 | `tb_stock_basic` |
| 头部操作 | "查看关联公司"按钮 | 通过 `company_id` 跳转 |
| 基础信息 | 股票代码 / Tushare代码 / 股票简称 / 股票全称 / 交易所代码 / 市场类型 | `tb_stock_basic` |
| 上市信息 | 上市日期 / 所属行业 / 所属地域 | `tb_stock_basic` |
| 股本信息 | 总股本（股）/ 流通股本（股）/ 总股本（亿股）/ 流通股本（亿股） | `tb_stock_basic` |
| 关联公司 | 公司名称 + 统一信用代码 + 跳转按钮 | JOIN `tb_company_basic` |

### 2.4 公司列表

**路径**：`/companies`  
**功能**：上市公司工商信息一览，支持多维筛选与关键词搜索。

**筛选器**：

| 筛选维度 | 字段 | 类型 | 说明 |
|---------|------|------|------|
| 关键词搜索 | `keyword` | input | 匹配 `name` / `short_name` / `unified_social_credit_code`（模糊） |
| 行业 | `industry` | select | 全部 + 动态加载 |
| 省份 | `province` | select | 全部 + 动态加载 |
| 企业性质 | `controller_type` | select | 全部 / 国企 / 民营 / 外资 / 其他 |

**表格字段**：

| 字段 | 对应 DB 字段 | 说明 |
|------|-------------|------|
| 公司全称 | `name` | — |
| 简称 | `short_name` | — |
| 行业 | `industry` | — |
| 省份 | `province` | — |
| 城市 | `city` | — |
| 企业性质 | `controller_type` | 国企/民营/外资/其他，带徽章样式 |
| 法人代表 | `legal_representative` | — |
| 员工人数 | `employees` | 千分位格式化 |
| 操作 | — | "详情"按钮跳转 |

### 2.5 公司详情

**路径**：`/companies/:uscCode`  
**功能**：单家公司全量工商、管理层、业务信息展示，并反查关联股票。

**页面结构**：

| 区块 | 字段 | 来源 |
|------|------|------|
| 头部信息 | 公司全称 + 企业性质徽章 | `tb_company_basic` |
| 头部副标题 | 公司简称 + 英文名称 | `tb_company_basic` |
| 头部操作 | "查看关联股票"按钮 | 反查 `tb_stock_basic` |
| 工商注册信息 | 统一社会信用代码 / 公司全称 / 公司简称 / 英文名称 / 曾用简称 / 注册资本（万元） | `tb_company_basic` |
| 成立与地址信息 | 成立日期 / 所在省份 / 所在城市 / 注册地址 / 办公地址 / 官方网站 | `tb_company_basic` |
| 管理层信息 | 法人代表 / 董事长 / 总经理 / 董事会秘书 / 员工人数 | `tb_company_basic` |
| 业务与治理信息 | 所属行业 / 实控人名称 / 实控人性质 | `tb_company_basic` |
| 主营业务 | 主营业务描述 | `tb_company_basic.main_business`，文本块展示 |
| 经营范围 | 经营范围 | `tb_company_basic.business_scope`，文本块展示 |
| 公司简介 | 公司简介 | `tb_company_basic.introduction`，文本块展示 |
| 关联股票 | 股票代码 + 名称 + 交易所徽章 + 跳转按钮 | JOIN `tb_stock_basic` |

### 2.6 采集任务

**路径**：`/collection/tasks`  
**功能**：查看采集历史、监控执行状态、手动触发任务。

> **注意**：本页面仅做"监控与触发"，不涉及定时规则配置。定时规则由采集器内部 `APScheduler` 管理，不暴露给前端。

**统计卡片**：

| 卡片 | 计算方式 |
|------|---------|
| 总任务数 | `SELECT COUNT(*) FROM tb_collection_task` |
| 执行成功 | `SELECT COUNT(*) WHERE status = 'success'` |
| 执行中 | `SELECT COUNT(*) WHERE status = 'running'` |
| 执行失败 | `SELECT COUNT(*) WHERE status = 'failed'` |

**搜索**：按任务名称或类型模糊过滤。

**任务卡片字段**：

| 字段 | 说明 |
|------|------|
| 任务名称 | 如"股票全量信息采集" |
| 任务类型 | `stock_full` / `company_full` / `stock_single` / `company_single` |
| 执行时间 | 实际开始时间 |
| 执行时长 | 完成/失败时显示耗时，进行中显示"进行中..." |
| 处理条数 | `total_count`（预期处理数） |
| 成功/失败数 | `success_count` / `fail_count` |
| 进度条 | `success_count / total_count * 100%` |
| 状态徽章 | 成功(绿) / 执行中(蓝+呼吸灯) / 失败(红) |
| 操作按钮 | "重新执行" — 仅对失败或成功任务可点击，创建一条新的即时任务 |

---

## 三、数据模型设计

**核心变化**：
- 废除 `tb_relation_stock_company`（关联表），改为 `tb_stock_basic.company_id` 外键直接关联。
- 废除 `tb_collection_task_schedule`（定时规则表），定时规则由采集器内部配置管理。

### 3.1 表清单

| 表名 | 中文名 | 说明 |
|------|--------|------|
| `tb_stock_basic` | 股票基础信息表 | 存储 A 股静态属性，含 `company_id` 外键 |
| `tb_company_basic` | 公司基础信息表 | 存储上市公司工商、管理层、业务信息 |
| `tb_collection_task` | 采集任务执行表 | 记录每次任务状态与结果 |

### 3.2 tb_stock_basic

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | VARCHAR(32) | PRIMARY KEY | 主键ID |
| `stock_code` | VARCHAR(20) | NOT NULL, UNIQUE | 股票代码，如 `000001` |
| `ts_code` | VARCHAR(20) | — | Tushare代码，如 `000001.SZ` |
| `name` | VARCHAR(100) | NOT NULL | 股票简称 |
| `full_name` | VARCHAR(200) | — | 股票全称 |
| `market` | VARCHAR(20) | — | 市场类型 |
| `exchange` | VARCHAR(10) | — | 交易所：SH / SZ / BJ |
| `list_date` | DATE | — | 上市日期 |
| `industry` | VARCHAR(50) | — | 所属行业 |
| `area` | VARCHAR(50) | — | 所属地域 |
| `total_shares` | BIGINT | — | 总股本（股） |
| `float_shares` | BIGINT | — | 流通股本（股） |
| `company_id` | VARCHAR(32) | — | **外键关联 `tb_company_basic.id`** |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |

**索引**：`stock_code`（唯一）、`ts_code`、`industry`、`market`、`exchange`、`company_id`

### 3.3 tb_company_basic

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | VARCHAR(32) | PRIMARY KEY | 主键ID |
| `unified_social_credit_code` | VARCHAR(50) | UNIQUE | 统一社会信用代码 |
| `name` | VARCHAR(200) | NOT NULL | 公司全称 |
| `short_name` | VARCHAR(100) | — | 公司简称 |
| `english_name` | VARCHAR(200) | — | 英文名称 |
| `former_name` | VARCHAR(200) | — | 曾用简称 |
| `legal_representative` | VARCHAR(50) | — | 法人代表 |
| `chairman` | VARCHAR(50) | — | 董事长 |
| `manager` | VARCHAR(50) | — | 总经理 |
| `secretary` | VARCHAR(50) | — | 董事会秘书 |
| `reg_capital` | DECIMAL(18,4) | — | 注册资本（万元）|
| `setup_date` | DATE | — | 成立日期 |
| `province` | VARCHAR(50) | — | 所在省份 |
| `city` | VARCHAR(50) | — | 所在城市 |
| `reg_address` | VARCHAR(500) | — | 注册地址 |
| `office_address` | VARCHAR(500) | — | 办公地址 |
| `website` | VARCHAR(200) | — | 官方网站 |
| `industry` | VARCHAR(50) | — | 所属行业 |
| `main_business` | TEXT | — | 主营业务描述 |
| `business_scope` | TEXT | — | 经营范围 |
| `introduction` | TEXT | — | 公司简介 |
| `employees` | INT | — | 员工人数 |
| `controller_name` | VARCHAR(100) | — | 实控人名称 |
| `controller_type` | VARCHAR(50) | — | 实控人性质：国企/民营/外资/其他 |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |

**索引**：`unified_social_credit_code`（唯一）、`name`、`industry`

### 3.4 tb_collection_task

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | VARCHAR(32) | PRIMARY KEY | 主键ID |
| `task_type` | VARCHAR(50) | NOT NULL | 任务类型：stock_full / company_full / stock_single / company_single |
| `task_params` | JSONB | — | 任务参数，如 `{"stock_code":"000001"}` |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'pending' | pending / running / success / failed |
| `data_source` | VARCHAR(20) | — | akshare / tushare |
| `total_count` | INT | DEFAULT 0 | 预期处理记录数 |
| `success_count` | INT | DEFAULT 0 | 成功记录数 |
| `fail_count` | INT | DEFAULT 0 | 失败记录数 |
| `error_message` | TEXT | — | 错误信息 |
| `started_at` | TIMESTAMP | — | 实际开始时间 |
| `completed_at` | TIMESTAMP | — | 实际完成时间 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |

**索引**：`status`、`task_type`、`created_at`

> **定时规则说明**：`tb_collection_task_schedule` 表不再创建。采集器内部通过 `APScheduler` 的 `BackgroundScheduler` 管理 Cron 规则，配置存放于采集器配置文件中（如 `collector/config/scheduler.yml` 或环境变量）。后端与前端均不感知定时规则细节。

---

## 四、API 接口规划

### 统一响应格式

```json
{
  "success": true,
  "code": 200,
  "message": "OK",
  "data": { ... },
  "timestamp": 1715340000000
}
```

### 股票 API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/stocks` | 股票列表（分页、筛选、搜索） |
| GET | `/api/v1/stocks/{stockCode}` | 股票详情（含关联公司简要信息） |

**列表查询参数**：`page`、`size`、`market`、`industry`、`area`、`keyword`

### 公司 API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/companies` | 公司列表（分页、筛选、搜索） |
| GET | `/api/v1/companies/{uscCode}` | 公司详情（含关联股票列表） |

**列表查询参数**：`page`、`size`、`industry`、`province`、`controller_type`、`keyword`

### 采集任务 API

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/collection/tasks` | 任务列表（分页、状态筛选、类型筛选） |
| GET | `/api/v1/collection/tasks/{taskId}` | 任务详情 |
| POST | `/api/v1/collection/tasks` | 创建即时采集任务 |

**创建任务请求体**：

```json
{
  "taskType": "stock_full",
  "taskParams": null,
  "dataSource": "akshare"
}
```

**任务类型枚举**：`stock_full`、`company_full`、`stock_single`、`company_single`

---

## 五、数据采集体系

### 5.1 架构简化说明

v1.0 中的"数据源适配器层 + 实时降级 + 双源并发"被废弃，改为：

```
┌─────────────────────────────────────────┐
│           采集器（Python）               │
│  ┌─────────────┐   ┌─────────────────┐ │
│  │ APScheduler │   │ HTTP API (可选)  │ │
│  │ 内部 Cron   │   │ POST /trigger   │ │
│  └──────┬──────┘   └─────────────────┘ │
│         ↓                               │
│  ┌─────────────────────────────────┐   │
│  │ 采集脚本（顺序执行）             │   │
│  │ 1. stock_full  →  AKShare      │   │
│  │ 2. company_full →  AKShare     │   │
│  │ 3. 字段补充脚本 →  Tushare      │   │
│  └─────────────┬───────────────────┘   │
│                ↓                        │
│         PostgreSQL（直接写入）           │
└─────────────────────────────────────────┘
```

**关键变化**：
1. **无适配器抽象**：直接编写面向 AKShare / Tushare 的独立采集脚本，用函数组织而非类继承。
2. **无实时降级**：AKShare 全量采集失败后，整体任务标记为 `failed`，由人工或告警处理；Tushare 补充字段脚本独立运行，失败不影响主数据。
3. **无 FastAPI**：采集器作为后台进程运行，通过 APScheduler 内部调度；后端通过写入 `tb_collection_task` 的 `pending` 记录触发（采集器轮询此表），或直接调用采集器暴露的极简 HTTP 端点。

### 5.2 采集任务类型

| 任务类型 | 说明 | 触发方式 | 建议周期 |
|---------|------|---------|---------|
| `stock_full` | 全量股票基础信息采集 | 定时 | 每日凌晨 2:00 |
| `company_full` | 全量公司工商信息采集 | 定时 | 每周日 3:00 |
| `stock_single` | 单只股票更新 | 手动 | 按需 |
| `company_single` | 单家公司更新 | 手动 | 按需 |
| `field_supplement` | Tushare 字段补充（area/ts_code/实控人等） | 定时 | 每周一 4:00 |

### 5.3 限流与容错

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `SOURCE_REQUEST_DELAY_MIN` | 1 秒 | 请求间最小延迟 |
| `SOURCE_REQUEST_DELAY_MAX` | 3 秒 | 请求间最大延迟 |
| `BATCH_FAIL_THRESHOLD` | 10% | 单批次失败率超过此阈值，整体任务标记 `failed` |

> 废弃 v1.0 中的指数退避重试与自动降级逻辑。单条记录失败记录到 `fail_count`，继续处理下一条；超过阈值后中断并人工介入。

---

## 六、实施计划

### P0（MVP）

| 序号 | 内容 |
|------|------|
| 1 | Flyway 迁移脚本：`tb_stock_basic`、`tb_company_basic`、`tb_collection_task` |
| 2 | 后端：股票列表 / 详情 / 搜索 API |
| 3 | 后端：公司列表 / 详情 / 搜索 API |
| 4 | 后端：采集任务列表 / 创建 / 详情 API |
| 5 | 采集器：AKShare `stock_full` 与 `company_full` 脚本 |
| 6 | 前端：股票列表 / 详情页面 |
| 7 | 前端：公司列表 / 详情页面 |
| 8 | 前端：采集任务页面 |

### P1（迭代）

| 序号 | 内容 |
|------|------|
| 1 | Tushare 字段补充脚本（area、ts_code、实控人、管理层） |
| 2 | 首页概览统计接口与数据对接 |
| 3 | 单只股票 / 单家公司手动刷新 |
| 4 | 采集任务执行时长自动计算 |

### P2（扩展）

| 序号 | 内容 |
|------|------|
| 1 | 港股公司信息接入 |
| 2 | 主营构成数据（`stock_zygc_em`） |
| 3 | 当 A+H 扩展需求明确时，恢复关联表设计 |

---

## 版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-11 | 基于原型重新设计：废除关联表与定时规则表、简化采集架构、聚焦原型页面功能 |
| v1.0 | 2026-05-11 | 初始版本（已废弃） |
