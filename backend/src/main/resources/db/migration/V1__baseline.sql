-- ============================================================
-- V1 Baseline: 证券分析系统完整数据库 Schema
-- 合并历史 V1~V6 脚本的最终状态，去除历史变更与数据迁移逻辑
-- ============================================================

-- 自定义枚举类型
CREATE TYPE user_status AS ENUM ('PENDING', 'APPROVED', 'DISABLED');
CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');

-- 1. 公司法人实体表
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    unified_code VARCHAR(50) UNIQUE,              -- 统一社会信用代码（预留，初期可为空）
    company_name VARCHAR(200) NOT NULL,           -- 公司全称
    short_name VARCHAR(100),                      -- 公司简称
    industry VARCHAR(100),                        -- 所属行业
    region VARCHAR(50),                           -- 地区（省份/直辖市）
    establish_date DATE,                          -- 成立日期
    registered_capital DECIMAL(20,4),             -- 注册资本（万元）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_name ON company(company_name);
CREATE INDEX idx_company_industry ON company(industry);
CREATE INDEX idx_company_region ON company(region);

-- 2. 上市证券表
CREATE TABLE IF NOT EXISTS company_security (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL UNIQUE,       -- 股票代码，全局唯一
    stock_name VARCHAR(100) NOT NULL,             -- 证券简称
    market VARCHAR(10),                           -- 市场板块：SH / SZ / BJ / HK
    security_type VARCHAR(20),                    -- 证券类型：A股 / B股 / H股 / 优先股 / ADR
    listing_date DATE,                            -- 在该市场的上市日期
    listing_status VARCHAR(20) DEFAULT 'listed',  -- 上市状态：listed / suspended / delisted
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cs_company_id ON company_security(company_id);
CREATE INDEX idx_cs_market ON company_security(market);
CREATE INDEX idx_cs_security_type ON company_security(security_type);
CREATE INDEX idx_cs_listing_status ON company_security(listing_status);

-- 3. 采集任务日志表
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

CREATE INDEX idx_task_log_started_at ON collector_task_log(started_at);
CREATE INDEX idx_task_log_task_type ON collector_task_log(task_type);
CREATE INDEX idx_task_log_status ON collector_task_log(status);

-- 4. 采集数据状态表
CREATE TABLE IF NOT EXISTS collector_data_status (
    id BIGSERIAL PRIMARY KEY,
    data_type VARCHAR(32) NOT NULL UNIQUE,
    total_rows INT NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP,
    last_task_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);



-- 5. 采集任务进度表（Session 故障恢复）
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

CREATE INDEX idx_task_progress_session ON collector_task_progress(session_id);
CREATE INDEX idx_task_progress_status ON collector_task_progress(status);

-- 6. 财务报表表
CREATE TABLE IF NOT EXISTS financial_report (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL,
    report_year INT NOT NULL,
    notice_date DATE,
    currency VARCHAR(10) DEFAULT 'CNY',

    -- 资产负债表核心指标
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

    -- 利润表核心指标
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

    -- 现金流量表核心指标
    operating_cash_flow DECIMAL(20,4),
    investing_cash_flow DECIMAL(20,4),
    financing_cash_flow DECIMAL(20,4),
    cce_add DECIMAL(20,4),
    end_cce DECIMAL(20,4),

    -- 完整原始数据（JSONB）
    balance_sheet JSONB,
    profit_sheet JSONB,
    cash_flow_sheet JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_fin_report UNIQUE (stock_code, report_date)
);

CREATE INDEX idx_fin_report_stock_code ON financial_report(stock_code);
CREATE INDEX idx_fin_report_date ON financial_report(report_date);
CREATE INDEX idx_fin_report_type ON financial_report(report_type);
CREATE INDEX idx_fin_report_year ON financial_report(report_year);
CREATE INDEX idx_fin_report_notice ON financial_report(notice_date);

-- 7. 财务报告采集同步状态表
CREATE TABLE IF NOT EXISTS collector_stock_sync_status (
    stock_code VARCHAR(20) PRIMARY KEY,
    latest_report_date DATE,              -- 该股票已采集的最新报告期
    report_count INT DEFAULT 0,           -- 累计采集报告数
    last_sync_at TIMESTAMP,               -- 最后同步时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_csss_last_sync ON collector_stock_sync_status(last_sync_at);

-- 8. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    status user_status NOT NULL DEFAULT 'PENDING',
    role user_role NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_user_status ON sys_user(status);
CREATE INDEX idx_sys_user_role ON sys_user(role);
