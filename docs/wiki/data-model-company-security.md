# 公司信息数据模型 — 多证券市场支持设计

## 1. 审查背景

### 1.1 审查目的

评估当前公司信息模块的数据模型是否能够正确表达「**一个公司法人实体在多个证券市场拥有多个上市证券**」这一现实场景，并提出可落地的新数据模型设计。

### 1.2 现实场景举例

在中国资本市场中，同一公司主体在多市场上市的情况并不罕见：

| 公司全称 | A股代码 | B股代码 | H股代码 | 现状说明 |
|----------|---------|---------|---------|----------|
| 山东晨鸣纸业集团股份有限公司 | 000488 | 200488 | — | A股深圳主板 + B股深圳B股 |
| 万科企业股份有限公司 | 000002 | — | 02202 | A股深圳主板 + H股港交所 |
| 中国平安保险（集团）股份有限公司 | 601318 | — | 02318 | A股上海主板 + H股港交所 |
| 中国石化上海石油化工股份有限公司 | 600688 | 900688 | 00338 | A股 + B股 + H股 三地上市 |

这些公司的核心工商信息（行业、地区、成立日期、注册资本）是统一的，不因证券市场而异；但股票代码、证券简称、上市日期、市场板块等属性则随证券不同而不同。

---

## 2. 现有模型分析

### 2.1 当前表结构

当前采用单表设计，`company` 表同时承载了「公司属性」与「证券属性」：

```sql
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL UNIQUE,   -- 证券级属性
    stock_name VARCHAR(100) NOT NULL,         -- 证券级属性（不同市场简称可能不同）
    industry VARCHAR(100),                    -- 公司级属性
    region VARCHAR(50),                       -- 公司级属性
    establish_date DATE,                      -- 公司级属性
    registered_capital DECIMAL(20,4),         -- 公司级属性
    listing_date DATE,                        -- 证券级属性
    market VARCHAR(10),                       -- 证券级属性
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 2.2 评估结论：不满足多证券市场场景

| 维度 | 现有设计 | 问题描述 |
|------|----------|----------|
| **唯一性约束** | `stock_code` 为 `UNIQUE` | 强制一条记录对应一个证券代码，本身合理；但没有更高层级的「公司」实体将多个证券关联起来 |
| **公司级标识** | 无独立公司标识 | 无法判断 `000488` 与 `200488` 是否属于同一公司 |
| **属性归属混乱** | 公司属性与证券属性混存一表 | industry、region、establish_date、registered_capital 在公司级应唯一，却在不同证券记录间重复存储 |
| **数据冗余** | 同一公司的不同证券各自保存一套公司信息 | 违反第一范式；若公司行业变更，需更新多条记录，容易不一致 |
| **前端表达力** | 「关联证券」Tab 无真实数据可展示 | 当前仅展示当前记录本身，因为数据库中不存在「同一公司下的其他证券」这一关系 |
| **数据源利用率** | 采集逻辑未利用 akshare 多代码字段 | `stock_profile_cninfo` 已返回 `A股代码`、`B股代码`、`H股代码` 等字段，但解析时仅提取单个传入代码 |

### 2.3 具体反例：晨鸣纸业在当前模型中的存储形态

假设已采集晨鸣纸业的 A 股与 B 股信息，数据库中会存在两条**完全独立、互不知晓**的记录：

| id | stock_code | stock_name | industry | region | establish_date | registered_capital | listing_date | market |
|----|------------|------------|----------|--------|----------------|--------------------|--------------|--------|
| 1 | 000488 | 晨鸣纸业 | 造纸 | 山东 | 1993-05-05 | 297198.0 | 1997-05-26 | SZ |
| 2 | 200488 | 晨鸣B | 造纸 | 山东 | 1993-05-05 | 297198.0 | 1997-05-26 | SZ |

> 这两条记录仅在人工观察时才可能被识别为同一家公司；系统层面没有任何字段可以建立它们的关联。

---

## 3. 新数据模型设计

### 3.1 设计原则

1. **公司-证券分离**：区分「公司法人实体」（company）与「上市证券」（security），分别用独立表存储。
2. **一对多关系**：一个公司可对应多个证券；一个证券仅属于一个公司。
3. **属性归位**：不因证券市场而变化的属性归入 `company` 表；随证券市场变化的属性归入 `company_security` 表。
4. **与模块职责对齐**：与 `module-design.md` 中已规划的「公司信息模块」「证券信息模块」职责边界一致。

### 3.2 实体关系图（ERD）

```
┌─────────────────────────────────┐         ┌─────────────────────────────────┐
│           company               │         │      company_security           │
├─────────────────────────────────┤         ├─────────────────────────────────┤
│ id (PK)                         │         │ id (PK)                         │
│ unified_code (UK)               │◄────────┤ company_id (FK)                 │
│ company_name                    │   1:N   │ stock_code (UK)                 │
│ short_name                      │         │ stock_name                      │
│ industry                        │         │ market                          │
│ region                          │         │ security_type                   │
│ establish_date                  │         │ listing_date                    │
│ registered_capital              │         │ listing_status                  │
│ created_at                      │         │ created_at                      │
│ updated_at                      │         │ updated_at                      │
└─────────────────────────────────┘         └─────────────────────────────────┘
```

### 3.3 表 1：company（公司法人实体）

存储不随证券市场变化的公司核心属性。

```sql
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    unified_code VARCHAR(50) UNIQUE,             -- 统一社会信用代码，公司级唯一标识
    company_name VARCHAR(200) NOT NULL,          -- 公司全称
    short_name VARCHAR(100),                     -- 公司简称
    industry VARCHAR(100),                       -- 所属行业
    region VARCHAR(50),                          -- 地区（省份/直辖市）
    establish_date DATE,                         -- 成立日期
    registered_capital DECIMAL(20,4),            -- 注册资本（万元）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_unified_code ON company(unified_code);
CREATE INDEX idx_company_name ON company(company_name);
CREATE INDEX idx_company_industry ON company(industry);
CREATE INDEX idx_company_region ON company(region);
```

**字段说明**：

| 字段 | 类型 | 约束 | 业务含义 |
|------|------|------|----------|
| `id` | BIGSERIAL | PK | 自增主键，系统内部使用 |
| `unified_code` | VARCHAR(50) | UNIQUE | 统一社会信用代码。初期若采集源未提供，可暂用自发生成的唯一编码占位 |
| `company_name` | VARCHAR(200) | NOT NULL | 工商注册的公司全称，不因证券市场变化 |
| `short_name` | VARCHAR(100) | | 公司简称（如「晨鸣纸业」），可与证券简称不同 |
| `industry` | VARCHAR(100) | | 所属行业分类 |
| `region` | VARCHAR(50) | | 地区，从注册地址提取的省份/直辖市 |
| `establish_date` | DATE | | 工商登记的成立日期 |
| `registered_capital` | DECIMAL(20,4) | | 注册资本，单位万元 |
| `created_at` / `updated_at` | TIMESTAMP | NOT NULL | 审计字段 |

### 3.4 表 2：company_security（上市证券）

存储随证券市场变化的证券级属性，通过 `company_id` 外键关联到公司。

```sql
CREATE TABLE IF NOT EXISTS company_security (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL UNIQUE,      -- 股票代码，全局唯一
    stock_name VARCHAR(100) NOT NULL,            -- 证券简称（不同市场可能不同）
    market VARCHAR(10),                          -- 市场板块：SH / SZ / BJ / HK
    security_type VARCHAR(20),                   -- 证券类型：A股 / B股 / H股 / 优先股 / ADR
    listing_date DATE,                           -- 在该市场的上市日期
    listing_status VARCHAR(20) DEFAULT 'listed', -- 上市状态：listed / suspended / delisted
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cs_stock_code ON company_security(stock_code);
CREATE INDEX idx_cs_company_id ON company_security(company_id);
CREATE INDEX idx_cs_market ON company_security(market);
CREATE INDEX idx_cs_security_type ON company_security(security_type);
CREATE INDEX idx_cs_listing_status ON company_security(listing_status);
```

**字段说明**：

| 字段 | 类型 | 约束 | 业务含义 |
|------|------|------|----------|
| `id` | BIGSERIAL | PK | 自增主键 |
| `company_id` | BIGINT | NOT NULL FK | 关联 `company.id`，级联删除 |
| `stock_code` | VARCHAR(20) | NOT NULL UNIQUE | 股票代码（如 000488、200488），全局唯一 |
| `stock_name` | VARCHAR(100) | NOT NULL | 证券简称（如「晨鸣纸业」「晨鸣B」），不同市场可不同 |
| `market` | VARCHAR(10) | | 市场板块。当前覆盖 SH（上海）、SZ（深圳）、BJ（北京）；预留 HK（香港）等扩展 |
| `security_type` | VARCHAR(20) | | 证券类型，便于区分 A/B/H 股等 |
| `listing_date` | DATE | | 在该具体市场的上市日期（不同市场可能不同）|
| `listing_status` | VARCHAR(20) | DEFAULT 'listed' | 上市状态，支持已上市/暂停上市/退市等状态 |
| `created_at` / `updated_at` | TIMESTAMP | NOT NULL | 审计字段 |

### 3.5 新模型下的晨鸣纸业存储形态

**company 表**：

| id | unified_code | company_name | short_name | industry | region | establish_date | registered_capital |
|----|--------------|--------------|------------|----------|--------|----------------|--------------------|
| 1 | 91370000163588173C | 山东晨鸣纸业集团股份有限公司 | 晨鸣纸业 | 造纸 | 山东 | 1993-05-05 | 297198.0 |

**company_security 表**：

| id | company_id | stock_code | stock_name | market | security_type | listing_date | listing_status |
|----|------------|------------|------------|--------|---------------|--------------|----------------|
| 1 | 1 | 000488 | 晨鸣纸业 | SZ | A股 | 1997-05-26 | listed |
| 2 | 1 | 200488 | 晨鸣B | SZ | B股 | 1997-05-26 | listed |

> 系统层面可通过 `company_id = 1` 明确知道这两支证券属于同一公司；公司级信息只存储一份，避免冗余。

---

## 4. 与现有模块职责的对齐

与 `module-design.md` 中已规划的模块边界完全对齐：

| 模块 | 对应数据表 | 职责 |
|------|------------|------|
| **公司信息模块** | `company` | 管理公司法人实体的静态信息（行业、地区、成立日期、注册资本等） |
| **证券信息模块** | `company_security` | 管理上市证券的代码、名称、市场板块、上市日期、上市状态等 |
| 两模块关联 | `company.id` ↔ `company_security.company_id` | 展示「某公司的全部证券」或「某证券所属公司」 |

---

## 5. 对现有系统的影响分析

### 5.1 数据库层

| 影响点 | 说明 |
|--------|------|
| 新增表 | 创建 `company`、`company_security` 两张新表 |
| 旧表处理 | 旧 `company` 表在新系统稳定运行后可标记为废弃，最终删除 |
| 数据迁移 | 需编写一次性脚本将旧 `company` 数据拆分：按 `stock_code` 去重生成 `company` 记录，原记录迁移为 `company_security` |

### 5.2 后端层

| 影响点 | 当前实现 | 适配方向 |
|--------|----------|----------|
| Domain 实体 | 单一 `Company` 实体 | 拆分为 `Company` + `CompanySecurity` 两个实体 |
| Repository | `CompanyRepository` 接口及 JDBC 实现 | 新增 `CompanySecurityRepository`，`CompanyRepository` 增加 `findById` 等方法 |
| Service | `CompanyService` 直接映射单实体 | 详情查询需关联加载 `List<CompanySecurity>` |
| API 接口 | `/api/companies` 列表 + `/api/companies/{stockCode}` 详情 | **列表接口**：保持按证券展示（兼容现有 UI 习惯，每行一条证券）；**详情接口**：返回公司信息 + `securities[]` 数组 |
| 新增接口 | 无 | 建议新增 `/api/companies/{stockCode}/securities` 子资源接口，供「关联证券」Tab 独立调用 |

### 5.3 前端层

| 影响点 | 当前实现 | 适配方向 |
|--------|----------|----------|
| 列表页 `CompanyListView` | 展示 stockCode、stockName、industry 等 | 无需改动；继续按证券维度展示，每行对应一条 `company_security` 记录 |
| 详情页「基本信息」Tab | 展示公司基本信息 | 字段与当前基本一致，数据来源改为 `company` 表 |
| 详情页「关联证券」Tab | 仅展示当前记录本身 | **核心收益点**：展示该公司下的所有证券卡片列表，调用详情接口中的 `securities` 数组或独立子资源接口 |
| TypeScript 类型 | `CompanyDetail` 接口 | 增加 `securities?: Security[]` 字段 |

### 5.4 数据采集层

| 影响点 | 当前实现 | 适配方向 |
|--------|----------|----------|
| 解析逻辑 | `_parse_company` 返回单条字典 | 拆分为 `_parse_company_entity`（返回公司级信息）+ `_parse_securities`（返回该股票代码及关联的 B 股/H 股信息列表） |
| upsert 逻辑 | 单表 upsert | 先 upsert `company`（按 unified_code 或名称匹配），获取 `company_id`，再 upsert `company_security`（按 stock_code 匹配） |
| 数据源利用 | 仅提取传入的单个 stock_code | 利用 akshare `stock_profile_cninfo` 返回的 `A股代码`、`B股代码`、`H股代码` 字段，一次性采集某公司的全部证券 |

---

## 6. 分阶段实施建议

### 阶段一：数据库与后端模型（基础设施）

1. 编写 `V2__create_company_and_security_tables.sql` 迁移脚本。
2. 编写一次性数据迁移脚本，将旧 `company` 表数据拆分到新表。
3. 后端新增 `CompanySecurity` Domain 实体、`CompanySecurityRepository` 接口及实现。
4. 修改 `CompanyService` 详情查询，关联加载 securities 列表。
5. 新增 `/api/companies/{stockCode}/securities` 子资源接口。

### 阶段二：前端「关联证券」Tab（用户可见功能）

1. 更新 `CompanyDetail` TypeScript 类型，增加 `securities` 数组。
2. 改造详情页「关联证券」Tab，展示该公司下的全部证券卡片。
3. 列表页保持现状，无需改动。

### 阶段三：采集模块适配（数据质量保证）

1. 重写 `company_task.py` 的解析与 upsert 逻辑。
2. 利用 akshare 多代码字段，实现「采集一个 A 股代码时，同时识别并入库其 B 股/H 股」。
3. 补充 `unified_code` 的采集或生成策略（初期若数据源无统一社会信用代码，可基于公司全称做模糊匹配关联）。

### 阶段四：旧表清理（收尾）

1. 确认新系统运行稳定、数据一致。
2. 删除旧 `company` 表及相关废弃代码。

---

## 7. 修订记录

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2026-04-30 | v1.0 | 初始版本：审查现有模型缺陷，提出 company + company_security 两表分离方案 | — |
