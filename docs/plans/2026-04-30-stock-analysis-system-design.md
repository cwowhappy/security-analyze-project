# 股票分析系统 — 系统划分设计

## 1. 项目概述

构建一个面向团队内部的 A 股（沪深京）分析平台，支持：
- 公司基本信息查询
- 上市证券信息查询
- 季度与年度财务报告数据查询与展示

## 2. 技术选型

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| 后端 | Java 21 + Spring Boot 3.5.x + Gradle 9.4 |
| 数据存储 | PostgreSQL |
| 数据采集 | Python 3.11+（初始接入 akshare） |
| 安全 | Spring Security + JWT |

## 3. 系统模块划分

系统由三大独立单元组成：

### 3.1 前端（frontend/）

- Vue 3 单页应用，管理后台风格
- 模块：Dashboard、股票列表、公司详情、财报中心、用户管理
- 状态管理 Pinia，路由 Vue Router
- 通过 RESTful API 与后端通信

### 3.2 后端（backend/）

单 Gradle 模块，内部按 package 分层：

```
com.example.stock
├── api          # Controller、DTO、请求/响应对象
├── application  # 业务用例、权限校验、数据编排
├── domain       # 核心领域模型、Repository 接口、领域服务
└── infrastructure  # Repository 实现、配置、工具类、外部调用封装
```

职责：
- 提供前端所需的全部 RESTful API
- 用户认证与 RBAC 权限控制
- 业务逻辑与领域规则（如财务指标计算）
- 不直接参与数据采集调度

### 3.3 数据采集（collector/）

独立的 Python 工程：

- 初始接入 akshare 获取 A 股数据
- 内置定时任务调度器（不依赖外部 Airflow）
- 任务直连 PostgreSQL 写入数据
- 提供采集日志记录，便于运维监控
- 预留多数据源接入的抽象能力（如后续接入 Tushare）

## 4. 数据流

```
┌─────────────┐     HTTP      ┌─────────────┐
│   前端       │ ◄───────────► │   后端       │
│  (Vue3)     │   REST API    │ (SpringBoot)│
└─────────────┘               └──────┬──────┘
                                     │
                                     │ 查询
                                     ▼
                              ┌─────────────┐
                              │  PostgreSQL  │
                              └──────▲──────┘
                                     │ 写入
                                     │
                              ┌──────┴──────┐
                              │  数据采集    │
                              │  (Python)   │
                              └─────────────┘
```

## 5. 部署形态

- 团队内部平台，建议容器化部署（Docker / Docker Compose）
- 初期为单体部署，前端 Nginx 静态托管，后端 JAR 运行，PostgreSQL 独立容器
- 数据采集模块作为独立容器，通过 cron 或内置调度器触发

## 6. 非功能性要求（概要）

- 数据一致性：财务报告数据按（股票代码 + 报告期）去重，避免重复采集
- 可追溯性：采集任务每次执行记录日志（任务名、起止时间、状态、影响行数）
- 安全性：JWT Token + RBAC，接口分级授权
- 可扩展性：数据源接入层预留抽象，方便后续替换或新增数据源

## 7. 后续可扩展点

- 数据源：从 akshare 扩展到 Tushare / Wind 等商业接口
- 市场：从 A 股扩展到港股、美股（需处理不同会计准则和币种）
- 功能：财务指标自定义计算、同行业对比、估值模型、数据导出 Excel/PDF
