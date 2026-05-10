-- ============================================
-- V1__baseline_init.sql
-- 基线脚本：股票与公司基础信息采集模块完整表结构
-- 包含：股票基础信息、公司基础信息、关联关系、采集任务、定时规则
-- ============================================

-- 1. 股票基础信息表
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
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_stock_basic IS '股票基础信息表：存储A股等市场股票静态属性';

CREATE INDEX IF NOT EXISTS idx_tb_stock_basic_stock_code ON tb_stock_basic(stock_code);
CREATE INDEX IF NOT EXISTS idx_tb_stock_basic_ts_code     ON tb_stock_basic(ts_code);
CREATE INDEX IF NOT EXISTS idx_tb_stock_basic_industry    ON tb_stock_basic(industry);
CREATE INDEX IF NOT EXISTS idx_tb_stock_basic_market      ON tb_stock_basic(market);
CREATE INDEX IF NOT EXISTS idx_tb_stock_basic_exchange    ON tb_stock_basic(exchange);

-- 2. 公司基础信息表
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

COMMENT ON TABLE tb_company_basic IS '公司基础信息表：存储上市公司工商信息与主营业务';

CREATE INDEX IF NOT EXISTS idx_tb_company_basic_usc_code ON tb_company_basic(unified_social_credit_code);
CREATE INDEX IF NOT EXISTS idx_tb_company_basic_name     ON tb_company_basic(name);
CREATE INDEX IF NOT EXISTS idx_tb_company_basic_industry ON tb_company_basic(industry);

-- 3. 股票与公司关联关系表
CREATE TABLE IF NOT EXISTS tb_relation_stock_company (
    id               VARCHAR(32) PRIMARY KEY,
    stock_code       VARCHAR(20)  NOT NULL,
    company_usc_code VARCHAR(50)  NOT NULL,
    exchange         VARCHAR(10),
    market_type      VARCHAR(20),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tb_relation_stock_company UNIQUE (stock_code, company_usc_code)
);

COMMENT ON TABLE tb_relation_stock_company IS '股票与公司关联关系表：一只股票对应一家公司';

CREATE INDEX IF NOT EXISTS idx_tb_relation_stock_company_code ON tb_relation_stock_company(stock_code);
CREATE INDEX IF NOT EXISTS idx_tb_relation_stock_company_usc  ON tb_relation_stock_company(company_usc_code);

-- 4. 采集任务执行表
CREATE TABLE IF NOT EXISTS tb_collection_task (
    id              VARCHAR(32) PRIMARY KEY,
    task_type       VARCHAR(50)  NOT NULL,
    task_params     JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    data_source     VARCHAR(20),
    total_count     INT          DEFAULT 0,
    success_count   INT          DEFAULT 0,
    fail_count      INT          DEFAULT 0,
    scheduled_at    TIMESTAMP,
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_collection_task IS '采集任务执行表：记录每次数据采集任务的执行状态与结果';

CREATE INDEX IF NOT EXISTS idx_tb_collection_task_status       ON tb_collection_task(status);
CREATE INDEX IF NOT EXISTS idx_tb_collection_task_scheduled_at ON tb_collection_task(scheduled_at) WHERE scheduled_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tb_collection_task_type         ON tb_collection_task(task_type);

-- 5. 采集定时规则表
CREATE TABLE IF NOT EXISTS tb_collection_task_schedule (
    id                  VARCHAR(32) PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    task_type           VARCHAR(50)  NOT NULL,
    task_params         JSONB,
    data_source         VARCHAR(20),
    cron_expression     VARCHAR(50)  NOT NULL,
    is_enabled          BOOLEAN      DEFAULT TRUE,
    last_triggered_at   TIMESTAMP,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_collection_task_schedule IS '采集定时规则表：定义周期性自动采集任务';

CREATE INDEX IF NOT EXISTS idx_tb_collection_task_schedule_enabled ON tb_collection_task_schedule(is_enabled) WHERE is_enabled = TRUE;
