# Security Analyze Project — Agent Guide

> 本文档面向 AI 编码助手。假设读者对该项目一无所知，阅读后即可开始安全、高效地贡献代码。
> 项目主要使用中文进行注释和文档，因此本文件以中文撰写。

## 1. 项目概述

Security Analyze Project 是一个安全分析平台，包含三个独立模块，统一使用 PostgreSQL 持久化数据：

- **frontend**：Vue 3 单页应用，面向用户的安全分析界面。
- **backend**：Spring Boot REST API，提供认证、安全事件管理、业务逻辑。
- **collector**：Python 数据采集与调度服务，负责从外部安全数据源定时采集事件并写入数据库。

本地开发可通过 `docker compose up -d` 一键启动全栈；也可分别启动各模块进行独立开发。

## 2. 技术栈与版本

| 模块 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + TypeScript + Vite | Vue 3.5.x, TS 5.7.x, Vite 6.x |
| 后端 | Java + Spring Boot + Gradle | Java 25, Spring Boot 4.1.x, Gradle 9.7.0+ |
| 数据收集 | Python + APScheduler + uv | Python 3.12+, APScheduler 3.11.x, uv 0.8.x |
| 数据库 | PostgreSQL | 17.x |

### 2.1 前端关键依赖

- **UI 框架**：Element Plus 2.9.7
- **状态管理**：Pinia 2.3.1
- **路由**：Vue Router 4.5.0
- **HTTP 客户端**：Axios 1.8.4
- **图表**：ECharts 5.6.0 + vue-echarts 7.0.3
- **图标**：@element-plus/icons-vue

### 2.2 后端关键依赖

- **Web**：Spring Boot Web, Validation
- **数据访问**：Spring Data JPA, PostgreSQL Driver, Flyway
- **安全**：Spring Security, jjwt 0.12.6
- **API 文档**：SpringDoc OpenAPI (Swagger UI)
- **工具库**：Lombok, MapStruct
- **测试**：Spring Boot Test, Spring Boot Testcontainers, Testcontainers (PostgreSQL), Rest Assured

### 2.3 数据收集关键依赖

- **调度**：APScheduler 3.11.0
- **ORM**：SQLAlchemy 2.0.36 + psycopg 3.2.4
- **配置**：Pydantic Settings 2.7.1
- **HTTP 客户端**：httpx 0.28.1, requests 2.32.3
- **日志**：loguru 0.7.3
- **测试**：pytest 8.3.5、pytest-asyncio 0.25.3、pytest-cov 6.0.0、pytest-httpx 0.35.0、factory-boy 3.3.1、freezegun 1.5.1
- **可选（Celery）**：celery 5.4.0 + redis 5.2.1

## 3. 项目结构

```
security-analyze-project/
├── frontend/                  # Vue 3 前端服务
│   ├── src/
│   │   ├── api/               # Axios 请求封装
│   │   ├── layouts/           # 页面布局
│   │   ├── router/            # Vue Router 配置
│   │   ├── stores/            # Pinia 状态管理
│   │   ├── views/             # 页面视图
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── Dockerfile
│   └── nginx.conf
├── backend/                   # Spring Boot 后端服务
│   ├── src/main/java/com/example/security/
│   │   ├── config/            # 安全配置、数据初始化
│   │   ├── controller/        # REST 控制器
│   │   ├── domain/entity/     # JPA 实体
│   │   ├── dto/               # 请求/响应 DTO（Java Record）
│   │   ├── exception/         # 全局异常处理
│   │   ├── repository/        # Spring Data JPA 仓库
│   │   ├── security/          # JWT 工具、过滤器、UserDetails
│   │   ├── service/           # 业务服务
│   │   └── SecurityAnalyzeApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml    # 主配置
│   │   ├── application-dev.yml# 开发环境增强日志
│   │   └── db/migration/      # Flyway 数据库迁移脚本
│   ├── build.gradle.kts
│   ├── gradle/libs.versions.toml
│   ├── settings.gradle.kts
│   └── Dockerfile
├── collector/                 # Python 数据收集服务
│   ├── src/
│   │   ├── collectors/        # HTTP 等采集器基类
│   │   ├── db/                # SQLAlchemy 会话与 Base
│   │   ├── models/            # SQLAlchemy 模型
│   │   ├── tasks/             # 定时任务实现
│   │   ├── config.py          # Pydantic Settings 配置
│   │   ├── main.py            # 服务入口
│   │   └── scheduler.py       # APScheduler 调度器
│   ├── tests/                 # 测试目录（当前为空）
│   ├── pyproject.toml
│   ├── uv.lock
│   ├── .env.example
│   └── Dockerfile
├── docker-compose.yml         # 本地全栈编排
├── README.md
└── AGENTS.md                  # 本文件
```

## 4. 构建与运行

### 4.1 环境要求

- Node.js 20/22
- Java 25
- Python 3.12+
- uv（Python 包管理器）
- Docker & Docker Compose

### 4.2 一键启动（推荐本地开发）

```bash
docker compose up -d
```

访问入口：

- 前端：http://localhost
- 后端 API：http://localhost:8080/api/v1
- Swagger UI：http://localhost:8080/swagger-ui/index.html
- Actuator Health：http://localhost:8080/actuator/health

默认管理员账号：

- 用户名：`admin`
- 密码：`admin`

> 生产环境务必修改 `JWT_SECRET` 和默认管理员密码。

### 4.3 各模块独立启动

#### 前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器运行在 http://localhost:5173，Vite 代理将 `/api` 转发到 http://localhost:8080。

#### 后端

```bash
cd backend
./gradlew bootRun
```

#### 数据收集服务

```bash
cd collector
cp .env.example .env
uv sync
uv run src/main.py
```

## 5. 测试命令

### 5.1 前端

```bash
cd frontend
npm run test                # Vitest 单元/组件测试
npm run test -- --coverage  # 生成覆盖率报告
npm run lint                # ESLint
npm run build               # 类型检查 + 生产构建
```

### 5.2 后端

```bash
cd backend
./gradlew test      # JUnit 5 单元/集成测试
./gradlew bootJar   # 构建可执行 jar
```

### 5.3 数据收集服务

```bash
cd collector
uv run pytest       # pytest（默认带覆盖率）
uv run ruff check . # Ruff 代码检查
```

## 6. 代码风格与开发约定

### 6.1 通用原则

- 保持最小变更：修复 bug 时不重构无关代码；新增功能时不引入过度抽象。
- 遵循各模块现有命名、注释和文件组织方式。
- 所有代码注释和文档使用中文（与项目现有风格一致）。

### 6.2 前端

- 使用 Vue 3 `<script setup lang="ts">` 组合式 API。
- 路径别名 `@/` 映射到 `frontend/src/`。
- 使用 Element Plus 组件构建 UI。
- Pinia Store 使用组合式写法（`ref` + `computed`）。
- HTTP 请求统一通过 `src/api/request.ts` 中的 Axios 实例发起，自动携带 `localStorage.token`。

### 6.3 后端

- Java 包名：`com.example.security`。
- 实体类放在 `domain/entity/`，使用 JPA 注解 + Lombok `@Getter`/`@Setter`。
- DTO 使用 Java `record`（如 `LoginRequest`、`ApiResponse`、`LoginResponse`）。
- 控制器使用构造函数注入，REST 路径前缀统一为 `/api/v1`。
- 仓库接口继承 `JpaRepository`。
- 安全配置：JWT 认证过滤器 + Spring Security，公开 `/api/v1/auth/**`、`/swagger-ui/**`、`/v3/api-docs/**`、`/actuator/health`。
- MapStruct 处理器参数已在 `build.gradle.kts` 中配置：`"-Amapstruct.defaultComponentModel=spring"`。

### 6.4 数据收集服务

- 入口：`src/main.py`；调度注册在 `src/scheduler.py`。
- 任务必须继承 `src/tasks/base_task.py` 中的 `BaseTask`，实现 `execute()` 方法。
- 配置使用 `src/config.py` 中的 `Settings`（Pydantic Settings），支持 `.env` 文件。
- 数据库会话使用 `src/db/session.py` 中的 `SessionLocal`。
- 日志统一使用 `loguru`，默认写入 `logs/collector.log`（按 10 MB 轮转，保留 7 天）。
- Ruff 配置：`line-length = 120`，`target-version = "py312"`。
- 测试使用 `pytest`，HTTP 请求使用 `pytest-httpx` 模拟，测试数据使用 `factory-boy` 生成，数据库使用 SQLite 内存库或 Testcontainers PostgreSQL。
- 任务类应支持注入 `session_factory`，便于测试时替换数据库连接。

## 7. 数据库与迁移

- 数据库名：`security_analyze`
- 默认用户/密码：`security` / `security`
- 后端使用 **Flyway** 管理数据库版本，脚本位于 `backend/src/main/resources/db/migration/`。
- 当前初始化脚本：`V1__init.sql`，包含以下表：
  - `users` / `roles` / `user_roles`：用户与角色
  - `security_events`：安全事件（核心）
  - `threat_intels`：威胁情报
  - `collector_tasks`：采集任务元数据
- JPA `ddl-auto` 设置为 `validate`，生产环境不允许自动建表，必须通过 Flyway 迁移。
- 后端启动时会执行 `DataInitializer`，自动创建 `ROLE_ADMIN`、`ROLE_USER` 以及默认管理员账号。

## 8. 安全注意事项

- **JWT Secret**：生产环境必须设置强随机字符串，通过环境变量 `JWT_SECRET` 注入。当前默认值 `change-me-in-production` 仅用于开发。
- **默认账号**：管理员账号 `admin`/`admin` 仅供本地开发，上线前必须修改或禁用。
- **CSRF**：后端配置为无状态 JWT，已关闭 CSRF（`SessionCreationPolicy.STATELESS`）。
- **数据库密码**：避免将真实密码提交到仓库；Docker Compose 中已通过 `${JWT_SECRET:-change-me-in-production}` 提供可覆盖机制。
- **SQL 注入**：后端使用 JPA 参数化查询；collector 使用 SQLAlchemy ORM，避免直接拼接 SQL。
- **依赖安全**：定期运行 `npm audit`、`./gradlew dependencyCheckAnalyze`（如已集成）、`uv pip audit` 检查依赖漏洞。
- **路径遍历**：处理文件路径时，禁止拼接用户输入到绝对路径，避免读取或写入工作目录外的文件。
- ** secrets 文件**：`.env`、SSH 私钥等已加入 `.gitignore`，不要通过任何方式泄露。

## 9. 部署说明

### 9.1 Docker Compose（本地/测试）

直接使用根目录 `docker-compose.yml`，包含 4 个服务：

- `postgres`：PostgreSQL 17 Alpine
- `backend`：Spring Boot 后端
- `frontend`：Nginx 托管的静态前端
- `collector`：Python 数据采集服务

### 9.2 各模块镜像构建

前端：

```bash
cd frontend
docker build -t security-analyze-frontend .
```

后端：

```bash
cd backend
docker build -t security-analyze-backend .
```

数据收集：

```bash
cd collector
docker build -t security-analyze-collector .
```

### 9.3 生产建议

- 使用外部 PostgreSQL 实例，移除 Docker Compose 中的 `postgres` 服务或仅用于本地开发。
- 通过 secrets 管理工具或环境变量注入 `JWT_SECRET`、数据库密码。
- 为 Nginx 配置 HTTPS/TLS，禁止明文传输 JWT。
- 启用后端 Actuator 健康检查，但限制敏感端点暴露。
- collector 服务应配置为单实例运行，避免 APScheduler 任务重复执行（除非切换到 Celery/Redis 分布式调度）。

## 10. 待完善项

- 前端 `LoginView.vue` 登录逻辑为 TODO，尚未真正调用后端 `/api/v1/auth/login`。
- `collector/src/tasks/cve_collector.py` 目前只写入示例数据，未对接真实 CVE/NVD API。
- 前端 `EventsView.vue` 使用静态 Mock 数据，需接入后端 `/api/v1/events`。
- 可考虑统一 API 错误处理、请求 loading、权限控制等前端基础设施。

## 11. 常用命令速查

```bash
# 全栈启动
docker compose up -d

# 前端
cd frontend && npm install && npm run dev

# 后端
cd backend && ./gradlew bootRun

# 数据收集
cd collector && uv sync && uv run src/main.py

# 测试与检查
cd frontend && npm run test && npm run lint
cd backend && ./gradlew test
cd collector && uv run pytest && uv run ruff check .
```
