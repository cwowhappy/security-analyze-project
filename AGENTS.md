# AGENTS.md — 证券分析系统 (Security Analyze Project)

> 本文件面向 AI 编程助手。阅读者应对本项目一无所知，所有信息必须基于实际代码与配置文件，不做假设。

---

## 项目概览

本项目是一个**证券分析系统**，采用前后端分离 + 数据采集的三模块架构：

| 模块 | 目录 | 技术栈 | 职责 |
|------|------|--------|------|
| 前端 | `frontend/` | Vue 3 + TypeScript + Vite | 用户界面 |
| 后端 | `backend/` | Java 21 + Spring Boot 3.5 + Gradle | REST API 与业务逻辑 |
| 数据采集 | `collector/` | Python 3.11 + Poetry | 股票数据采集（主要来源：东方财富） |

项目当前处于早期阶段，部分目录仅包含 `.gitkeep` 占位文件，待后续填充。

---

## 各模块详细说明

### 1. 后端 (`backend/`)

#### 技术栈
- **构建工具**：Gradle 9.4.1（Kotlin DSL），通过 Wrapper 调用 (`./gradlew`)
- **JDK**：Java 21（`JavaLanguageVersion.of(21)`）
- **框架**：Spring Boot 3.5.0
- **数据库**：PostgreSQL
- **迁移工具**：Flyway（`flyway-core` + `flyway-database-postgresql`）
- **其他**：Lombok（编译时代码生成）、Spring Boot Actuator（监控端点）
- **测试**：JUnit 5（`spring-boot-starter-test`）

#### 项目结构
```
backend/
├── build.gradle.kts          # Gradle 构建配置（Kotlin DSL）
├── settings.gradle.kts       # 根项目名：security-analyze-backend
├── gradle/wrapper/           # Gradle Wrapper
├── src/main/java/org/cwowhappy/securityanalyze/
│   ├── SecurityAnalyzeApplication.java   # Spring Boot 入口
│   └── config/.gitkeep       # 配置类占位
├── src/main/resources/
│   ├── application.yml       # 主配置文件
│   └── db/migration/.gitkeep # Flyway 迁移脚本占位
└── src/test/java/org/cwowhappy/securityanalyze/.gitkeep
```

#### 关键配置 (`application.yml`)
- 服务端口：`8080`
- 数据库连接：`jdbc:postgresql://localhost:5432/db-security-analyze`
- 数据库凭据通过环境变量注入：`DB_USER` / `DB_PASSWORD`
- Flyway 启用，迁移路径 `classpath:db/migration`，`baseline-on-migrate: true`
- Actuator 暴露端点：`health`, `info`, `metrics`；`health` 始终显示详情

#### 常用命令
```bash
cd backend
./gradlew build      # 构建
./gradlew bootRun    # 运行应用
./gradlew test       # 运行测试
```

---

### 2. 数据采集模块 (`collector/`)

#### 技术栈
- **语言**：Python 3.11+
- **依赖管理**：Poetry（`poetry-core` 构建后端）
- **数据验证**：Pydantic v2 + `pydantic-settings`
- **配置解析**：PyYAML（字段映射外置配置）
- **HTTP 请求**：`requests`
- **测试**：pytest + pytest-asyncio

#### 项目结构（v3.0 通用采集管道）
```
collector/
├── pyproject.toml            # Poetry 项目配置
├── poetry.lock               # 锁定依赖版本
├── .env.example              # 环境变量示例
├── config/mappings/          # YAML 字段映射配置
│   ├── stock_basic.yaml
│   ├── company_info.yaml
│   ├── financial_income.yaml
│   ├── financial_balance.yaml
│   ├── financial_cashflow.yaml
│   └── financial_indicator.yaml
├── src/data_collector/
│   ├── cli.py                # CLI 入口
│   ├── config.py             # pydantic-settings 配置（含采集管道参数）
│   ├── task_executor.py      # 任务执行器（按 task_type + mode 路由）
│   ├── core/
│   │   ├── domain/           # 领域模型（Stock、Company、CollectionTask 等）
│   │   ├── config/           # 配置加载器
│   │   │   └── field_mapping_config.py
│   │   └── pipeline/         # 通用采集管道核心组件
│   │       ├── adaptive_request_engine.py  # 智能调速
│   │       ├── field_mapper.py             # 字段映射与转换
│   │       ├── source_fallback_pipeline.py # 多源串行 fallback
│   │       ├── stock_state_tracker.py      # stock 级状态追踪
│   │       └── converters.py               # 转换器注册表
│   ├── adapters/             # 仓库实现 + 数据源适配器
│   │   ├── db_stock_repository.py
│   │   ├── db_company_repository.py
│   │   ├── db_collection_task_repository.py
│   │   ├── db_stock_state_repository.py
│   │   ├── data_source_adapter.py          # 适配器协议
│   │   ├── stock_basic_akshare_adapter.py
│   │   ├── stock_basic_tushare_adapter.py
│   │   ├── company_info_akshare_adapter.py
│   │   ├── company_info_tushare_adapter.py
│   │   ├── financial_sina_adapter.py
│   │   └── financial_indicator_calculated_adapter.py
│   ├── scripts/              # 采集脚本（Legacy，逐步迁移中）
│   │   ├── stock_full.py
│   │   ├── company_full.py
│   │   ├── field_supplement.py
│   │   └── financial_*.py
│   └── infrastructure/       # 数据库连接池、日志配置
└── tests/
    └── unit/
```

#### 环境变量说明（见 `.env.example`）
- **数据库**：`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- **连接池**（可选）：`DB_POOL_MIN_SIZE`, `DB_POOL_MAX_SIZE`
- **采集管道**：
  - `COLLECTION_TTL_HOURS` — stock 级状态有效期（默认 24）
  - `COLLECTION_BATCH_SIZE` — 批处理大小（默认 20）
  - `ADAPTIVE_MIN_DELAY` / `ADAPTIVE_MAX_DELAY` — 调用间隔范围（默认 1~60 秒）
  - `ADAPTIVE_BACKOFF_JITTER` — 退避抖动（默认 0.5）
  - `ADAPTIVE_SUCCESS_THRESHOLD` — 连续成功降速阈值（默认 10）
  - `RETRY_MAX_ATTEMPTS` — 单请求最大重试次数（默认 3）
- **批次失败率阈值**：`BATCH_FAIL_THRESHOLD`（默认 0.1）
- **Tushare Token**：`TUSHARE_TOKEN`（补充数据源使用）

#### 常用命令
```bash
cd collector
poetry install                # 安装依赖
poetry run python -m data_collector   # 运行模块
poetry run pytest             # 运行测试
```

---

### 3. 前端 (`frontend/`)

#### 技术栈
- **框架**：Vue 3.5.32（Composition API + `<script setup>`）
- **语言**：TypeScript ~6.0.2
- **构建工具**：Vite 8.0.10
- **测试**：Vitest 4.1.5 + jsdom + `@vue/test-utils`
- **TS 配置**：采用 Vue 官方推荐的 `"@vue/tsconfig/tsconfig.dom.json"` 作为基础

#### 项目结构
```
frontend/
├── package.json
├── index.html                # HTML 入口
├── vite.config.ts            # Vite 配置
├── vitest.config.ts          # Vitest 配置（含 `@/` -> `src/` 别名）
├── tsconfig.json             # 项目引用配置（引用 app + node）
├── tsconfig.app.json         # 应用 TS 配置
├── tsconfig.node.json        # 构建工具 TS 配置
├── public/                   # 静态资源
│   ├── favicon.svg
│   └── icons.svg             # SVG Sprite（包含社交/文档图标）
└── src/
    ├── main.ts               # 应用入口
    ├── App.vue               # 根组件
    ├── style.css             # 全局样式
    ├── assets/               # 图片资源
    └── components/
        ├── HelloWorld.vue    # 示例组件
        └── __tests__/.gitkeep
```

#### TS 与代码规范
- `tsconfig.app.json` 启用了较严格的检查：
  - `noUnusedLocals: true`
  - `noUnusedParameters: true`
  - `noFallthroughCasesInSwitch: true`
  - `erasableSyntaxOnly: true`
- Vitest 配置中开启 `globals: true`，测试环境为 `jsdom`

#### 常用命令
```bash
cd frontend
npm install        # 安装依赖
npm run dev        # 启动开发服务器
npm run build      # 构建生产包（先执行 vue-tsc 类型检查）
npm run preview    # 预览生产包
npm run test       # 运行 Vitest
npm run test:ui    # 运行 Vitest UI 模式
```

---

## 构建与测试总览

| 模块 | 安装依赖 | 开发启动 | 构建 | 测试 |
|------|----------|----------|------|------|
| 后端 | `./gradlew build` | `./gradlew bootRun` | `./gradlew build` | `./gradlew test` |
| 采集器 | `poetry install` | `poetry run python -m data_collector` | — | `poetry run pytest` |
| 前端 | `npm install` | `npm run dev` | `npm run build` | `npm run test` |

---

## 数据库与运行时架构

- **数据库**：PostgreSQL（本地开发时需在 `localhost:5432` 启动服务）
- **后端端口**：`8080`
- **前端开发服务器**：Vite 默认端口（通常为 `5173`）
- **数据流向**：
  1. `collector` 从外部数据源（东方财富）采集股票数据
  2. 数据写入 PostgreSQL
  3. `backend` 通过 Spring Data JDBC 提供 REST API
  4. `frontend` 调用 API 展示给用户

### 部署与运维相关
- 后端通过 **Spring Boot Actuator** 暴露 `/actuator/health`、`/actuator/info`、`/actuator/metrics`
- 目前**未发现** Docker、docker-compose、Kubernetes 或 CI/CD 配置文件，部署流程尚未定义。

---

## 代码组织约定

### 包命名
- **Java**：`org.cwowhappy.securityanalyze.*`
- **Python**：`stock_collector`

### 目录占位
- 多个目录（如 `backend/config/`、`backend/db/migration/`、`backend/src/test/...`、`collector/src/stock_collector/`、`frontend/src/components/__tests__/`）当前仅含 `.gitkeep`，说明模块骨架已搭好，业务代码待补充。

### Git 忽略
- 根目录 `.gitignore` 统一忽略了：
  - `.kimi/`（Coding Agent 相关）
  - `node_modules/`, `dist/`, `dist-ssr/`（前端）
  - `backend/.gradle/`, `backend/build/`（后端构建产物）
  - `__pycache__/`, `.pytest_cache/`, `*.pyc`（Python）
  - 编辑器文件（`.vscode/*`, `.idea`, `.DS_Store` 等）

---

## 安全注意事项

1. **数据库密码硬编码风险**：
   - 后端 `application.yml` 中 `spring.datasource.password` 使用 `${DB_PASSWORD:SecurityAnalyze@2026}`，虽然支持环境变量注入，但默认值是一个真实格式的密码，生产环境务必通过环境变量覆盖。
   - 采集器 `.env.example` 同样包含明文密码示例。

2. **Actuator 端点暴露**：
   - `management.endpoint.health.show-details: always` 在开发环境方便调试，但生产环境需谨慎评估是否暴露过多信息。

3. **外部数据源采集**：
   - 采集器面向东方财富等外部 API，已配置重试与随机延迟以应对限流。修改并发参数时需特别注意对方站点的访问策略，避免触发封禁。

4. **Secrets 管理**：
   - 目前无 Vault、KMS 或加密配置的迹象，敏感信息均通过环境变量或 `.env` 文件传递，后续如需强化安全应考虑引入 secrets 管理方案。

---

## 给 AI 助手的快速参考

- 本项目是**中文项目**，所有注释、文档、README 均使用中文，新增代码的注释也应优先使用中文。
- 修改任一模块时，请同时检查相关模块的 README 和本文件，确保一致性。
- 后端 Flyway 迁移脚本应放在 `backend/src/main/resources/db/migration/`，命名遵循 `V{version}__{description}.sql`。
- 前端组件使用 Vue 3 Composition API + `<script setup lang="ts">` 模式。
- 采集器使用 Pydantic v2 进行配置与数据模型校验，环境变量通过 `pydantic-settings` 读取。
