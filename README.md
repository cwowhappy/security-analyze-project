# 证券分析系统

> 面向团队内部的 A 股（沪深京）上市公司数据查询与分析平台。

---

## 项目简介

**证券分析系统**（内部代号 `security-analyze`）是一个支持多维度证券数据查询、财务报表分析与可视化展示的内部数据平台。系统覆盖 **上市公司信息、财务报表、行业分类、指数行情、ETF 基金** 五大核心数据域，提供从数据采集、清洗存储到前端可视化的一站式解决方案。

系统采用前后端分离架构，由三大独立单元组成：
- **前端**（Vue 3 + TypeScript）：提供交互式数据查询与图表分析界面
- **后端**（Spring Boot 3 + Java 21）：REST API 服务与业务逻辑编排
- **数据采集**（Python 3.11+）：基于 `akshare` 的自动化数据采集与定时调度

三者共享同一个 PostgreSQL 数据库，支持用户权限管理（JWT + RBAC）与采集任务监控。

---

## 技术栈

| 层级 | 技术 | 版本/说明 |
|------|------|-----------|
| 前端 | Vue 3 + TypeScript + Vite | Vue 3.5, TS 5.8, Vite 6 |
| 前端 UI | Element Plus + ECharts | Element Plus 2.9, ECharts 5.6 |
| 前端状态 | Pinia + Vue Router | Pinia 3.0, Vue Router 4.5 |
| 后端 | Java 21 + Spring Boot 3.5.x + Gradle 9.4 | Spring Data JDBC（非 JPA） |
| 数据库 | PostgreSQL 16+ | 支持 JSONB、自定义枚举 |
| 数据采集 | Python 3.11+ + Poetry | akshare, psycopg, APScheduler |
| 安全 | Spring Security + JWT | RBAC 权限控制 |

---

## 系统模块与功能点

### 一、用户认证与权限管理

- **用户注册**：前台用户自主注册，默认状态为 `待审批（PENDING）`
- **登录认证**：基于 JWT Token 的无状态认证，支持前台用户登录与管理后台独立登录
- **权限控制**：
  - `USER` 角色：可访问公司、行业、指数等数据查询功能
  - `ADMIN` 角色：额外拥有用户管理、采集监控后台权限
- **用户生命周期管理**：管理员可审批、禁用、启用普通用户账号

### 二、上市公司信息

- **公司列表搜索**：支持按股票代码、公司名称关键词模糊搜索，带分页与联想提示
- **公司详情页**：Tab 页签结构展示：
  - **基本信息**：股票代码、公司名称、所属行业、地区、成立日期、注册资本、上市日期、市场板块
  - **关联证券**：展示该公司所有上市证券（A股 / B股 / H股 / 优先股 / ADR）及上市状态
  - **财务报告**：嵌入财务分析面板（见下文）
  - **历史变更**：预留扩展
- **多标准行业映射**：支持同时展示申万行业分类与东方财富行业分类

### 三、财务报表分析

- **报告列表**：按股票代码查询历史财务报表，支持按报告期筛选
- **核心指标趋势分析**：
  - 可视化展示营业总收入、归母净利润、毛利率、净利率、资产负债率等核心指标
  - 支持按报告类型（一季报 / 中报 / 三季报 / 年报）过滤
  - 支持自定义报告期起止范围
- **年度报告期对比**：同一年度四个报告期的横向对比分析
- **报告期详情**：
  - 资产负债表（货币资金、应收账款、存货、资产总计、负债合计等 10 项核心指标）
  - 利润表（营业总收入、营业成本、三费、营业利润、净利润、归母净利润等 11 项）
  - 现金流量表（经营 / 投资 / 筹资活动现金流、现金净增加额、期末余额等 5 项）
  - 汇总卡片展示：营业总收入、归母净利润、总资产、净资产、经营现金流

### 四、行业分类体系

- **双标准支持**：
  - **申万行业分类（SW）**：两级树形结构，支持一级行业钻取到二级行业
  - **东方财富行业分类（EM）**：扁平化的二级行业板块列表
- **行业详情**：
  - 行业指数走势可视化（近 1 月 / 3 月 / 6 月 / 1 年）
  - 成分股列表展示，支持分页，可跳转至公司详情
- **公司与行业关联**：支持一对多映射（同一公司可属于多个分类标准）

### 五、指数与 ETF

- **指数列表**：
  - 核心指数分类展示（宽基 / 行业 / 主题 / 策略 / 其他）
  - 支持按指数代码、名称关键词搜索
- **指数详情**：
  - **基本信息**：指数代码、名称、类型、市场、基日、基点、成分股数量、发布日期
  - **趋势分析**：ECharts 双轴图表展示收盘价走势与成交量，支持日线 / 周线 / 月线切换
  - **关联 ETF**：展示跟踪该指数的 ETF 列表（ETF 代码、名称、基金规模、成立日期等）
- **核心指数标记**：内置 18 只核心指数（沪深300、上证50、创业板指、中证医疗、中证白酒等）

### 六、数据采集与监控

- **数据源**：统一封装 `akshare` 作为数据来源，预留多数据源扩展接口
- **采集任务类型**：
  | 任务 | 采集内容 | 目标表 |
  |------|----------|--------|
  | 公司信息采集 | A 股上市公司基础信息、关联证券、行业映射 | `company`, `company_security`, `company_industry_mapping` |
  | 财务报告采集 | 资产负债表、利润表、现金流量表 | `financial_report` |
  | 行业分类同步 | 申万一级/二级行业、东财行业板块 | `industry_category` |
  | 指数基本信息采集 | 指数代码、名称、类型、基日、基点 | `index_info` |
  | 指数历史行情采集 | 日/周/月 K 线数据（OHLCV、涨跌幅、换手率） | `index_history` |
  | ETF 基本信息采集 | ETF 代码、名称、基金规模、管理费率 | `etf_info` |
- **高级采集能力**：
  - **Session 断点续传**：全量任务自动生成 UUID Session，逐只股票记录进度；中断后可通过 Session ID 恢复，自动跳过已成功的股票并重试失败的
  - **增量采集**：基于 `collector_stock_sync_status` 记录的最新报告期，仅采集新增数据
  - **批量并发处理**：财务报告采集支持按批次处理（默认每批 100 家），批次内多线程并发
  - **智能重试**：数据源请求失败时自动重试（最多 3 次，指数退避）
- **采集监控面板**（Admin 权限）：
  - 数据采集概览：各数据类型的总记录数、最新任务状态、最后更新时间
  - 最近 7 天任务记录列表：支持按数据类型、任务状态筛选，自动刷新

---

## 项目结构

```
security-analyze-project/
├── backend/                     # Java 后端服务
│   ├── src/main/java/com/example/securityanalyze/
│   │   ├── SecurityAnalyzeApplication.java
│   │   ├── company/             # 上市公司模块
│   │   ├── finance/             # 财务报表模块
│   │   ├── index/               # 指数与 ETF 模块
│   │   ├── industry/            # 行业分类模块
│   │   ├── auth/                # 前台认证模块
│   │   ├── admin/               # 管理后台模块
│   │   ├── user/                # 用户域（共享）
│   │   ├── collector/           # 采集监控模块
│   │   └── config/              # 安全配置、CORS 等
│   └── src/main/resources/db/migration/  # 数据库迁移脚本
│
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── api/                 # HTTP API 封装（axios）
│       ├── types/               # TypeScript 类型定义
│       ├── views/               # 页面视图
│       │   ├── HomeView.vue
│       │   ├── company/         # 公司列表、详情
│       │   ├── industry/        # 行业列表、详情
│       │   ├── index/           # 指数列表、详情
│       │   ├── collector/       # 采集监控面板
│       │   ├── auth/            # 登录、注册
│       │   └── admin/           # 管理后台登录、用户管理
│       └── router/index.ts      # 路由定义
│
├── collector/                   # Python 数据采集模块
│   ├── main.py                  # 入口脚本（CLI + 调度器）
│   ├── collector/
│   │   ├── scheduler.py         # APScheduler 定时调度
│   │   ├── db/postgres.py       # PostgreSQL 连接池封装
│   │   ├── sources/akshare_source.py   # 数据源封装
│   │   └── tasks/               # 采集任务
│   │       ├── company_task.py
│   │       ├── finance_task.py
│   │       ├── index_basic_task.py
│   │       ├── index_history_task.py
│   │       ├── etf_basic_task.py
│   │       └── industry_classification_sync.py
│   └── pyproject.toml           # Poetry 依赖配置
│
├── docs/                        # 设计文档与接口契约
│   ├── plans/                   # 系统设计文档
│   └── wiki/                    # 模块设计、API 契约
│
└── AGENTS.md                    # 面向 AI 助手的项目指南
```

---

## 快速开始

> 详细的构建、运行、部署说明请查阅 [`AGENTS.md`](./AGENTS.md)。

### 环境要求

- PostgreSQL 16+（本地开发可用 Homebrew / 包管理器安装）
- Java 21
- Node.js 20+
- Python 3.11+

### 一键启动（本地开发）

```bash
# 1. 初始化数据库
psql -U postgres -c "CREATE DATABASE security_analyze;"
for f in backend/src/main/resources/db/migration/V*.sql; do
    psql -U postgres -d security_analyze -f "$f"
done

# 2. 启动后端
cd backend && ./gradlew bootRun

# 3. 启动前端
cd frontend && npm install && npm run dev

# 4. 执行数据采集（按需）
cd collector && poetry install
poetry run python main.py --run-company
poetry run python main.py --sync-industry
```

前端开发服务器运行在 `http://localhost:3000`，后端 API 运行在 `http://localhost:8080`。

---

## 文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| 开发指南 | [`AGENTS.md`](./AGENTS.md) | 技术栈、分层约定、构建命令、测试策略 |
| 系统设计 | `docs/plans/2026-04-30-stock-analysis-system-design.md` | 技术选型、模块划分、数据流 |
| 模块设计 | `docs/wiki/module-design.md` | 功能模块职责边界与分层归属 |
| API 契约 | `docs/wiki/api-company.md` | 公司列表、公司详情接口定义 |
| 部署文档 | `docs/deploy-aliyun.md` | 阿里云 ECS + RDS 生产部署指南 |

---

## 截图预览

> TODO: 补充首页、公司详情、财务分析、行业详情、指数趋势等核心页面截图。

---

*项目处于持续迭代中，欢迎通过 Issue 或 PR 提交反馈。*
