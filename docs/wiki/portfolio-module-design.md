# 持仓管理模块设计文档

## 概述

本文档定义股票持仓管理模块的完整设计方案，覆盖数据模型、业务规则、分层架构、接口契约与前端规划。

**需求背景**：用户在系统内记录和管理自己的股票持仓，支持多组合（多账户）管理、完整成交记录录入与复盘分析。行情数据每日收盘后同步，不做实时行情。

**文档目的**：为前后端开发、数据库迁移、采集任务提供统一的设计参考。

---

## 数据模型

### 实体关系

```
sys_user (1) ───< portfolio (N) ───< transaction_record (N)
                                      │
                                      └──> position (1 per stock)

daily_quote (N) ──> position 市值计算（查询时关联）
```

---

### 1. portfolio（组合 / 证券账户）

```sql
CREATE TYPE portfolio_type AS ENUM ('REAL', 'SIMULATION');

CREATE TABLE IF NOT EXISTS portfolio (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,                          -- 组合名称，如"华泰证券主账户"
    type portfolio_type NOT NULL DEFAULT 'REAL',         -- REAL: 实盘, SIMULATION: 模拟盘
    broker VARCHAR(100),                                 -- 券商名称（可选）
    description VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,           -- 逻辑删除标志
    deleted_at TIMESTAMP,                                -- 删除时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_portfolio_user ON portfolio(user_id);
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| user_id | BIGINT | FK → sys_user.id, NOT NULL | 所属用户 |
| name | VARCHAR(100) | NOT NULL | 组合名称 |
| type | portfolio_type | NOT NULL, DEFAULT 'REAL' | REAL: 实盘, SIMULATION: 模拟盘 |
| broker | VARCHAR(100) | 可空 | 券商名称 |
| description | VARCHAR(500) | 可空 | 描述 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | 逻辑删除标志 |
| deleted_at | TIMESTAMP | 可空 | 删除时间 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL | 更新时间 |

---

### 2. transaction_record（成交记录）

```sql
CREATE TYPE trade_type AS ENUM (
    'BUY',      -- 买入
    'SELL',     -- 卖出
    'DIVIDEND', -- 现金分红
    'BONUS',    -- 送股
    'RIGHTS',   -- 配股
    'SPLIT',    -- 股份拆分（A股极少见）
    'MERGER',   -- 吸收合并
    'OTHER'     -- 其他
);

CREATE TABLE IF NOT EXISTS transaction_record (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,                            -- 交易日期
    trade_type trade_type NOT NULL,
    price DECIMAL(18, 4),                                -- 成交价格（分红/送股可为空）
    quantity DECIMAL(18, 4) NOT NULL,                    -- 成交股数：始终为正数，方向由 trade_type 决定
    fee DECIMAL(18, 4) NOT NULL DEFAULT 0,               -- 交易费用（佣金、过户费等）
    tax DECIMAL(18, 4) NOT NULL DEFAULT 0,               -- 税费（印花税、红利税等）
    amount DECIMAL(18, 4),                               -- 成交总额 = price * quantity（系统计算）
    realized_pnl DECIMAL(18, 4) DEFAULT 0,               -- 该笔交易产生的已实现盈亏（仅卖出时）
    remark VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,           -- 逻辑删除标志
    deleted_at TIMESTAMP,                                -- 删除时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tx_portfolio ON transaction_record(portfolio_id);
CREATE INDEX idx_tx_stock ON transaction_record(stock_code);
CREATE INDEX idx_tx_date ON transaction_record(trade_date);
CREATE INDEX idx_tx_portfolio_date ON transaction_record(portfolio_id, trade_date);
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| portfolio_id | BIGINT | FK → portfolio.id, NOT NULL | 所属组合 |
| stock_code | VARCHAR(20) | NOT NULL | 股票代码 |
| trade_date | DATE | NOT NULL | 交易日期 |
| trade_type | trade_type | NOT NULL | 交易类型枚举 |
| price | DECIMAL(18,4) | 可空 | 成交价格 |
| quantity | DECIMAL(18,4) | NOT NULL | 成交股数（始终为正） |
| fee | DECIMAL(18,4) | NOT NULL, DEFAULT 0 | 交易费用（佣金、过户费等） |
| tax | DECIMAL(18,4) | NOT NULL, DEFAULT 0 | 税费（印花税、红利税等） |
| amount | DECIMAL(18,4) | 可空 | 成交总额（系统计算） |
| realized_pnl | DECIMAL(18,4) | DEFAULT 0 | 该笔已实现盈亏 |
| remark | VARCHAR(500) | 可空 | 备注 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | 逻辑删除标志 |
| deleted_at | TIMESTAMP | 可空 | 删除时间 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

**成交股数方向约定**：`quantity` 始终存正数，方向由 `trade_type` 决定。
- `BUY` / `RIGHTS`：增加持仓
- `SELL`：减少持仓
- `DIVIDEND`：quantity = 0，仅金额变动
- `BONUS` / `SPLIT`：增加持仓，成本不变

---

### 3. position（持仓汇总快照）

由成交记录驱动更新，用于快速查询当前持仓。

```sql
CREATE TABLE IF NOT EXISTS position (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL,
    current_quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,  -- 当前持仓股数
    total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,        -- 持仓成本（不含交易费用）
    avg_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,          -- 平均成本
    realized_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,      -- 累计已实现盈亏
    first_buy_date DATE,                                 -- 首次买入日期
    last_trade_date DATE,                                -- 最近交易日期
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,           -- 逻辑删除标志
    deleted_at TIMESTAMP,                                -- 删除时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (portfolio_id, stock_code)
);

CREATE INDEX idx_position_portfolio ON position(portfolio_id);
CREATE INDEX idx_position_stock ON position(stock_code);
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 自增主键 |
| portfolio_id | BIGINT | FK → portfolio.id, NOT NULL | 所属组合 |
| stock_code | VARCHAR(20) | NOT NULL | 股票代码 |
| current_quantity | DECIMAL(18,4) | NOT NULL, DEFAULT 0 | 当前持仓股数 |
| total_cost | DECIMAL(18,4) | NOT NULL, DEFAULT 0 | 持仓成本（不含交易费用） |
| avg_cost | DECIMAL(18,4) | NOT NULL, DEFAULT 0 | 平均成本 |
| realized_pnl | DECIMAL(18,4) | NOT NULL, DEFAULT 0 | 累计已实现盈亏 |
| first_buy_date | DATE | 可空 | 首次买入日期 |
| last_trade_date | DATE | 可空 | 最近交易日期 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | 逻辑删除标志 |
| deleted_at | TIMESTAMP | 可空 | 删除时间 |
| updated_at | TIMESTAMP | NOT NULL | 更新时间 |

**唯一约束**：`(portfolio_id, stock_code)` 唯一。注意：逻辑删除后，若同一组合再次买入该股票，可重新生成一条 position 记录。

---

### 删除策略（逻辑删除）

持仓管理模块所有业务表（`portfolio`、`transaction_record`、`position`）统一采用**逻辑删除**，禁止物理删除。

- 所有表均包含 `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` 和 `deleted_at TIMESTAMP` 字段。
- **DELETE 接口**实际执行 `UPDATE`：将 `is_deleted` 置为 `TRUE`，`deleted_at` 置为当前时间。
- **查询接口**自动追加 `WHERE is_deleted = FALSE` 条件，已删除数据对用户不可见。
- **级联行为**：
  - 删除组合（portfolio）时，级联逻辑删除该组合下的所有成交记录（transaction_record）和持仓记录（position）。
  - 删除单条成交记录时，触发该组合的持仓重算；若删除后某股票持仓归零，对应 position 记录也标记为逻辑删除（而非物理删除）。

---

### 4. daily_quote（日行情）

盘后同步，用于计算持仓市值和浮动盈亏。

```sql
CREATE TABLE IF NOT EXISTS daily_quote (
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(18, 4),
    high_price DECIMAL(18, 4),
    low_price DECIMAL(18, 4),
    close_price DECIMAL(18, 4) NOT NULL,                 -- 收盘价，用于计算市值
    volume BIGINT,
    amount DECIMAL(18, 4),                               -- 成交额
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_code, trade_date)
);

CREATE INDEX idx_quote_date ON daily_quote(trade_date);
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| stock_code | VARCHAR(20) | PK(1/2) | 股票代码 |
| trade_date | DATE | PK(2/2) | 交易日期 |
| open_price | DECIMAL(18,4) | 可空 | 开盘价 |
| high_price | DECIMAL(18,4) | 可空 | 最高价 |
| low_price | DECIMAL(18,4) | 可空 | 最低价 |
| close_price | DECIMAL(18,4) | NOT NULL | 收盘价 |
| volume | BIGINT | 可空 | 成交量 |
| amount | DECIMAL(18,4) | 可空 | 成交额 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

---

## 核心业务规则

### 持仓计算逻辑（平均成本法）

每次**新增 / 修改 / 删除**成交记录后，触发对应 `portfolio_id + stock_code` 的持仓重算：

```
1. 筛选该组合下该股票的所有成交记录，按 trade_date ASC, id ASC 排序
2. 遍历流水：
   - BUY / RIGHTS:
       current_quantity += qty
       total_cost += price * qty
   - SELL:
       sell_cost = avg_cost * qty
       realized_pnl += (price * qty) - sell_cost - fee - tax
       current_quantity -= qty
       total_cost -= sell_cost
   - BONUS / SPLIT:
       current_quantity += qty（total_cost 不变，拉低成本）
   - DIVIDEND:
       amount 直接记为收益，不进入持仓成本
3. avg_cost = current_quantity > 0 ? total_cost / current_quantity : 0
4. 若 current_quantity == 0，清空 total_cost、avg_cost（保留 realized_pnl）
```

**关键原则**：
- 卖出按**当前平均成本**扣减，不区分批次。
- 手续费和税费不计入持仓成本，仅在卖出时扣减已实现盈亏。
- 清仓后持仓归零，成本归零，但 `realized_pnl` 保留作为历史累计。

### 浮动盈亏计算（查询时实时算）

```
market_value = current_quantity * daily_quote.close_price
floating_pnl = current_quantity * (close_price - avg_cost)
floating_pnl_rate = avg_cost > 0 ? (close_price - avg_cost) / avg_cost * 100 : 0
weight = total_portfolio_market_value > 0 ? market_value / total_portfolio_market_value * 100 : 0
```

若某股票当日无行情数据（如停牌），`close_price` 取最近一个交易日的收盘价，前端展示"已停牌"或"无最新行情"提示。

---

## 后端模块设计（DDD 分层）

按现有 `company/` 模块风格，新建 `portfolio/` 包：

```
backend/src/main/java/com/example/securityanalyze/portfolio/
├── api/
│   ├── PortfolioController.java
│   ├── TransactionController.java
│   ├── PositionController.java
│   ├── PortfolioRequest.java
│   ├── PortfolioResponse.java
│   ├── TransactionRequest.java
│   ├── TransactionResponse.java
│   └── PositionResponse.java
├── application/
│   ├── PortfolioService.java
│   ├── TransactionService.java
│   ├── PositionCalculationService.java    -- 持仓重算核心逻辑
│   └── PortfolioQueryService.java         -- 持仓概览查询（联表行情）
├── domain/
│   ├── Portfolio.java
│   ├── TransactionRecord.java
│   ├── Position.java
│   ├── DailyQuote.java
│   ├── TradeType.java                     -- Enum
│   ├── PortfolioType.java                 -- Enum
│   ├── PortfolioRepository.java
│   ├── TransactionRepository.java
│   ├── PositionRepository.java
│   └── DailyQuoteRepository.java
└── infrastructure/
    ├── PortfolioRepositoryImpl.java
    ├── TransactionRepositoryImpl.java
    ├── PositionRepositoryImpl.java
    └── DailyQuoteRepositoryImpl.java
```

**依赖方向**：`api → application → domain ← infrastructure`

---

## 接口契约

详见独立文档：[api-portfolio.md](./api-portfolio.md)

---

## 前端页面规划

```
src/views/portfolio/
├── PortfolioListView.vue          # 组合列表页（增删改查）
├── PortfolioDetailView.vue        # 组合详情页（Tab 页签结构）
│   ├── PositionTab.vue            #   Tab 1: 当前持仓（表格 + 汇总卡片）
│   ├── TransactionTab.vue         #   Tab 2: 成交记录（时间轴/表格 + 筛选）
│   └── AnalysisTab.vue            #   Tab 3: 收益分析（已实现/未实现、行业分布饼图）
└── TransactionFormDialog.vue      # 录入/编辑交易弹窗

src/api/portfolio.ts               # API 封装
src/types/portfolio.ts             # TypeScript 类型定义
```

**PortfolioDetailView 布局**：
- **顶部**：组合名称 + 汇总卡片（组合总市值、总成本、浮动盈亏、已实现盈亏、累计盈亏、收益率）
- **Tab 页签**：持仓 / 流水 / 分析
- **持仓 Tab**：Element Plus `el-table`，最后一行合计；支持按市场/行业筛选
- **流水 Tab**：按时间倒序，支持按股票代码、交易类型、日期范围筛选

---

## 盘后行情采集任务（Python）

在 `collector/tasks/` 新增 `quote_task.py`：

**功能**：
- 每日 15:30（收盘后）通过 akshare 拉取当日所有 A 股收盘价
- 写入 `daily_quote` 表
- 仅拉取当前所有组合中有持仓的股票代码（定向采集）

**命令行入口**：
```bash
python main.py --run-quotes                        # 手动执行一次（默认当天）
python main.py --run-quotes --date 2026-05-05      # 补录历史某天的行情
```

**采集逻辑（伪代码）**：
```python
def sync_daily_quote(trade_date: date):
    held_stocks = db.query(
        "SELECT DISTINCT stock_code FROM position WHERE current_quantity > 0"
    )
    for code in held_stocks:
        df = akshare.stock_zh_a_hist(
            symbol=code,
            start_date=trade_date,
            end_date=trade_date,
            adjust=""
        )
        upsert_into_daily_quote(df)
```

**调度策略**：
- 通过 `collector/scheduler.py` 注册每日 15:30 定时任务
- 若当天为节假日（无交易数据），akshare 返回空表，跳过写入

---

## 实现优先级

| 阶段 | 内容 | 说明 |
|------|------|------|
| **P0 - MVP** | 组合 CRUD + 成交录入（BUY/SELL）+ 持仓重算 + 持仓列表页 | 核心闭环，优先保证成交录入和持仓计算正确 |
| **P1** | 分红/送股/配股交易类型 + 成交记录页 + 已实现盈亏统计 | 完善非交易变动场景 |
| **P2** | 盘后行情同步 + 浮动盈亏/市值计算 + 汇总卡片 | 需要日行情数据支撑 |
| **P3** | 批量导入 CSV + 收益分析图表（ECharts 饼图/折线图） | 提升操作效率与可视化 |
| **P4** | 多成本算法（先进先出）+ 持仓对比基准指数 | 后续迭代优化 |

---

## 修订记录

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2026-05-06 | v1.0 | 初始版本，定义数据模型、业务规则、分层架构与实现计划 | — |
