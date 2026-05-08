-- ============================================================
-- V1 Baseline: 证券分析系统完整数据库 Schema
-- 合并 migration V1~V5 全部对象，作为 Flyway 基线
-- PostgreSQL 16+
-- ============================================================

-- ============================================================
-- 1. 枚举类型
-- ============================================================
CREATE TYPE user_status AS ENUM ('PENDING', 'APPROVED', 'DISABLED');

CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');

CREATE TYPE portfolio_type AS ENUM ('REAL', 'SIMULATION');

CREATE TYPE trade_type AS ENUM (
    'BUY',
    'SELL',
    'DIVIDEND',
    'BONUS',
    'RIGHTS',
    'SPLIT',
    'MERGER',
    'OTHER'
);

-- ============================================================
-- 2. 公司法人实体表
-- ============================================================
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    unified_code VARCHAR(50) UNIQUE,              -- 统一社会信用代码（预留）
    company_name VARCHAR(200) NOT NULL,           -- 公司全称
    short_name VARCHAR(100),                      -- 公司简称
    industry VARCHAR(100),                        -- 所属行业
    region VARCHAR(50),                           -- 地区（省份/直辖市）
    establish_date DATE,                          -- 成立日期
    registered_capital DECIMAL(20,4),             -- 注册资本（万元）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. 上市证券表（支持 A股/B股/H股 等多证券）
-- ============================================================
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

-- ============================================================
-- 4. 系统用户表（Spring Security + JWT）
-- ============================================================
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

-- ============================================================
-- 5. 采集任务日志表（支持 Session 级故障恢复）
-- ============================================================
CREATE TABLE IF NOT EXISTS collector_task_log (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status VARCHAR(16) NOT NULL,
    rows_affected INT,
    error_message TEXT,
    session_id VARCHAR(36) UNIQUE,                -- Session UUID（用于断点续传）
    params JSONB,                                 -- 任务参数快照
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. 采集数据状态快照表
-- ============================================================
CREATE TABLE IF NOT EXISTS collector_data_status (
    id BIGSERIAL PRIMARY KEY,
    data_type VARCHAR(32) NOT NULL UNIQUE,
    total_rows INT NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP,
    last_task_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 7. 财务报告采集同步状态表（支持增量更新）
-- ============================================================
CREATE TABLE IF NOT EXISTS collector_stock_sync_status (
    stock_code VARCHAR(20) PRIMARY KEY,
    latest_report_date DATE,                      -- 该股票已采集的最新报告期
    report_count INT DEFAULT 0,                   -- 累计采集报告数
    last_sync_at TIMESTAMP,                       -- 最后同步时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. 采集任务进度表（Session 故障恢复用）
-- ============================================================
CREATE TABLE IF NOT EXISTS collector_task_progress (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending / running / success / failed
    rows_created INT DEFAULT 0,
    rows_updated INT DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_progress_session_stock UNIQUE (session_id, stock_code)
);

-- ============================================================
-- 9. 财务报表数据表（资产负债表/利润表/现金流量表）
-- ============================================================
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

-- ============================================================
-- 10. 行业分类标准字典
-- ============================================================
CREATE TABLE IF NOT EXISTS industry_classification_standard (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(50)  NOT NULL,
    level_count INT          NOT NULL,
    description VARCHAR(200)
);

INSERT INTO industry_classification_standard (code, name, level_count, description) VALUES
('SW', '申万行业分类', 2, '申万宏源研究所发布的行业分类标准，含一级31个、二级124个'),
('EM', '东方财富行业分类', 1, '东方财富网行业板块分类，以二级为主')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 11. 行业分类维度表
-- ============================================================
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

-- ============================================================
-- 12. 公司与行业分类映射表（支持一对多）
-- ============================================================
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

-- ============================================================
-- 13. 指数基本信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS index_info (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL UNIQUE,       -- 指数代码，如 000001、HSI、SPX
    index_name VARCHAR(200) NOT NULL,             -- 指数名称
    index_type VARCHAR(50),                       -- 指数类型：宽基/行业/概念/策略/主题/其他
    market VARCHAR(10) NOT NULL DEFAULT 'CN',     -- 市场：SH / SZ / BJ / HK / US / CN
    base_date DATE,                               -- 基日
    base_point DECIMAL(20,4),                     -- 基点
    component_count INT,                          -- 成分股数量（如有）
    publish_date DATE,                            -- 发布日期
    source VARCHAR(50),                           -- 数据来源标识
    is_core BOOLEAN DEFAULT FALSE,                -- 是否核心指数
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 初始化核心指数标记（宽基）
UPDATE index_info SET is_core = TRUE WHERE index_code IN (
    '000001',  -- 上证指数
    '399001',  -- 深证成指
    '399006',  -- 创业板指
    '000300',  -- 沪深300
    '000016',  -- 上证50
    '000905',  -- 中证500
    '000852',  -- 中证1000
    '000688',  -- 科创50
    '399005',  -- 中小板指
    '399673'   -- 创业板50
);

-- 初始化核心指数标记（行业）
UPDATE index_info SET is_core = TRUE WHERE index_code IN (
    '399989',  -- 中证医疗
    '399997',  -- 中证白酒
    '399998',  -- 中证煤炭
    '399995',  -- 中证基建
    '399991',  -- 中证一带一路
    '399993',  -- 中证生物科技
    '399996',  -- 中证智能家居
    '399994'   -- 中证信息安全
);

-- 初始化核心指数标记（主题）
UPDATE index_info SET is_core = TRUE WHERE index_code IN (
    '000021',  -- 180治理
    '000022',  -- 上证公司债
    '000042',  -- 上证央企
    '000043',  -- 上证超大盘
    '000044'   -- 上证中盘
);

-- ============================================================
-- 14. 指数历史行情表（天/周/月合一，通过 granularity 区分）
-- ============================================================
CREATE TABLE IF NOT EXISTS index_history (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,              -- 指数代码
    trade_date DATE NOT NULL,                     -- 交易日期
    granularity VARCHAR(10) NOT NULL DEFAULT 'day', -- 粒度：day / week / month
    open_price DECIMAL(20,4),                     -- 开盘价
    high_price DECIMAL(20,4),                     -- 最高价
    low_price DECIMAL(20,4),                      -- 最低价
    close_price DECIMAL(20,4),                    -- 收盘价
    volume BIGINT,                                -- 成交量（股/手，依数据源）
    amount DECIMAL(30,4),                         -- 成交额
    amplitude DECIMAL(10,4),                      -- 振幅 (%)
    change_pct DECIMAL(10,4),                     -- 涨跌幅 (%)
    change_amount DECIMAL(20,4),                  -- 涨跌额
    turnover_rate DECIMAL(10,4),                  -- 换手率 (%)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_index_history UNIQUE (index_code, trade_date, granularity)
);

-- ============================================================
-- 15. ETF 基本信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS etf_info (
    id BIGSERIAL PRIMARY KEY,
    etf_code VARCHAR(20) NOT NULL UNIQUE,         -- ETF 代码
    etf_name VARCHAR(200) NOT NULL,               -- ETF 名称
    tracking_index_code VARCHAR(20),              -- 跟踪指数代码（预留扩展）
    management_fee DECIMAL(10,4),                 -- 管理费率 (%)
    fund_size DECIMAL(30,4),                      -- 基金规模（元）
    establish_date DATE,                          -- 成立日期
    market VARCHAR(10) DEFAULT 'CN',              -- 市场
    source VARCHAR(50),                           -- 数据来源标识
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 16. 指数-ETF 关联映射表（支持多对多）
-- ============================================================
CREATE TABLE IF NOT EXISTS index_etf_mapping (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,
    etf_code VARCHAR(20) NOT NULL,
    relation_type VARCHAR(20) DEFAULT 'track',    -- 关系类型：track（跟踪）/ related（相关）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_index_etf_mapping UNIQUE (index_code, etf_code, relation_type)
);

-- ============================================================
-- 17. 组合 / 证券账户表
-- ============================================================
CREATE TABLE IF NOT EXISTS portfolio (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type portfolio_type NOT NULL DEFAULT 'REAL',
    broker VARCHAR(100),
    description VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 18. 成交记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_record (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    trade_type trade_type NOT NULL,
    price DECIMAL(18, 4),
    quantity DECIMAL(18, 4) NOT NULL,
    fee DECIMAL(18, 4) NOT NULL DEFAULT 0,
    tax DECIMAL(18, 4) NOT NULL DEFAULT 0,
    amount DECIMAL(18, 4),
    realized_pnl DECIMAL(18, 4) DEFAULT 0,
    remark VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 19. 持仓汇总快照表
-- ============================================================
CREATE TABLE IF NOT EXISTS position (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolio(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL,
    current_quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,
    avg_cost DECIMAL(18, 4) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(18, 4) NOT NULL DEFAULT 0,
    first_buy_date DATE,
    last_trade_date DATE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 20. 日行情表（盘后同步，P2 阶段启用采集）
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_quote (
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(18, 4),
    high_price DECIMAL(18, 4),
    low_price DECIMAL(18, 4),
    close_price DECIMAL(18, 4) NOT NULL,
    volume BIGINT,
    amount DECIMAL(18, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_code, trade_date)
);

-- ============================================================
-- 21. 索引
-- ============================================================

-- company 索引
CREATE INDEX IF NOT EXISTS idx_company_unified_code ON company(unified_code);
CREATE INDEX IF NOT EXISTS idx_company_name ON company(company_name);
CREATE INDEX IF NOT EXISTS idx_company_industry ON company(industry);
CREATE INDEX IF NOT EXISTS idx_company_region ON company(region);

-- company_security 索引
CREATE INDEX IF NOT EXISTS idx_cs_stock_code ON company_security(stock_code);
CREATE INDEX IF NOT EXISTS idx_cs_company_id ON company_security(company_id);
CREATE INDEX IF NOT EXISTS idx_cs_market ON company_security(market);
CREATE INDEX IF NOT EXISTS idx_cs_security_type ON company_security(security_type);
CREATE INDEX IF NOT EXISTS idx_cs_listing_status ON company_security(listing_status);

-- sys_user 索引
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user(status);
CREATE INDEX IF NOT EXISTS idx_sys_user_role ON sys_user(role);

-- collector_task_log 索引
CREATE INDEX IF NOT EXISTS idx_task_log_started_at ON collector_task_log(started_at);
CREATE INDEX IF NOT EXISTS idx_task_log_session_id ON collector_task_log(session_id);
CREATE INDEX IF NOT EXISTS idx_task_log_task_type ON collector_task_log(task_type);
CREATE INDEX IF NOT EXISTS idx_task_log_status ON collector_task_log(status);

-- collector_data_status 索引
CREATE INDEX IF NOT EXISTS idx_data_status_type ON collector_data_status(data_type);

-- collector_stock_sync_status 索引
CREATE INDEX IF NOT EXISTS idx_csss_last_sync ON collector_stock_sync_status(last_sync_at);

-- collector_task_progress 索引
CREATE INDEX IF NOT EXISTS idx_task_progress_session ON collector_task_progress(session_id);
CREATE INDEX IF NOT EXISTS idx_task_progress_status ON collector_task_progress(status);

-- financial_report 索引
CREATE INDEX IF NOT EXISTS idx_fin_report_stock_code ON financial_report(stock_code);
CREATE INDEX IF NOT EXISTS idx_fin_report_date ON financial_report(report_date);
CREATE INDEX IF NOT EXISTS idx_fin_report_type ON financial_report(report_type);
CREATE INDEX IF NOT EXISTS idx_fin_report_year ON financial_report(report_year);
CREATE INDEX IF NOT EXISTS idx_fin_report_notice ON financial_report(notice_date);

-- JSONB 字段 GIN 索引（按需启用：若需要通过 JSONB 内部键值查询时取消注释）
-- CREATE INDEX IF NOT EXISTS idx_fin_report_balance_gin ON financial_report USING GIN (balance_sheet);
-- CREATE INDEX IF NOT EXISTS idx_fin_report_profit_gin ON financial_report USING GIN (profit_sheet);
-- CREATE INDEX IF NOT EXISTS idx_fin_report_cashflow_gin ON financial_report USING GIN (cash_flow_sheet);

-- industry_category 索引
CREATE INDEX IF NOT EXISTS idx_industry_category_standard ON industry_category(standard_code);
CREATE INDEX IF NOT EXISTS idx_industry_category_standard_level ON industry_category(standard_code, level);
CREATE INDEX IF NOT EXISTS idx_industry_category_parent ON industry_category(standard_code, parent_code);

-- company_industry_mapping 索引
CREATE INDEX IF NOT EXISTS idx_cim_company ON company_industry_mapping(company_id);
CREATE INDEX IF NOT EXISTS idx_cim_standard_l1 ON company_industry_mapping(standard_code, level1_code);
CREATE INDEX IF NOT EXISTS idx_cim_standard_l2 ON company_industry_mapping(standard_code, level2_code);
CREATE INDEX IF NOT EXISTS idx_cim_company_standard ON company_industry_mapping(company_id, standard_code);

-- index_info 索引
CREATE INDEX IF NOT EXISTS idx_index_type ON index_info(index_type);
CREATE INDEX IF NOT EXISTS idx_index_market ON index_info(market);
CREATE INDEX IF NOT EXISTS idx_index_core ON index_info(is_core, index_type);

-- index_history 索引
CREATE INDEX IF NOT EXISTS idx_ih_index_code ON index_history(index_code);
CREATE INDEX IF NOT EXISTS idx_ih_trade_date ON index_history(trade_date);
CREATE INDEX IF NOT EXISTS idx_ih_granularity ON index_history(granularity);
CREATE INDEX IF NOT EXISTS idx_ih_code_date ON index_history(index_code, trade_date);
CREATE INDEX IF NOT EXISTS idx_ih_code_gran_date ON index_history(index_code, granularity, trade_date);

-- etf_info 索引
CREATE INDEX IF NOT EXISTS idx_etf_tracking ON etf_info(tracking_index_code);

-- index_etf_mapping 索引
CREATE INDEX IF NOT EXISTS idx_iem_index ON index_etf_mapping(index_code);
CREATE INDEX IF NOT EXISTS idx_iem_etf ON index_etf_mapping(etf_code);

-- portfolio 索引
CREATE INDEX IF NOT EXISTS idx_portfolio_user ON portfolio(user_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_deleted ON portfolio(is_deleted);

-- transaction_record 索引
CREATE INDEX IF NOT EXISTS idx_tx_portfolio ON transaction_record(portfolio_id);
CREATE INDEX IF NOT EXISTS idx_tx_stock ON transaction_record(stock_code);
CREATE INDEX IF NOT EXISTS idx_tx_date ON transaction_record(trade_date);
CREATE INDEX IF NOT EXISTS idx_tx_portfolio_date ON transaction_record(portfolio_id, trade_date);
CREATE INDEX IF NOT EXISTS idx_tx_deleted ON transaction_record(is_deleted);

-- position 索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_position_unique_active ON position(portfolio_id, stock_code) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_position_portfolio ON position(portfolio_id);
CREATE INDEX IF NOT EXISTS idx_position_stock ON position(stock_code);
CREATE INDEX IF NOT EXISTS idx_position_deleted ON position(is_deleted);

-- daily_quote 索引
CREATE INDEX IF NOT EXISTS idx_quote_date ON daily_quote(trade_date);
CREATE INDEX IF NOT EXISTS idx_quote_stock ON daily_quote(stock_code);

-- ============================================================
-- 22. 数据库对象注释
-- ============================================================

COMMENT ON TABLE company IS '公司法人实体表';
COMMENT ON COLUMN company.unified_code IS '统一社会信用代码（预留）';
COMMENT ON COLUMN company.company_name IS '公司全称';
COMMENT ON COLUMN company.short_name IS '公司简称';
COMMENT ON COLUMN company.registered_capital IS '注册资本（万元）';

COMMENT ON TABLE company_security IS '上市证券表（支持 A股/B股/H股 等多证券）';
COMMENT ON COLUMN company_security.stock_code IS '股票代码，全局唯一';
COMMENT ON COLUMN company_security.market IS '市场板块：SH / SZ / BJ / HK';
COMMENT ON COLUMN company_security.listing_status IS '上市状态：listed / suspended / delisted';

COMMENT ON TABLE financial_report IS '财务报表数据表（资产负债表/利润表/现金流量表）';
COMMENT ON COLUMN financial_report.stock_code IS '股票代码';
COMMENT ON COLUMN financial_report.report_date IS '报告期';
COMMENT ON COLUMN financial_report.report_type IS '报告类型';
COMMENT ON COLUMN financial_report.balance_sheet IS '资产负债表完整原始数据（JSONB）';
COMMENT ON COLUMN financial_report.profit_sheet IS '利润表完整原始数据（JSONB）';
COMMENT ON COLUMN financial_report.cash_flow_sheet IS '现金流量表完整原始数据（JSONB）';

COMMENT ON TABLE index_info IS '指数基本信息表';
COMMENT ON COLUMN index_info.index_code IS '指数代码，如 000001';
COMMENT ON COLUMN index_info.is_core IS '是否核心指数';

COMMENT ON TABLE index_history IS '指数历史行情表（天/周/月合一）';
COMMENT ON COLUMN index_history.granularity IS '粒度：day / week / month';

COMMENT ON TABLE etf_info IS 'ETF 基本信息表';
COMMENT ON TABLE index_etf_mapping IS '指数-ETF 关联映射表（支持多对多）';

COMMENT ON TABLE collector_task_log IS '采集任务日志表（支持 Session 级故障恢复）';
COMMENT ON TABLE collector_task_progress IS '采集任务进度表（Session 故障恢复用）';
COMMENT ON TABLE collector_stock_sync_status IS '财务报告采集同步状态表（支持增量更新）';

COMMENT ON TABLE industry_classification_standard IS '行业分类标准字典';
COMMENT ON TABLE industry_category IS '行业分类维度表（支持多标准、多级）';
COMMENT ON TABLE company_industry_mapping IS '公司与行业分类映射表（支持一对多）';

COMMENT ON TABLE portfolio IS '组合 / 证券账户表';
COMMENT ON TABLE transaction_record IS '成交记录表';
COMMENT ON TABLE position IS '持仓汇总快照表';
COMMENT ON TABLE daily_quote IS '日行情表（盘后同步，P2 阶段启用采集）';
