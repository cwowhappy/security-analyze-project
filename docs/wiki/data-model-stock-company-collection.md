# 数据模型设计：股票与公司基础信息

> 本文档记录证券分析系统中"股票与公司基础信息"模块的数据库表结构设计。
> 关联功能：数据采集任务管理、定时采集调度。

---

## 一、表清单

| 表名 | 中文名 | 说明 |
|------|--------|------|
| `tb_stock_basic` | 股票基础信息表 | 存储A股等市场股票静态属性 |
| `tb_company_basic` | 公司基础信息表 | 存储上市公司工商信息与主营业务 |
| `tb_relation_stock_company` | 股票与公司关联关系表 | 一只股票对应一家公司的关联关系 |
| `tb_collection_task` | 采集任务执行表 | 记录每次数据采集任务的执行状态与结果 |
| `tb_collection_task_schedule` | 采集定时规则表 | 定义周期性自动采集任务 |

---

## 二、表结构定义

### 2.1 tb_stock_basic（股票基础信息表）

存储A股等市场股票的静态基础属性，数据来源为 akshare / tushare。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| id | VARCHAR(32) | PRIMARY KEY | 主键ID |
| stock_code | VARCHAR(20) | NOT NULL, UNIQUE | 股票编号，如000001、600000 |
| ts_code | VARCHAR(20) | | Tushare股票编号，如000001.SZ、600000.SH |
| name | VARCHAR(100) | NOT NULL | 股票简称，如平安银行 |
| full_name | VARCHAR(200) | | 股票全称，如平安银行股份有限公司 |
| market | VARCHAR(20) | | 市场类型：主板/创业板/科创板/北交所/CDR |
| exchange | VARCHAR(10) | | 交易所代码：SH上交所/SZ深交所/BJ北交所 |
| list_date | DATE | | 上市日期 |
| industry | VARCHAR(50) | | 所属行业，如银行、全国地产 |
| area | VARCHAR(50) | | 所属地域，如深圳、上海 |
| total_shares | BIGINT | | 总股本（股） |
| float_shares | BIGINT | | 流通股本（股） |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_stock_basic_stock_code` ON `stock_code`
- `idx_tb_stock_basic_ts_code` ON `ts_code`
- `idx_tb_stock_basic_industry` ON `industry`
- `idx_tb_stock_basic_market` ON `market`
- `idx_tb_stock_basic_exchange` ON `exchange`

---

### 2.2 tb_company_basic（公司基础信息表）

存储上市公司工商注册信息、管理层信息及主营业务描述。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| id | VARCHAR(32) | PRIMARY KEY | 主键ID |
| unified_social_credit_code | VARCHAR(50) | UNIQUE | 统一社会信用代码 |
| name | VARCHAR(200) | NOT NULL | 公司全称 |
| short_name | VARCHAR(100) | | 公司简称 |
| english_name | VARCHAR(200) | | 英文名称 |
| former_name | VARCHAR(200) | | 曾用简称 |
| legal_representative | VARCHAR(50) | | 法人代表 |
| chairman | VARCHAR(50) | | 董事长 |
| manager | VARCHAR(50) | | 总经理 |
| secretary | VARCHAR(50) | | 董事会秘书 |
| reg_capital | DECIMAL(18,4) | | 注册资本，单位：万元 |
| setup_date | DATE | | 成立日期 |
| province | VARCHAR(50) | | 所在省份 |
| city | VARCHAR(50) | | 所在城市 |
| reg_address | VARCHAR(500) | | 注册地址 |
| office_address | VARCHAR(500) | | 办公地址 |
| website | VARCHAR(200) | | 官方网站 |
| industry | VARCHAR(50) | | 所属行业 |
| main_business | TEXT | | 主营业务描述 |
| business_scope | TEXT | | 经营范围 |
| introduction | TEXT | | 公司简介 |
| employees | INT | | 员工人数 |
| controller_name | VARCHAR(100) | | 实控人名称 |
| controller_type | VARCHAR(50) | | 实控人企业性质：国企/民营/外资/其他 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_company_basic_usc_code` ON `unified_social_credit_code`
- `idx_tb_company_basic_name` ON `name`
- `idx_tb_company_basic_industry` ON `industry`

---

### 2.3 tb_relation_stock_company（股票与公司关联关系表）

建立股票与公司之间的关联关系。A股中一只股票通常对应一家公司，通过此表实现解耦。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| id | VARCHAR(32) | PRIMARY KEY | 主键ID |
| stock_code | VARCHAR(20) | NOT NULL | 股票编号，关联 `tb_stock_basic.stock_code` |
| company_usc_code | VARCHAR(50) | NOT NULL | 公司统一社会信用代码，关联 `tb_company_basic.unified_social_credit_code` |
| exchange | VARCHAR(10) | | 交易所代码：SH/SZ/BJ |
| market_type | VARCHAR(20) | | 市场类型：主板/创业板/科创板/北交所 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**约束：**
- UNIQUE (`stock_code`, `company_usc_code`)

**索引：**
- `idx_tb_relation_stock_company_code` ON `stock_code`
- `idx_tb_relation_stock_company_usc` ON `company_usc_code`

---

### 2.4 tb_collection_task（采集任务执行表）

记录每次数据采集任务的创建、执行、完成全过程，支持即时执行与定时执行。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| id | VARCHAR(32) | PRIMARY KEY | 主键ID |
| task_type | VARCHAR(50) | NOT NULL | 任务类型：`stock_full`-股票全量 / `company_full`-公司全量 / `stock_single`-单股票 / `company_single`-单公司 |
| task_params | JSONB | | 任务参数，如 `{"stock_code":"000001"}` |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'pending' | 任务状态：`pending`待执行 / `running`执行中 / `success`成功 / `failed`失败 |
| data_source | VARCHAR(20) | | 数据源：`akshare` / `tushare` |
| total_count | INT | DEFAULT 0 | 预期处理记录数 |
| success_count | INT | DEFAULT 0 | 成功处理记录数 |
| fail_count | INT | DEFAULT 0 | 失败记录数 |
| scheduled_at | TIMESTAMP | | 定时执行时间，NULL表示立即执行 |
| error_message | TEXT | | 错误信息 |
| started_at | TIMESTAMP | | 实际开始时间 |
| completed_at | TIMESTAMP | | 实际完成时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_collection_task_status` ON `status`
- `idx_tb_collection_task_scheduled_at` ON `scheduled_at`（WHERE `scheduled_at` IS NOT NULL）
- `idx_tb_collection_task_type` ON `task_type`

---

### 2.5 tb_collection_task_schedule（采集定时规则表）

定义周期性自动采集规则，由采集器定期扫描并生成 `tb_collection_task` 记录。

| 字段名 | 类型 | 约束 | 注释 |
|--------|------|------|------|
| id | VARCHAR(32) | PRIMARY KEY | 主键ID |
| name | VARCHAR(100) | NOT NULL | 规则名称，如"每日股票全量采集" |
| task_type | VARCHAR(50) | NOT NULL | 任务类型，同 `tb_collection_task.task_type` |
| task_params | JSONB | | 默认任务参数 |
| data_source | VARCHAR(20) | | 默认数据源：`akshare` / `tushare` |
| cron_expression | VARCHAR(50) | NOT NULL | Cron表达式，如 `0 2 * * *` 表示每天2点 |
| is_enabled | BOOLEAN | DEFAULT TRUE | 是否启用 |
| last_triggered_at | TIMESTAMP | | 上次触发时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- `idx_tb_collection_task_schedule_enabled` ON `is_enabled`（WHERE `is_enabled` = TRUE）

---

## 三、数据源字段映射

### 3.1 股票基础信息（tb_stock_basic）

| 本表字段 | akshare来源 | tushare来源 | 说明 |
|----------|-------------|-------------|------|
| stock_code | `stock_info_a_code_name.code` | `stock_basic.symbol` | 股票编号 |
| ts_code | — | `stock_basic.ts_code` | Tushare专用代码 |
| name | `stock_info_a_code_name.name` | `stock_basic.name` | 股票简称 |
| full_name | `stock_info_sh_name_code.公司全称` | — | 股票全称 |
| market | `stock_info_sz_name_code.板块` | `stock_basic.market` | 市场类型 |
| exchange | `stock_profile_cninfo.所属市场` | — | 交易所 |
| list_date | `stock_info_sh/sz_name_code.上市日期` | `stock_basic.list_date` | 上市日期 |
| industry | `stock_info_sz_name_code.所属行业` | `stock_basic.industry` | 所属行业 |
| area | — | `stock_basic.area` | 所属地域 |
| total_shares | `stock_info_sz_name_code.A股总股本` | — | 总股本 |
| float_shares | `stock_info_sz_name_code.A股流通股本` | — | 流通股本 |

### 3.2 公司基础信息（tb_company_basic）

| 本表字段 | akshare来源 | tushare来源 | 说明 |
|----------|-------------|-------------|------|
| unified_social_credit_code | — | `stock_company.com_id` | 统一社会信用代码 |
| name | `stock_profile_cninfo.公司名称` | `stock_company.com_name` | 公司全称 |
| short_name | `stock_profile_cninfo.公司简称` | — | 公司简称 |
| english_name | `stock_profile_cninfo.英文名称` | — | 英文名称 |
| former_name | `stock_profile_cninfo.曾用简称` | — | 曾用简称 |
| legal_representative | `stock_profile_cninfo.法人代表` | — | 法人代表 |
| chairman | — | `stock_company.chairman` | 董事长 |
| manager | — | `stock_company.manager` | 总经理 |
| secretary | — | `stock_company.secretary` | 董事会秘书 |
| reg_capital | `stock_profile_cninfo.注册资金` | `stock_company.reg_capital` | 注册资本（万元） |
| setup_date | `stock_profile_cninfo.成立日期` | `stock_company.setup_date` | 成立日期 |
| province | — | `stock_company.province` | 所在省份 |
| city | — | `stock_company.city` | 所在城市 |
| reg_address | `stock_profile_cninfo.注册地址` | — | 注册地址 |
| office_address | `stock_profile_cninfo.办公地址` | `stock_company.office` | 办公地址 |
| website | `stock_profile_cninfo.官方网站` | `stock_company.website` | 官方网站 |
| industry | `stock_profile_cninfo.所属行业` | — | 所属行业 |
| main_business | `stock_profile_cninfo.主营业务` | `stock_company.main_business` | 主营业务 |
| business_scope | `stock_profile_cninfo.经营范围` | `stock_company.business_scope` | 经营范围 |
| introduction | `stock_profile_cninfo.机构简介` | `stock_company.introduction` | 公司简介 |
| employees | — | `stock_company.employees` | 员工人数 |
| controller_name | — | `stock_basic.act_name` | 实控人名称 |
| controller_type | — | `stock_basic.act_ent_type` | 实控人企业性质 |

---

## 四、Flyway迁移脚本

```sql
-- ============================================
-- V2__create_stock_company_collection_tables.sql
-- 股票基础信息、公司基础信息、关联关系、采集任务
-- ============================================

-- 1. 清理旧表（如有数据，请提前备份）
DROP TABLE IF EXISTS stock CASCADE;

-- 2. 股票基础信息表
COMMENT ON TABLE tb_stock_basic IS '股票基础信息表：存储A股等市场股票静态属性';

CREATE TABLE IF NOT EXISTS tb_stock_basic (
    id              VARCHAR(32) PRIMARY KEY COMMENT '主键ID',
    stock_code      VARCHAR(20)  NOT NULL UNIQUE COMMENT '股票编号，如000001、600000',
    ts_code         VARCHAR(20)  COMMENT 'Tushare股票编号，如000001.SZ、600000.SH',
    name            VARCHAR(100) NOT NULL COMMENT '股票简称，如平安银行',
    full_name       VARCHAR(200) COMMENT '股票全称，如平安银行股份有限公司',
    market          VARCHAR(20)  COMMENT '市场类型：主板/创业板/科创板/北交所/CDR',
    exchange        VARCHAR(10)  COMMENT '交易所代码：SH上交所/SZ深交所/BJ北交所',
    list_date       DATE         COMMENT '上市日期',
    industry        VARCHAR(50)  COMMENT '所属行业，如银行、全国地产',
    area            VARCHAR(50)  COMMENT '所属地域，如深圳、上海',
    total_shares    BIGINT       COMMENT '总股本（股）',
    float_shares    BIGINT       COMMENT '流通股本（股）',
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

CREATE INDEX idx_tb_stock_basic_stock_code ON tb_stock_basic(stock_code);
CREATE INDEX idx_tb_stock_basic_ts_code     ON tb_stock_basic(ts_code);
CREATE INDEX idx_tb_stock_basic_industry    ON tb_stock_basic(industry);
CREATE INDEX idx_tb_stock_basic_market      ON tb_stock_basic(market);
CREATE INDEX idx_tb_stock_basic_exchange    ON tb_stock_basic(exchange);

-- 3. 公司基础信息表
COMMENT ON TABLE tb_company_basic IS '公司基础信息表：存储上市公司工商信息与主营业务';

CREATE TABLE IF NOT EXISTS tb_company_basic (
    id                          VARCHAR(32) PRIMARY KEY COMMENT '主键ID',
    unified_social_credit_code  VARCHAR(50)  UNIQUE COMMENT '统一社会信用代码',
    name                        VARCHAR(200) NOT NULL COMMENT '公司全称',
    short_name                  VARCHAR(100) COMMENT '公司简称',
    english_name                VARCHAR(200) COMMENT '英文名称',
    former_name                 VARCHAR(200) COMMENT '曾用简称',
    legal_representative        VARCHAR(50)  COMMENT '法人代表',
    chairman                    VARCHAR(50)  COMMENT '董事长',
    manager                     VARCHAR(50)  COMMENT '总经理',
    secretary                   VARCHAR(50)  COMMENT '董事会秘书',
    reg_capital                 DECIMAL(18, 4) COMMENT '注册资本，单位：万元',
    setup_date                  DATE         COMMENT '成立日期',
    province                    VARCHAR(50)  COMMENT '所在省份',
    city                        VARCHAR(50)  COMMENT '所在城市',
    reg_address                 VARCHAR(500) COMMENT '注册地址',
    office_address              VARCHAR(500) COMMENT '办公地址',
    website                     VARCHAR(200) COMMENT '官方网站',
    industry                    VARCHAR(50)  COMMENT '所属行业',
    main_business               TEXT         COMMENT '主营业务描述',
    business_scope              TEXT         COMMENT '经营范围',
    introduction                TEXT         COMMENT '公司简介',
    employees                   INT          COMMENT '员工人数',
    controller_name             VARCHAR(100) COMMENT '实控人名称',
    controller_type             VARCHAR(50)  COMMENT '实控人企业性质：国企/民营/外资/其他',
    updated_at                  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at                  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

CREATE INDEX idx_tb_company_basic_usc_code ON tb_company_basic(unified_social_credit_code);
CREATE INDEX idx_tb_company_basic_name     ON tb_company_basic(name);
CREATE INDEX idx_tb_company_basic_industry ON tb_company_basic(industry);

-- 4. 股票与公司关联关系表
COMMENT ON TABLE tb_relation_stock_company IS '股票与公司关联关系表：一只股票对应一家公司';

CREATE TABLE IF NOT EXISTS tb_relation_stock_company (
    id               VARCHAR(32) PRIMARY KEY COMMENT '主键ID',
    stock_code       VARCHAR(20)  NOT NULL COMMENT '股票编号，关联tb_stock_basic.stock_code',
    company_usc_code VARCHAR(50)  NOT NULL COMMENT '公司统一社会信用代码，关联tb_company_basic.unified_social_credit_code',
    exchange         VARCHAR(10)  COMMENT '交易所代码：SH/SZ/BJ',
    market_type      VARCHAR(20)  COMMENT '市场类型：主板/创业板/科创板/北交所',
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT uk_tb_relation_stock_company UNIQUE (stock_code, company_usc_code)
);

CREATE INDEX idx_tb_relation_stock_company_code ON tb_relation_stock_company(stock_code);
CREATE INDEX idx_tb_relation_stock_company_usc  ON tb_relation_stock_company(company_usc_code);

-- 5. 采集任务执行表
COMMENT ON TABLE tb_collection_task IS '采集任务执行表：记录每次数据采集任务的执行状态与结果';

CREATE TABLE IF NOT EXISTS tb_collection_task (
    id              VARCHAR(32) PRIMARY KEY COMMENT '主键ID',
    task_type       VARCHAR(50)  NOT NULL COMMENT '任务类型：stock_full-股票全量/company_full-公司全量/stock_single-单股票/company_single-单公司',
    task_params     JSONB        COMMENT '任务参数，如{"stock_code":"000001"}',
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending待执行/running执行中/success成功/failed失败',
    data_source     VARCHAR(20)  COMMENT '数据源：akshare/tushare',
    total_count     INT          DEFAULT 0 COMMENT '预期处理记录数',
    success_count   INT          DEFAULT 0 COMMENT '成功处理记录数',
    fail_count      INT          DEFAULT 0 COMMENT '失败记录数',
    scheduled_at    TIMESTAMP    COMMENT '定时执行时间，NULL表示立即执行',
    error_message   TEXT         COMMENT '错误信息',
    started_at      TIMESTAMP    COMMENT '实际开始时间',
    completed_at    TIMESTAMP    COMMENT '实际完成时间',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

CREATE INDEX idx_tb_collection_task_status       ON tb_collection_task(status);
CREATE INDEX idx_tb_collection_task_scheduled_at ON tb_collection_task(scheduled_at) WHERE scheduled_at IS NOT NULL;
CREATE INDEX idx_tb_collection_task_type         ON tb_collection_task(task_type);

-- 6. 采集定时规则表
COMMENT ON TABLE tb_collection_task_schedule IS '采集定时规则表：定义周期性自动采集任务';

CREATE TABLE IF NOT EXISTS tb_collection_task_schedule (
    id                  VARCHAR(32) PRIMARY KEY COMMENT '主键ID',
    name                VARCHAR(100) NOT NULL COMMENT '规则名称，如每日股票全量采集',
    task_type           VARCHAR(50)  NOT NULL COMMENT '任务类型，同tb_collection_task.task_type',
    task_params         JSONB        COMMENT '默认任务参数',
    data_source         VARCHAR(20)  COMMENT '默认数据源：akshare/tushare',
    cron_expression     VARCHAR(50)  NOT NULL COMMENT 'Cron表达式，如0 2 * * *表示每天2点',
    is_enabled          BOOLEAN      DEFAULT TRUE COMMENT '是否启用',
    last_triggered_at   TIMESTAMP    COMMENT '上次触发时间',
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

CREATE INDEX idx_tb_collection_task_schedule_enabled ON tb_collection_task_schedule(is_enabled) WHERE is_enabled = TRUE;
```

---

## 五、版本记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-05-10 | 初始创建：股票/公司/关联/任务/定时规则五张表 | AI助手 |
