# 数据模型设计：股票与公司基础信息 v2

> 本文档记录证券分析系统中"股票与公司基础信息"模块的数据库表结构设计。  
> 版本：v2.0 | 日期：2026-05-11  
> 变更：废除 `tb_relation_stock_company` 关联表与 `tb_collection_task_schedule` 定时规则表，采用极简模型。

---

## 一、表清单

| 表名 | 中文名 | 说明 |
|------|--------|------|
| `tb_stock_basic` | 股票基础信息表 | 存储 A 股静态属性，直接外键关联公司 |
| `tb_company_basic` | 公司基础信息表 | 存储上市公司工商信息与主营业务 |
| `tb_collection_task` | 采集任务执行表 | 记录每次数据采集任务的执行状态与结果 |

---

## 二、表结构定义

### 2.1 tb_stock_basic（股票基础信息表）

存储 A 股等市场股票的静态基础属性。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| `id` | VARCHAR(32) | PRIMARY KEY | 主键ID |
| `stock_code` | VARCHAR(20) | NOT NULL, UNIQUE | 股票编号，如 `000001`、`600000` |
| `ts_code` | VARCHAR(20) | — | Tushare 股票编号，如 `000001.SZ` |
| `name` | VARCHAR(100) | NOT NULL | 股票简称，如平安银行 |
| `full_name` | VARCHAR(200) | — | 股票全称，如平安银行股份有限公司 |
| `market` | VARCHAR(20) | — | 市场类型：主板/创业板/科创板/北交所/CDR |
| `exchange` | VARCHAR(10) | — | 交易所代码：SH 上交所 / SZ 深交所 / BJ 北交所 |
| `list_date` | DATE | — | 上市日期 |
| `industry` | VARCHAR(50) | — | 所属行业，如银行、全国地产 |
| `area` | VARCHAR(50) | — | 所属地域，如深圳、上海 |
| `total_shares` | BIGINT | — | 总股本（股） |
| `float_shares` | BIGINT | — | 流通股本（股） |
| `company_id` | VARCHAR(32) | — | **关联公司 ID**，对应 `tb_company_basic.id` |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_stock_basic_stock_code` ON `stock_code`
- `idx_tb_stock_basic_ts_code` ON `ts_code`
- `idx_tb_stock_basic_industry` ON `industry`
- `idx_tb_stock_basic_market` ON `market`
- `idx_tb_stock_basic_exchange` ON `exchange`
- `idx_tb_stock_basic_company_id` ON `company_id`

---

### 2.2 tb_company_basic（公司基础信息表）

存储上市公司工商注册信息、管理层信息及主营业务描述。

| 字段名 | 类型 | 约束 | 注释 |
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
| `reg_capital` | DECIMAL(18, 4) | — | 注册资本，单位：万元 |
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
| `controller_type` | VARCHAR(50) | — | 实控人企业性质：国企/民营/外资/其他 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_company_basic_usc_code` ON `unified_social_credit_code`
- `idx_tb_company_basic_name` ON `name`
- `idx_tb_company_basic_industry` ON `industry`

---

### 2.3 tb_collection_task（采集任务执行表）

记录每次数据采集任务的创建、执行、完成全过程。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| `id` | VARCHAR(32) | PRIMARY KEY | 主键ID |
| `task_type` | VARCHAR(50) | NOT NULL | 任务类型：`stock_full` / `company_full` / `stock_single` / `company_single` |
| `task_params` | JSONB | — | 任务参数，如 `{"stock_code":"000001"}` |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'pending' | 任务状态：`pending` 待执行 / `running` 执行中 / `success` 成功 / `failed` 失败 |
| `data_source` | VARCHAR(20) | — | 数据源：`akshare` / `tushare` |
| `total_count` | INT | DEFAULT 0 | 预期处理记录数 |
| `success_count` | INT | DEFAULT 0 | 成功处理记录数 |
| `fail_count` | INT | DEFAULT 0 | 失败记录数 |
| `error_message` | TEXT | — | 错误信息 |
| `started_at` | TIMESTAMP | — | 实际开始时间 |
| `completed_at` | TIMESTAMP | — | 实际完成时间 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_collection_task_status` ON `status`
- `idx_tb_collection_task_type` ON `task_type`
- `idx_tb_collection_task_created_at` ON `created_at`

> **定时规则说明**：本版本不再创建 `tb_collection_task_schedule` 表。采集器内部的 APScheduler 通过配置文件或环境变量管理 Cron 规则，后端与前端均不感知定时规则详情。

---

## 三、Flyway 迁移脚本

```sql
-- ============================================
-- V2__create_stock_company_collection_tables.sql
-- 股票基础信息、公司基础信息、采集任务
-- 版本：v2.0（简化模型）
-- ============================================

-- 1. 公司基础信息表（先创建，被股票表外键引用）
CREATE TABLE IF NOT EXISTS tb_company_basic (
    id                          VARCHAR(32) PRIMARY KEY,
    unified_social_credit_code  VARCHAR(50)  UNIQUE,
    name                        VARCHAR(200) NOT NULL,
    short_name                  VARCHAR(100),
    english_name                VARCHAR(200),
    former_name                 VARCHAR(200),
    legal_representative        VARCHAR(50),
    chairman                    VARCHAR(50),
    manager                     VARCHAR(50),
    secretary                   VARCHAR(50),
    reg_capital                 DECIMAL(18, 4),
    setup_date                  DATE,
    province                    VARCHAR(50),
    city                        VARCHAR(50),
    reg_address                 VARCHAR(500),
    office_address              VARCHAR(500),
    website                     VARCHAR(200),
    industry                    VARCHAR(50),
    main_business               TEXT,
    business_scope              TEXT,
    introduction                TEXT,
    employees                   INT,
    controller_name             VARCHAR(100),
    controller_type             VARCHAR(50),
    updated_at                  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at                  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_company_basic_usc_code ON tb_company_basic(unified_social_credit_code);
CREATE INDEX idx_tb_company_basic_name     ON tb_company_basic(name);
CREATE INDEX idx_tb_company_basic_industry ON tb_company_basic(industry);

COMMENT ON TABLE tb_company_basic IS '公司基础信息表：存储上市公司工商信息与主营业务';

-- 2. 股票基础信息表
CREATE TABLE IF NOT EXISTS tb_stock_basic (
    id              VARCHAR(32) PRIMARY KEY,
    stock_code      VARCHAR(20)  NOT NULL UNIQUE,
    ts_code         VARCHAR(20),
    name            VARCHAR(100) NOT NULL,
    full_name       VARCHAR(200),
    market          VARCHAR(20),
    exchange        VARCHAR(10),
    list_date       DATE,
    industry        VARCHAR(50),
    area            VARCHAR(50),
    total_shares    BIGINT,
    float_shares    BIGINT,
    company_id      VARCHAR(32),
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_stock_basic_stock_code  ON tb_stock_basic(stock_code);
CREATE INDEX idx_tb_stock_basic_ts_code     ON tb_stock_basic(ts_code);
CREATE INDEX idx_tb_stock_basic_industry    ON tb_stock_basic(industry);
CREATE INDEX idx_tb_stock_basic_market      ON tb_stock_basic(market);
CREATE INDEX idx_tb_stock_basic_exchange    ON tb_stock_basic(exchange);
CREATE INDEX idx_tb_stock_basic_company_id  ON tb_stock_basic(company_id);

COMMENT ON TABLE tb_stock_basic IS '股票基础信息表：存储A股静态属性，直接外键关联公司';

-- 3. 采集任务执行表
CREATE TABLE IF NOT EXISTS tb_collection_task (
    id              VARCHAR(32) PRIMARY KEY,
    task_type       VARCHAR(50)  NOT NULL,
    task_params     JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    data_source     VARCHAR(20),
    total_count     INT          DEFAULT 0,
    success_count   INT          DEFAULT 0,
    fail_count      INT          DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_collection_task_status      ON tb_collection_task(status);
CREATE INDEX idx_tb_collection_task_type        ON tb_collection_task(task_type);
CREATE INDEX idx_tb_collection_task_created_at  ON tb_collection_task(created_at);

COMMENT ON TABLE tb_collection_task IS '采集任务执行表：记录每次数据采集任务的执行状态与结果';
```

---

## 四、数据源字段映射

### 4.1 股票基础信息（tb_stock_basic）

| 本表字段 | akshare 来源 | tushare 来源 | 说明 |
|----------|-------------|-------------|------|
| `stock_code` | `stock_info_a_code_name.code` | `stock_basic.symbol` | AKShare 为主 |
| `ts_code` | — | `stock_basic.ts_code` | 仅 Tushare 提供 |
| `name` | `stock_info_a_code_name.name` | `stock_basic.name` | AKShare 为主 |
| `full_name` | `stock_info_sh_name_code.公司全称` | — | AKShare 提供 |
| `market` | `stock_info_sz_name_code.板块` | `stock_basic.market` | 互补 |
| `exchange` | `stock_profile_cninfo.所属市场` | — | AKShare 提供 |
| `list_date` | `stock_info_sh/sz_name_code.上市日期` | `stock_basic.list_date` | AKShare 为主 |
| `industry` | `stock_info_sz_name_code.所属行业` | `stock_basic.industry` | AKShare 为主 |
| `area` | — | `stock_basic.area` | **仅 Tushare 提供** |
| `total_shares` | `stock_info_sz_name_code.A股总股本` | — | AKShare 提供 |
| `float_shares` | `stock_info_sz_name_code.A股流通股本` | — | AKShare 提供 |
| `company_id` | — | — | 由采集脚本关联写入 |

### 4.2 公司基础信息（tb_company_basic）

| 本表字段 | akshare 来源 | tushare 来源 | 说明 |
|----------|-------------|-------------|------|
| `unified_social_credit_code` | — | `stock_company.com_id` | **仅 Tushare 提供** |
| `name` | `stock_profile_cninfo.公司名称` | `stock_company.com_name` | AKShare 为主 |
| `short_name` | `stock_profile_cninfo.公司简称` | — | AKShare 提供 |
| `english_name` | `stock_profile_cninfo.英文名称` | — | AKShare 提供 |
| `former_name` | `stock_profile_cninfo.曾用简称` | — | AKShare 提供 |
| `legal_representative` | `stock_profile_cninfo.法人代表` | — | AKShare 提供 |
| `chairman` | — | `stock_company.chairman` | **仅 Tushare 提供** |
| `manager` | — | `stock_company.manager` | **仅 Tushare 提供** |
| `secretary` | — | `stock_company.secretary` | **仅 Tushare 提供** |
| `reg_capital` | `stock_profile_cninfo.注册资金` | `stock_company.reg_capital` | AKShare 为主 |
| `setup_date` | `stock_profile_cninfo.成立日期` | `stock_company.setup_date` | AKShare 为主 |
| `province` | — | `stock_company.province` | **仅 Tushare 提供** |
| `city` | — | `stock_company.city` | **仅 Tushare 提供** |
| `reg_address` | `stock_profile_cninfo.注册地址` | — | AKShare 提供 |
| `office_address` | `stock_profile_cninfo.办公地址` | `stock_company.office` | AKShare 为主 |
| `website` | `stock_profile_cninfo.官方网站` | `stock_company.website` | AKShare 为主 |
| `industry` | `stock_profile_cninfo.所属行业` | — | AKShare 提供 |
| `main_business` | `stock_profile_cninfo.主营业务` | `stock_company.main_business` | AKShare 为主 |
| `business_scope` | `stock_profile_cninfo.经营范围` | `stock_company.business_scope` | AKShare 为主 |
| `introduction` | `stock_profile_cninfo.机构简介` | `stock_company.introduction` | AKShare 为主 |
| `employees` | — | `stock_company.employees` | **仅 Tushare 提供** |
| `controller_name` | — | `stock_basic.act_name` | **仅 Tushare 提供** |
| `controller_type` | — | `stock_basic.act_ent_type` | **仅 Tushare 提供** |

---

## 五、版本记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v2.0 | 2026-05-11 | 简化模型：废除关联表与定时规则表，`tb_stock_basic` 增加 `company_id` 外键 | AI助手 |
| v1.0 | 2026-05-10 | 初始创建：五张表含关联表与定时规则表（已废弃） | AI助手 |
