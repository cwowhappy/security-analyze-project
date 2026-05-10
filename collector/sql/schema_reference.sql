-- ============================================================
-- Collector Module Schema Reference
-- 数据采集模块涉及的数据库表结构引用
--
-- 注意：本文件仅供查阅，实际建表由后端 migration 统一管理。
--       collector 模块通过 psycopg 直接操作以下表。
-- ============================================================

-- --------------------------------------------------------------
-- 1. 公司法人实体表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    unified_code VARCHAR(50) UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    industry VARCHAR(100),
    region VARCHAR(50),
    establish_date DATE,
    registered_capital DECIMAL(20,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------------
-- 2. 上市证券表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS company_security (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL UNIQUE,
    stock_name VARCHAR(100) NOT NULL,
    market VARCHAR(10),
    security_type VARCHAR(20),
    listing_date DATE,
    listing_status VARCHAR(20) DEFAULT 'listed',
    total_shares DECIMAL(20,4),        -- 总股本（股）
    circulating_shares DECIMAL(20,4),  -- 流通股本（股）
    market_cap DECIMAL(20,4),          -- 总市值（元）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------------
-- 3. 财务报表数据表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_report (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL,
    report_year INT NOT NULL,
    notice_date DATE,
    currency VARCHAR(10) DEFAULT 'CNY',

    -- 资产负债表
    total_assets DECIMAL(20,4),
    total_liabilities DECIMAL(20,4),
    total_equity DECIMAL(20,4),
    monetary_funds DECIMAL(20,4),
    accounts_receivable DECIMAL(20,4),
    inventory DECIMAL(20,4),
    total_current_assets DECIMAL(20,4),
    total_noncurrent_assets DECIMAL(20,4),
    total_current_liabilities DECIMAL(20,4),
    total_noncurrent_liabilities DECIMAL(20,4),

    -- 利润表
    total_revenue DECIMAL(20,4),
    operate_income DECIMAL(20,4),
    operate_cost DECIMAL(20,4),
    sale_expense DECIMAL(20,4),
    manage_expense DECIMAL(20,4),
    research_expense DECIMAL(20,4),
    finance_expense DECIMAL(20,4),
    operate_profit DECIMAL(20,4),
    total_profit DECIMAL(20,4),
    net_profit DECIMAL(20,4),
    parent_net_profit DECIMAL(20,4),

    -- 现金流量表
    operating_cash_flow DECIMAL(20,4),
    investing_cash_flow DECIMAL(20,4),
    financing_cash_flow DECIMAL(20,4),
    cce_add DECIMAL(20,4),
    end_cce DECIMAL(20,4),

    -- JSONB 原始数据
    balance_sheet JSONB,
    profit_sheet JSONB,
    cash_flow_sheet JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_fin_report UNIQUE (stock_code, report_date)
);

-- --------------------------------------------------------------
-- 4. 采集任务监控表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS collector_task_log (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status VARCHAR(16) NOT NULL,
    rows_affected INT,
    error_message TEXT,
    session_id VARCHAR(36) UNIQUE,
    params JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS collector_data_status (
    id BIGSERIAL PRIMARY KEY,
    data_type VARCHAR(32) NOT NULL UNIQUE,
    total_rows INT NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP,
    last_task_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS collector_task_progress (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    rows_created INT DEFAULT 0,
    rows_updated INT DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_progress_session_stock UNIQUE (session_id, stock_code)
);

-- --------------------------------------------------------------
-- 5. 财务报告采集同步状态表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS collector_stock_sync_status (
    stock_code VARCHAR(20) PRIMARY KEY,
    latest_report_date DATE,
    report_count INT DEFAULT 0,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------------
-- 6. 行业分类体系表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS industry_classification_standard (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(50)  NOT NULL,
    level_count INT          NOT NULL,
    description VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS industry_category (
    id              BIGSERIAL PRIMARY KEY,
    standard_code   VARCHAR(20)  NOT NULL REFERENCES industry_classification_standard(code),
    level           INT          NOT NULL CHECK (level IN (1, 2)),
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    parent_code     VARCHAR(20),
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (standard_code, level, code)
);

CREATE TABLE IF NOT EXISTS company_industry_mapping (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    standard_code   VARCHAR(20)  NOT NULL REFERENCES industry_classification_standard(code),
    level1_code     VARCHAR(20)  NOT NULL,
    level2_code     VARCHAR(20),
    is_primary      BOOLEAN      DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, standard_code, level2_code)
);

-- --------------------------------------------------------------
-- 7. 指数与 ETF 表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS index_info (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL UNIQUE,
    index_name VARCHAR(200) NOT NULL,
    index_type VARCHAR(50),
    market VARCHAR(10) NOT NULL DEFAULT 'CN',
    base_date DATE,
    base_point DECIMAL(20,4),
    component_count INT,
    publish_date DATE,
    source VARCHAR(50),
    is_core BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS index_history (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    granularity VARCHAR(10) NOT NULL DEFAULT 'day',
    open_price DECIMAL(20,4),
    high_price DECIMAL(20,4),
    low_price DECIMAL(20,4),
    close_price DECIMAL(20,4),
    volume BIGINT,
    amount DECIMAL(30,4),
    amplitude DECIMAL(10,4),
    change_pct DECIMAL(10,4),
    change_amount DECIMAL(20,4),
    turnover_rate DECIMAL(10,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_index_history UNIQUE (index_code, trade_date, granularity)
);

CREATE TABLE IF NOT EXISTS etf_info (
    id BIGSERIAL PRIMARY KEY,
    etf_code VARCHAR(20) NOT NULL UNIQUE,
    etf_name VARCHAR(200) NOT NULL,
    tracking_index_code VARCHAR(20),
    management_fee DECIMAL(10,4),
    fund_size DECIMAL(30,4),
    establish_date DATE,
    market VARCHAR(10) DEFAULT 'CN',
    source VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS index_etf_mapping (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,
    etf_code VARCHAR(20) NOT NULL,
    relation_type VARCHAR(20) DEFAULT 'track',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_index_etf_mapping UNIQUE (index_code, etf_code, relation_type)
);

-- --------------------------------------------------------------
-- 8. 股票基本面衍生指标物化表（阶段B预计算）
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_fundamental_metrics (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    report_year INTEGER NOT NULL,

    -- 同比增长率
    revenue_yoy DECIMAL(10,4),
    profit_yoy DECIMAL(10,4),
    asset_growth_rate DECIMAL(10,4),

    -- 效率指标
    roe DECIMAL(10,4),
    roa DECIMAL(10,4),
    asset_turnover DECIMAL(10,4),
    equity_multiplier DECIMAL(10,4),

    -- 偿债指标
    current_ratio DECIMAL(10,4),
    quick_ratio DECIMAL(10,4),

    -- 盈利质量
    cashflow_profit_ratio DECIMAL(10,4),
    period_expense_rate DECIMAL(10,4),

    -- 标准审计字段
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_fundamental_metrics UNIQUE (stock_code, report_year)
);

CREATE INDEX IF NOT EXISTS idx_sfm_stock_code ON stock_fundamental_metrics(stock_code);
CREATE INDEX IF NOT EXISTS idx_sfm_report_year ON stock_fundamental_metrics(report_year);
CREATE INDEX IF NOT EXISTS idx_sfm_is_deleted ON stock_fundamental_metrics(is_deleted);
CREATE INDEX IF NOT EXISTS idx_sfm_stock_year_deleted ON stock_fundamental_metrics(stock_code, report_year, is_deleted);

-- --------------------------------------------------------------
-- 9. 股票估值指标表（阶段C）
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_valuation_metrics (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    close_price DECIMAL(18,4),
    pe_ttm DECIMAL(10,4),
    pe_lyr DECIMAL(10,4),
    pb DECIMAL(10,4),
    ps_ttm DECIMAL(10,4),
    pe_ttm_percentile DECIMAL(5,4),
    pb_percentile DECIMAL(5,4),
    ps_ttm_percentile DECIMAL(5,4),
    dcf_fair_price DECIMAL(18,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_valuation_metrics UNIQUE (stock_code, trade_date)
);

CREATE INDEX IF NOT EXISTS idx_val_stock ON stock_valuation_metrics(stock_code);
CREATE INDEX IF NOT EXISTS idx_val_date ON stock_valuation_metrics(trade_date);
CREATE INDEX IF NOT EXISTS idx_val_stock_date ON stock_valuation_metrics(stock_code, trade_date);
