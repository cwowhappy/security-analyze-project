# AGENTS.md — 证券分析系统

本文档面向 AI 编程助手，描述本项目的架构、技术栈、构建方式与开发约定。阅读本文档前，默认你对项目一无所知。

---

## 项目概述

**证券分析系统**（内部代号 `security-analyze`）是一个面向团队内部的 A 股（沪深京）上市公司数据查询与分析平台。

当前已实现的功能：
- 上市公司基本信息列表与分页搜索
- 公司详情页（基本信息、关联证券、财务报告、历史变更 Tab 页签结构，后两者为占位）
- 手动触发数据采集任务，从 akshare 拉取 A 股公司信息写入 PostgreSQL

系统由三大独立单元组成：前端（Vue 3）、后端（Spring Boot）、数据采集（Python），共享同一个 PostgreSQL 数据库。

---

## 技术栈

| 层级 | 技术 | 版本/说明 |
|------|------|-----------|
| 前端 | Vue 3 + TypeScript + Vite | Vue 3.5, TS 5.8, Vite 6 |
| 前端 UI | Element Plus + ECharts | Element Plus 2.9, ECharts 5.6 |
| 前端状态 | Pinia + Vue Router | Pinia 3.0, Vue Router 4.5 |
| 后端 | Java 21 + Spring Boot 3.5.x + Gradle 9.4 | 使用 Spring Data JDBC（非 JPA） |
| 数据库 | PostgreSQL 16+ | 独立安装部署（不通过 Docker） |
| 数据采集 | Python 3.11+ + Poetry | 使用 akshare、psycopg、schedule |
| 安全 | Spring Security | 当前开发阶段对 `/api/**` 全部放行，仅保留 CORS 配置 |

---

## 项目结构

```
security-analyze-project/
├── backend/                     # Java 后端服务
│   ├── build.gradle             # Gradle 构建配置
│   ├── settings.gradle
│   ├── gradlew / gradlew.bat
│   └── src/main/java/com/example/securityanalyze/
│       ├── SecurityAnalyzeApplication.java
│       ├── config/              # 安全配置等
│       └── company/             # 公司信息模块（按 package 分层）
│           ├── api/             # Controller、DTO（请求/响应对象）
│           ├── application/     # Service 业务逻辑
│           ├── domain/          # 实体（Company）、Repository 接口
│           └── infrastructure/  # Repository 实现（NamedParameterJdbcTemplate）
│       └── src/main/resources/
│           ├── application.yml  # Spring 配置（端口 8080）
│           └── db/migration/    # 数据库初始化脚本（Flyway 风格）
│
├── collector/                   # Python 数据采集模块
│   ├── pyproject.toml           # Poetry 配置
│   ├── requirements.txt         # 备用依赖列表
│   ├── main.py                  # 入口脚本（调度器 / 手动任务）
│   └── collector/
│       ├── scheduler.py         # 定时调度器封装
│       ├── db/postgres.py       # PostgreSQL 连接封装（psycopg）
│       ├── sources/akshare_source.py   # akshare 数据源封装
│       └── tasks/               # 采集任务
│           ├── company_task.py  # 公司信息采集（已实现）
│           └── finance_task.py  # 财务报告采集（已实现，支持 Session 断点续传）
│
├── frontend/                    # Vue 3 前端
│   ├── package.json
│   ├── vite.config.ts           # Vite 配置，开发端口 3000，代理 /api -> localhost:8080
│   ├── tsconfig*.json
│   └── src/
│       ├── main.ts              # 入口：Pinia + Router + ElementPlus
│       ├── App.vue
│       ├── router/index.ts      # 路由定义
│       ├── api/company.ts       # HTTP API 封装（axios）
│       ├── types/company.ts     # TypeScript 类型定义
│       └── views/               # 页面视图
│           ├── HomeView.vue
│           └── company/
│               ├── CompanyListView.vue
│               └── CompanyDetailView.vue
│
└── docs/                        # 设计文档与接口契约
    ├── plans/                   # 系统设计文档
    └── wiki/                    # 模块设计、API 契约
```

### 后端分层约定

后端采用 **按 package 分层** 的架构，非多模块 Gradle 项目：

- `api` — 对外暴露的 REST 接口（Controller）、DTO（Request/Response）。
- `application` — 应用服务层，编排领域对象完成业务用例，处理事务边界。
- `domain` — 核心领域模型（Entity）、Repository 接口、领域服务（如有）。
- `infrastructure` — 技术实现细节：Repository 的 JDBC 实现、框架配置、外部调用封装。

**依赖方向**：api → application → domain ← infrastructure。domain 不依赖任何外层 package。

---

## 构建与运行命令

### 1. 启动数据库（必需）

PostgreSQL 采用**独立部署**，需在宿主机安装并运行 PostgreSQL 服务。

```bash
# macOS (Homebrew)
brew install postgresql@16
brew services start postgresql@16

# Linux (systemd)
sudo systemctl start postgresql
```

数据库运行在 `localhost:5432`。首次部署需手动创建数据库和用户，并按顺序执行初始化脚本：

```bash
# 创建用户和数据库
psql -U postgres -c "CREATE USER stock WITH PASSWORD 'stock';"
psql -U postgres -c "CREATE DATABASE security_analyze OWNER stock;"

# 按顺序执行初始化脚本
for f in backend/src/main/resources/db/migration/V*.sql; do
    psql -h localhost -p 5432 -U stock -d security_analyze -f "$f"
done
```

### 2. 后端（backend/）

```bash
cd backend

# 开发运行
./gradlew bootRun

# 构建可执行 JAR
./gradlew bootJar

# 运行测试（当前无实际测试文件，但已配置 JUnit Platform）
./gradlew test
```

后端默认监听 `8080`，可通过环境变量覆盖数据库连接：
- `DB_HOST`（默认 localhost）
- `DB_PORT`（默认 5432）
- `DB_NAME`（默认 security_analyze）
- `DB_USER`（默认 stock）
- `DB_PASSWORD`（默认 stock）

### 3. 前端（frontend/）

```bash
cd frontend

# 安装依赖
npm install

# 开发服务器（带热更新，代理 /api 到 localhost:8080）
npm run dev

# 生产构建（输出到 dist/）
npm run build

# 预览生产构建
npm run preview
```

前端开发服务器运行在 `3000`，通过 `vite.config.ts` 中的 `proxy` 将 `/api` 请求转发到后端 `8080`。

### 4. 数据采集（collector/）

```bash
cd collector

# 安装依赖（推荐 Poetry）
poetry install

# 或使用 pip
pip install -r requirements.txt

# 启动调度器（常驻进程，当前无默认定时任务）
python main.py

# 手动执行一次全量公司信息采集
python main.py --run-company

# 按公司名称或股票代码采集
python main.py --company 贵州茅台
python main.py --company 600519

# 手动执行一次全量财务报告采集（默认每批100家）
python main.py --run-finance

# 恢复中断的财务报告采集（从断点继续，跳过已成功的股票）
python main.py --run-finance --finance-session-id <uuid>

# 按股票代码采集指定公司财务报告
python main.py --finance 600519
```

采集模块通过 `.env` 文件或环境变量读取数据库配置，模板见 `collector/.env.example`。

---

## 代码风格与开发约定

### 通用约定

- 项目注释、文档、日志、用户界面文本 **主要使用中文**。
- 代码标识符（类名、方法名、变量名）使用英文，遵循各语言惯例。

### 后端（Java）

- 使用 **Lombok**（`@Data`、`@RequiredArgsConstructor`）减少样板代码。
- 使用 **Spring Data JDBC**（`@Table`、`@Id`），非 JPA/Hibernate。自定义查询通过 `NamedParameterJdbcTemplate` 手写 SQL 实现。
- DTO 与 Domain Entity 分离：API 层只返回 DTO，不直接暴露 Entity。
- SQL 关键字与字段名使用 snake_case，Java 实体使用 camelCase，由 JDBC Template 的 RowMapper 手动映射。
- Controller 统一返回 `ResponseEntity<T>`。

### 前端（TypeScript / Vue）

- 使用 `<script setup lang="ts">` 单文件组件写法。
- 类型定义集中放在 `src/types/` 目录，API 层返回类型需与后端 DTO 严格对齐。
- 使用 Element Plus 组件，图标来自 `@element-plus/icons-vue`。
- 路由采用懒加载 `() => import('@/views/...')`。
- API baseURL 通过 `import.meta.env.VITE_API_BASE_URL` 读取，开发时留空则 fallback 到 `http://localhost:8080/api`。

### 数据采集（Python）

- 使用 `logging` 模块记录日志，格式包含时间、logger 名、级别、消息。
- 数据源封装在 `sources/` 中，预留多数据源扩展接口（当前仅实现 `AkshareSource`）。
- 数据库操作封装在 `db/postgres.py`，使用 `psycopg`（版本 3）连接 PostgreSQL。
- 任务类（如 `CompanyTask`、`FinanceTask`）负责：拉取数据 → 解析清洗 → upsert 到数据库。
- `FinanceTask` 支持 **Session 级故障恢复**：全量采集任务启动时自动生成 `session_id`（UUID），逐只股票将处理状态写入 `collector_task_progress`；中断后可通过 `--finance-session-id` 恢复，自动跳过已成功的股票并重试失败的。
- 日期格式统一为 `YYYY-MM-DD`，注册资本单位为 **万元**。

---

## 测试策略

> **当前状态**：测试基础设施已配置，采集模块已补充 `test_finance_task_recovery.py` 验证 Session 断点续传逻辑。新增功能时应补充测试。

- **后端**：Gradle 已配置 `spring-boot-starter-test`、`spring-security-test`、JUnit Platform。请在 `backend/src/test/` 下按 package 镜像结构编写单元测试与集成测试。
- **前端**：尚未配置测试框架。如需添加，建议引入 Vitest + Vue Test Utils。
- **数据采集**：尚未配置测试框架。建议对 `sources/` 和 `tasks/` 中的核心逻辑编写 pytest 单元测试，数据源侧使用 mock 避免实际调用 akshare。

---

## 安全注意事项

- **当前阶段**：已启用 JWT + RBAC 认证。`/api/auth/**` 和 `/api/admin/auth/**` 公开，其余 `/api/**` 需认证，`/api/admin/**` 仅 ADMIN 可访问。CSRF 关闭，CORS 允许 `localhost:3000`。
- 生产环境部署前必须：
  - 修改默认管理员密码 `admin123`。
  - 配置合理的 CORS 白名单，禁止通配符 `*`。
  - 数据库密码应使用强密码，并通过环境变量或密钥管理服务注入，禁止硬编码。
  - JWT Secret 应使用强随机字符串，通过环境变量注入，禁止硬编码。
- 采集模块直接操作数据库，不暴露 HTTP 服务，因此无接口攻击面，但需注意 akshare 数据源返回数据的异常处理，避免脏数据入库。

---

## 数据库与迁移

- 初始化脚本位于 `backend/src/main/resources/db/migration/V1__create_company_table.sql`。
- 当前采用 **手动管理 SQL 脚本** 的方式（Flyway 风格命名），需按版本顺序手动执行。
- 后续如需正式引入 Flyway 或 Liquibase，请在 `build.gradle` 中添加对应依赖。

### company 表核心字段

| 字段 | 说明 |
|------|------|
| stock_code | 股票代码，唯一索引 |
| stock_name | 公司简称 |
| industry | 所属行业 |
| region | 地区（省份/直辖市） |
| establish_date | 成立日期 |
| registered_capital | 注册资本（万元） |
| listing_date | 上市日期 |
| market | 市场板块：SH / SZ / BJ |

---

## 部署说明

- 当前为 **开发阶段单体部署**：
  - PostgreSQL 独立安装部署（Homebrew / 系统包管理器）。
  - 前端开发时使用 `npm run dev`。
  - 后端开发时使用 `./gradlew bootRun`。
  - 采集模块按需手动执行或常驻调度器。
- 生产部署建议：
  - 前端：`npm run build` 产出 `dist/`，通过 Nginx 托管静态资源。
  - 后端：`./gradlew bootJar` 产出可执行 JAR，通过 `java -jar` 运行。
  - 采集模块：作为 cron job 或 systemd 服务运行。
  - 数据库：使用独立部署的 PostgreSQL 或托管云数据库，做好备份策略。

---

## 关键文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| 系统设计 | `docs/plans/2026-04-30-stock-analysis-system-design.md` | 技术选型、模块划分、数据流、部署形态 |
| 模块设计 | `docs/wiki/module-design.md` | 7 大功能模块的职责边界与分层归属 |
| API 契约 | `docs/wiki/api-company.md` | 公司列表、公司详情接口的字段定义与约束 |

---

## 给 AI 助手的快速参考

- 新增后端模块时，复制 `company/` 或 `auth/` / `admin/` / `user/` 的 package 结构（api / application / domain / infrastructure）。
- 新增前端页面时，在 `src/views/` 创建组件，在 `src/router/index.ts` 注册路由，在 `src/api/` 添加接口封装。
- 新增采集任务时，在 `collector/tasks/` 创建 Task 类，在 `collector/sources/` 如需新增数据源则继承风格保持一致。
- 修改数据库 schema 时，新建 `Vx__description.sql` 脚本，并同步更新 Java Entity 与 Python 的 upsert 逻辑。
