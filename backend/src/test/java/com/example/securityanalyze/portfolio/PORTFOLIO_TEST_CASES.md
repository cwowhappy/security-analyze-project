# Portfolio 模块测试用例文档

## 一、覆盖率现状

| 包 | 指令覆盖率 | 分支覆盖率 | 核心盲区 |
|----|-----------|-----------|---------|
| `portfolio.api` | **4%** | **0%** | 全部 Controller、DTO 均无测试 |
| `portfolio.application` | 82% | 58% | TransactionService 异常路径 |
| `portfolio.infrastructure` | 92% | 61% | `PositionRepositoryImpl.findByPortfolioIdWithQuote()` 无测试 |
| `portfolio.domain` | — | — | 简单 POJO，无需单测 |

## 二、本次补充测试范围

聚焦**持仓分析查看功能**（PositionController + PositionRepositoryImpl），兼顾 PortfolioController 基础 CRUD。

### 2.1 PositionRepositoryImpl 集成测试

| 用例编号 | 场景 | 前置条件 | 预期结果 |
|---------|------|---------|---------|
| PR-001 | `findByPortfolioIdWithQuote` 返回完整行情数据 | 存在 portfolio、position、company、company_security、daily_quote 数据 | 返回 Map 包含 stock_name、industry、market、close_price |
| PR-002 | `findByPortfolioIdWithQuote` 无行情数据 | 存在 portfolio、position，但无 daily_quote | close_price 为 null，其他字段正常 |
| PR-003 | `findByPortfolioIdWithQuote` 无证券映射 | 存在 portfolio、position，但 company_security 无对应记录 | stock_name、industry、market 为 null |

### 2.2 PositionController 单元测试（MockMvc）

| 用例编号 | 场景 | 输入 | 预期结果 |
|---------|------|------|---------|
| PC-001 | `GET /positions` 正常返回持仓列表 | portfolioId=1，2 只持仓，有行情 | HTTP 200，JSON 包含 marketValue、floatingPnl、weight（权重之和 100%） |
| PC-002 | `GET /positions` 空持仓 | portfolioId=1，无持仓 | HTTP 200，返回空数组 |
| PC-003 | `GET /positions` 权限拒绝 | portfolioId=1，非当前用户组合 | HTTP 403 或 404（PortfolioAccessDeniedException） |
| PC-004 | `GET /summary` 正常返回汇总 | portfolioId=1，2 只持仓，有行情和交易 | HTTP 200，JSON 包含 portfolioName、totalMarketValue、totalFloatingPnl、totalRealizedPnl |
| PC-005 | `GET /summary` 无持仓 | portfolioId=1，无持仓 | HTTP 200，totalCost=0，totalMarketValue=0 |

### 2.3 PortfolioController 单元测试（MockMvc）

| 用例编号 | 场景 | 输入 | 预期结果 |
|---------|------|------|---------|
| POC-001 | `GET /portfolios` 列表 | 当前用户有 2 个组合 | HTTP 200，返回 2 条记录 |
| POC-002 | `POST /portfolios` 创建 | PortfolioRequest | HTTP 200，返回创建后的组合 |
| POC-003 | `PUT /portfolios/{id}` 更新 | id=1，PortfolioRequest | HTTP 200，返回更新后的组合 |
| POC-004 | `DELETE /portfolios/{id}` 删除 | id=1 | HTTP 204 |

## 三、测试策略

- **Repository 集成测试**：继承 `RepositoryTestBase`，使用 `Testcontainers` + `NamedParameterJdbcTemplate` 直接写入数据，验证 SQL 查询结果。
- **Controller 单元测试**：`@SpringBootTest` + `@AutoConfigureMockMvc` + `@MockitoBean`，仅验证 HTTP 层与 Controller 逻辑，Service 层全部 Mock。
