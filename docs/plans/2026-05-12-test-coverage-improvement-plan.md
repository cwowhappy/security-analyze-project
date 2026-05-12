# 证券分析系统 — 测试覆盖率分析与提升计划

> 生成日期：2026-05-12
> 涵盖模块：前端 (frontend)、后端 (backend)、数据采集 (collector)

---

## 一、当前测试执行结果总览

| 模块 | 测试框架 | 测试文件数 | 测试用例数 | 通过率 | 语句/指令覆盖 | 分支覆盖 | 行覆盖 |
|------|----------|-----------|-----------|--------|--------------|----------|--------|
| 前端 | Vitest + jsdom | 2 | 6 | ✅ 100% | **92.3%** | **44.4%** | **96.0%** |
| 后端 | JUnit 5 + Testcontainers | 26 | 约 200+ | ✅ 100% | **66.9%** | **38.0%** | **70.5%** |
| 数据采集 | pytest | 13 | 62 | ✅ 100% | **72.0%** | — | **72.0%** |

> ⚠️ **注意**：前端"高覆盖率"是假象——仅 2/43 个源码文件有测试，其余 41 个文件零覆盖。高数值来自已测文件的局部计算。

---

## 二、分模块详细分析

### 2.1 前端 (frontend/)

#### 现状
- **源码文件**：43 个（`.ts` / `.vue`）
- **已测文件**：仅 2 个
  - `components/base/BaseButton.vue` — 100% 覆盖
  - `composables/useRequest.ts` — 90.9% 语句，分支 44.4%
- **未测文件**：41 个，涵盖：
  - **Views**（12 个）：`HomeView.vue`、`StockListView.vue`、`LoginView.vue` 等全部页面
  - **Components**（9 个）：`UserTable.vue`、`LoginLogTable.vue`、`VerifyStatus.vue` 等
  - **Stores**（8 个）：`auth.ts`、`stock.ts`、`company.ts`、`collection.ts` 等 Pinia Store
  - **API 模块**（4 个）：`collection.ts`、`stock.ts`、`company.ts`
  - **Types**（4 个）：`api.ts`、`stock.ts`、`company.ts`、`collection.ts`
  - **Utils / Router**（2 个）：`request.ts`、`router/index.ts`

#### 核心问题
1. **测试数量严重不足**：41/43 文件无单元测试。
2. **分支覆盖薄弱**：`useRequest.ts` 的异常分支、`loading` 状态分支未覆盖。
3. **无组件交互测试**：表格排序、分页、表单验证、弹窗交互等均未测试。
4. **无 Store 测试**：Pinia Store 的状态变更、Actions 调用 API 的逻辑未验证。
5. **无集成测试**：页面路由、组件组合、API → Store → View 的数据流未验证。

---

### 2.2 后端 (backend/)

#### 现状
- **源码文件**：110 个 `.java`
- **测试文件**：26 个
- **JaCoCo 覆盖率**：指令 66.9% | 分支 38.0% | 行 70.5% | 方法 66.5% | 类 89.3%

#### 高覆盖领域（≥80%）
| 包 | 指令覆盖 | 说明 |
|---|---|---|
| `collection.infrastructure.persistence.mapper` | 100% | `CollectionTaskRowMapper` |
| `company.infrastructure.persistence.mapper` | 99% | `CompanyRowMapper` |
| `stock.infrastructure.persistence.mapper` | 99% | `StockRowMapper` |
| `company.application.service.impl` | 100% | `CompanyAppServiceImpl` |
| `stock.application.service.impl` | 100% | `StockAppServiceImpl` |
| `interfaces.rest.response` | 100% | `ApiResponse` |
| `interfaces.filter` | 95% | `MdcFilter` |
| `collection.infrastructure.persistence.repository` | 87% | `JdbcCollectionTaskRepository` |
| `interfaces.rest.controller` | 88% | 各 Controller |

#### 零覆盖 / 极低覆盖领域（<50%）
| 包 | 指令覆盖 | 影响文件数 | 说明 |
|---|---|---|---|
| `interfaces.rest.request` | **0%** | 8 个 | `LoginRequest`、`RegisterRequest`、`Create*Request` 等全部 DTO 无测试 |
| `shared.dto` | **0%** | 4 个 | `PageQuery`、`PageResult`、`CompanyBriefDTO`、`StockBriefDTO` |
| `*.*.dto` / `*.*.command` | **0%** | 10+ 个 | 各模块 DTO、Command 对象 |
| `*.*.entity` | **0%** | 8 个 | JPA/JDBC Entity 类 |
| `*.*.repository` (接口) | **0%** | 7 个 | Repository 接口声明 |
| `shared.mail` | **5%** | 2 个 | `SmtpMailService`(0%)、`ConsoleMailService`(14%) |
| `user.infrastructure.persistence.repository` | **35%** | 5 个 | `JdbcUserRepository`(14%)、`JdbcTokenSessionRepository`(0%) 等 |
| `user.infrastructure.persistence.mapper` | **40%** | 4 个 | `UserRowMapper`(3%)、`EmailVerificationRowMapper`(5%) |
| `user.application.service.impl` | **58%** | 6 个 | `UserAppServiceImpl`(0%)、`TokenBlacklistServiceImpl`(7%)、`EmailVerificationServiceImpl`(4%) |
| `user.application.service` | **9%** | 1 个 | `LoginAttemptRecorder` |
| `config` | **55%** | 4 个 | `JwtTokenProvider`(7%) |
| `interfaces.rest.support` | **0%** | 1 个 | `AuthContextHelper` |
| `interfaces.rest.advice` | **68%** | 1 个 | `GlobalExceptionHandler` 异常分支未覆盖 |
| `stock.infrastructure.persistence.repository` | **46%** | 1 个 | `JdbcStockRepository` 部分方法未测 |

#### 核心问题
1. **DTO / Entity / Command 全面裸奔**：20+ 个纯数据类零覆盖。虽然逻辑简单，但构造器、Builder、校验注解变更时无回归保护。
2. **分支覆盖率仅 38%**：大量 `if (obj == null)`、`try/catch`、权限校验、参数校验分支未走。
3. **User 模块严重缺测**：认证、邮箱验证、Token 黑名单、密码重置等核心安全逻辑几乎无测。
4. **Mail 服务零覆盖**：生产环境切到 SMTP 后无测试保障。
5. **缺少 Contract 测试**：REST API 的 request/response 序列化/反序列化未系统验证。

---

### 2.3 数据采集 (collector/)

#### 现状
- **源码文件**：约 20 个 `.py`
- **测试文件**：13 个（全为单元测试）
- **pytest 覆盖率**：72%（737 statements, 205 missed）

#### 高覆盖模块（≥90%）
| 文件 | 覆盖 | 说明 |
|---|---|---|
| `core/domain/*.py` | 100% | `CollectionTask`、`Company`、`Stock` 领域模型 |
| `adapters/db_*_repository.py` | 92-100% | 数据库仓储实现 |
| `config.py` | 100% | Pydantic Settings 配置 |
| `cli.py` | 97% | CLI 入口 |
| `task_executor.py` | 98% | 任务执行器 |
| `infrastructure/db.py` | 98% | 数据库连接池 |
| `infrastructure/logging/config.py` | 100% | 日志配置 |

#### 低覆盖模块（<50%）
| 文件 | 覆盖 | 缺失行 | 说明 |
|---|---|---|---|
| `scripts/field_supplement.py` | **18%** | 27-36, 41-46, 51-56, 67-120, 133-165, 175-202 | 字段补全脚本 |
| `scripts/company_full.py` | **24%** | 26-31, 36-42, 51-90, 101-141 | 公司全量采集脚本 |
| `scripts/stock_full.py` | **28%** | 25, 29, 32-41, 49, 52-61, 66-71, 80-134 | 股票全量采集脚本 |

#### 核心问题
1. **scripts/ 目录为黑盒**：3 个核心采集脚本合计仅 20% 左右覆盖，这些是直接面向东方财富 API 的代码，风险最高。
2. **集成测试缺失**：`tests/integration/` 目录为空，数据库交互、API 调用链路无端到端验证。
3. **异常路径未测**：`db.py` 第 22 行（连接异常）、`task_executor.py` 第 100/130 行（重试/异常分支）未覆盖。
4. **无 Mock Server 测试**：东方财富 API 的响应解析、限流处理、重试逻辑依赖真实网络，无法稳定复测。

---

## 三、提升测试覆盖率的具体建议

### 3.1 前端

#### 短期（1-2 周）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P0 | 为核心 Composables 补充测试：`useRequest.ts` 异常分支、`loading` 状态 | 2h | 分支 +20% |
| P0 | 为 `request.ts`（Axios 封装）写单元测试：拦截器、错误处理、Token 刷新 | 4h | 新增覆盖文件 |
| P1 | 为 Pinia Stores 写测试：`auth.ts`、`stock.ts`、`company.ts` | 6h | 新增 8 文件覆盖 |
| P1 | 为 API 模块写 Mock 测试 | 4h | 新增 4 文件覆盖 |

#### 中期（2-4 周）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P1 | 为 Base Components 写测试：表格、表单、Badge | 8h | 新增 6 文件覆盖 |
| P2 | 为 Views 写组件测试（挂载 + 路由 + Store 交互） | 16h | 新增 12 文件覆盖 |
| P2 | 引入 Vue Test Utils + MSW 做 API Mock | 4h | 基础设施 |

#### 长期（1-2 月）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P2 | Playwright / Cypress E2E 测试：登录 → 股票列表 → 详情页 | 16h | 端到端保障 |
| P3 | 视觉回归测试（可选） | 8h | UI 稳定性 |

---

### 3.2 后端

#### 短期（1-2 周）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P0 | **User 模块补测**：`UserAppServiceImpl`、`EmailVerificationServiceImpl`、`TokenBlacklistServiceImpl` | 12h | 指令 +8-10% |
| P0 | **Mail 服务测试**：`SmtpMailService`（Mock JavaMailSender）、`ConsoleMailService` | 3h | 指令 +3% |
| P0 | **RowMapper 补测**：`UserRowMapper`、`EmailVerificationRowMapper` | 4h | 指令 +2% |
| P1 | **Repository 补测**：`JdbcUserRepository`、`JdbcTokenSessionRepository`、`JdbcEmailVerificationRepository` | 8h | 指令 +5% |
| P1 | **JwtTokenProvider 测试**：Token 生成、解析、过期校验 | 3h | 指令 +2% |

#### 中期（2-4 周）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P1 | **DTO / Request / Command 测试**：使用 `assertj` 或 `jqwik` 做属性测试 | 6h | 指令 +5% |
| P1 | **异常分支覆盖**：GlobalExceptionHandler、各 Service 的 `catch` 块 | 6h | 分支 +10-15% |
| P2 | **AuthContextHelper / WebConfig / MdcFilter 补充测试** | 3h | 指令 +1% |
| P2 | **Controller Contract 测试**：使用 `@WebMvcTest` + `MockMvc` 验证序列化/反序列化 | 8h | 指令 +3% |

#### 长期（1-2 月）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P2 | **集成测试扩展**：基于 `AbstractIntegrationTest` + Testcontainers，覆盖关键业务流程 | 16h | 端到端保障 |
| P3 | **ArchUnit 架构测试扩展**：验证分层依赖、命名规范 | 4h | 架构保障 |
| P3 | **突变测试 (PIT)**：验证测试有效性 | 8h | 测试质量 |

---

### 3.3 数据采集 (collector/)

#### 短期（1-2 周）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P0 | **脚本核心逻辑抽离**：将 `scripts/*.py` 中 HTTP 调用与业务逻辑分离，使业务逻辑可单元测试 | 6h | scripts 覆盖 +50% |
| P0 | **Mock HTTP 测试**：使用 `responses` / `pytest-httpx` 模拟东方财富 API 返回 | 4h | 新增覆盖 |
| P1 | **task_executor / db 异常分支测试**：连接失败、重试耗尽、事务回滚 | 3h | 覆盖 +3% |

#### 中期（2-4 周）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P1 | **集成测试填充**：`tests/integration/` 目录补充数据库读写、完整采集链路测试 | 8h | 端到端保障 |
| P2 | **数据校验测试**：采集回来的字段格式、类型、空值处理 | 4h | 数据质量 |

#### 长期（1-2 月）
| 优先级 | 目标 | 工作量 | 预期增益 |
|--------|------|--------|----------|
| P2 | **录制-回放测试**：捕获真实 API 响应作为 Fixture，离线运行 | 6h | 稳定性 |
| P3 | **性能/限流测试**：并发采集时的速率控制、退避策略 | 4h | 可靠性 |

---

## 四、分阶段实施计划

### 阶段 1：止血与核心覆盖（第 1-2 周）

**目标**：将后端的 User 安全模块、前端核心逻辑、Collector 脚本提升到可用水平。

| 模块 | 任务 | 负责人建议 | 验收标准 |
|------|------|-----------|----------|
| 后端 | `UserAppServiceImpl` 单元测试 | 后端 dev | 覆盖注册/登录/修改密码主路径 |
| 后端 | `JwtTokenProvider` 单元测试 | 后端 dev | Token 生成/解析/过期 100% 覆盖 |
| 后端 | `SmtpMailService` / `ConsoleMailService` Mock 测试 | 后端 dev | 邮件发送主路径覆盖 |
| 前端 | `useRequest.ts` 分支补全 | 前端 dev | 分支覆盖 ≥80% |
| 前端 | `request.ts` 拦截器测试 | 前端 dev | 错误处理、Token 刷新覆盖 |
| 前端 | Pinia `auth.ts` Store 测试 | 前端 dev | login/logout/userInfo 覆盖 |
| Collector | `scripts/` 核心逻辑抽离 + 单元测试 | 采集 dev | 3 个脚本业务逻辑 ≥60% |
| Collector | `db.py` 异常分支测试 | 采集 dev | 连接失败路径覆盖 |

**预期效果**：
- 后端指令覆盖：66.9% → **72-75%**
- 后端分支覆盖：38.0% → **45-50%**
- 前端语句覆盖：92.3% → **保持**（但有效覆盖文件从 2 个增至 10+）
- Collector 覆盖：72% → **75-78%**

---

### 阶段 2：补齐骨架（第 3-4 周）

**目标**：让 DTO、Entity、Mapper、Repository 等"骨架代码"有基本回归保护。

| 模块 | 任务 | 验收标准 |
|------|------|----------|
| 后端 | 所有 `*RowMapper` 100% 覆盖 | 5 个 Mapper |
| 后端 | 所有 `Jdbc*Repository` 集成测试 | 7 个 Repository |
| 后端 | `GlobalExceptionHandler` 异常分支 | 分支 +10% |
| 后端 | 关键 DTO / Request 的构造器/Getter 测试 | `LoginRequest`、`RegisterRequest` 等 |
| 前端 | 所有 API 模块 Mock 测试 | 4 个模块 |
| 前端 | 所有 Stores 单元测试 | 7 个 Store |
| 前端 | `BaseButton` 之外的 Components 测试 | `UserTable`、`VerifyStatus` 等 |
| Collector | `task_executor` 重试/并发逻辑测试 | 异常分支覆盖 |
| Collector | 集成测试基类 + 1 个完整链路测试 | `tests/integration/test_collection_flow.py` |

**预期效果**：
- 后端指令覆盖：72-75% → **78-82%**
- 后端分支覆盖：45-50% → **55-60%**
- 前端有效覆盖文件：10+ → **25+**
- Collector 覆盖：75-78% → **80-85%**

---

### 阶段 3：深度与端到端（第 5-8 周）

**目标**：提升分支覆盖率，建立集成/E2E 测试体系。

| 模块 | 任务 | 验收标准 |
|------|------|----------|
| 后端 | Controller `@WebMvcTest` Contract 测试 | 7 个 Controller 的请求/响应序列化 |
| 后端 | 集成测试扩展：注册 → 登录 → 查询股票 → 创建采集任务 | 1-2 个关键业务流程 |
| 后端 | 突变测试 (PIT) 试点 | `user` 模块 |
| 前端 | Views 组件测试 | 关键页面挂载 + 交互 |
| 前端 | Playwright E2E 试点：登录 → 股票列表 | 1 条核心用户旅程 |
| Collector | 录制-回放 Fixture | 东方财富 API 典型响应 |
| Collector | 限流/退避策略验证 | 并发控制逻辑 |

**预期效果**：
- 后端指令覆盖：78-82% → **85%+**
- 后端分支覆盖：55-60% → **70%+**
- 前端语句覆盖（真实）：**40-50% 文件有测试**
- Collector 覆盖：**85-90%**

---

## 五、基础设施与规范建议

### 5.1 CI 集成

在 GitHub Actions / GitLab CI 中加入：

```yaml
# 前端
- run: cd frontend && npm run test -- --run --coverage
  # 阈值：语句 ≥80%，分支 ≥60%

# 后端
- run: cd backend && ./gradlew test jacocoTestReport
  # 阈值：指令 ≥70%，分支 ≥50%

# Collector
- run: cd collector && poetry run pytest --cov=data_collector --cov-fail-under=70
```

### 5.2 覆盖率门禁

| 模块 | 当前 | 阶段 1 | 阶段 2 | 阶段 3 |
|------|------|--------|--------|--------|
| 后端指令 | 66.9% | 70% | 75% | 80% |
| 后端分支 | 38.0% | 45% | 55% | 65% |
| Collector | 72.0% | 75% | 80% | 85% |
| 前端文件覆盖数 | 2/43 | 10/43 | 25/43 | 35/43 |

### 5.3 测试命名与组织规范

```
backend/src/test/java/...
├── unit/                    # 纯单元测试（无 Spring 上下文）
│   ├── service/
│   ├── domain/
│   └── mapper/
├── integration/             # 集成测试（@SpringBootTest + Testcontainers）
│   ├── repository/
│   └── controller/
└── architecture/            # ArchUnit 测试

frontend/src/
├── components/__tests__/    # 组件测试
├── composables/__tests__/   # Composable 测试
├── stores/__tests__/        # Store 测试
├── api/__tests__/           # API 模块测试
└── e2e/                     # Playwright E2E

collector/tests/
├── unit/                    # 单元测试
├── integration/             # 集成测试（DB + Mock HTTP）
└── fixtures/                # API 响应录制
```

### 5.4 测试数据管理

- **后端**：使用 Testcontainers PostgreSQL + Flyway 迁移脚本，每个测试类独立数据库。
- **前端**：使用 MSW (Mock Service Worker) 统一拦截 HTTP，避免直接 Mock Axios。
- **Collector**：使用 `responses` 库 Mock `requests`，录制的 Fixture 存入 `tests/fixtures/`。

---

## 六、风险与注意事项

1. **东方财富 API 不稳定**：Collector 集成测试不应依赖真实网络，必须使用录制 Fixture。
2. **前端覆盖率计算陷阱**：Vitest 默认只统计被导入的文件。大量未测文件不会拉低"All files"覆盖率数值，需关注**文件覆盖数**而非单纯百分比。
3. **Testcontainers 启动慢**：后端集成测试较多时，CI 时间可能显著增加。可考虑：复用容器、`@TestInstance(PER_CLASS)`、并行测试。
4. **JWT / 密码等安全逻辑**：User 模块测试涉及密码哈希、Token 签发，注意测试中使用弱参数或固定密钥。
5. **邮件服务测试**：`SmtpMailService` 测试时需 Mock `JavaMailSender`，避免真实发信。

---

## 七、附录：当前测试文件清单

### 前端 (2 个)
- `src/components/__tests__/BaseButton.spec.ts`
- `src/composables/__tests__/useRequest.spec.ts`

### 后端 (26 个)
- `AbstractIntegrationTest.java`
- `ArchitectureTest.java`
- `ApiResponseTest.java`
- `GlobalExceptionHandlerTest.java`
- `SharedExceptionTest.java`
- `AuthControllerTest.java`
- `CompanyControllerTest.java`
- `StockControllerTest.java`
- `CollectionTaskControllerTest.java`
- `AdminUserControllerTest.java`
- `AdminLoginLogControllerTest.java`
- `AuthAppServiceTest.java`
- `AdminUserAppServiceImplTest.java`
- `AdminLogAppServiceImplTest.java`
- `LoginLogServiceImplTest.java`
- `StockAppServiceTest.java`
- `CompanyAppServiceTest.java`
- `CollectionTaskAppServiceTest.java`
- `JdbcStockRepositoryTest.java`
- `JdbcCompanyRepositoryTest.java`
- `JdbcCollectionTaskRepositoryTest.java`
- `JdbcLoginLogRepositoryTest.java`
- `JdbcPasswordResetRepositoryTest.java`
- `StockRowMapperTest.java`
- `CompanyRowMapperTest.java`
- `CollectionTaskRowMapperTest.java`

### Collector (13 个)
- `tests/unit/test_cli.py`
- `tests/unit/test_collection_task_domain.py`
- `tests/unit/test_company_domain.py`
- `tests/unit/test_config.py`
- `tests/unit/test_db.py`
- `tests/unit/test_db_collection_task_repository.py`
- `tests/unit/test_db_company_repository.py`
- `tests/unit/test_db_stock_repository.py`
- `tests/unit/test_logging_config.py`
- `tests/unit/test_main.py`
- `tests/unit/test_stock_domain.py`
- `tests/unit/test_task_executor.py`
