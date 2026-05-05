-- ============================================================
-- V3 Index Module: 指数基本信息、历史行情、ETF 及关联映射
-- ============================================================

-- 1. 指数基本信息表
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_index_code ON index_info(index_code);
CREATE INDEX idx_index_type ON index_info(index_type);
CREATE INDEX idx_index_market ON index_info(market);

-- 2. 指数历史行情表（天/周/月合一，通过 granularity 区分）
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

CREATE INDEX idx_ih_index_code ON index_history(index_code);
CREATE INDEX idx_ih_trade_date ON index_history(trade_date);
CREATE INDEX idx_ih_granularity ON index_history(granularity);
CREATE INDEX idx_ih_code_date ON index_history(index_code, trade_date);
CREATE INDEX idx_ih_code_gran_date ON index_history(index_code, granularity, trade_date);

-- 3. ETF 基本信息表
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

CREATE INDEX idx_etf_code ON etf_info(etf_code);
CREATE INDEX idx_etf_tracking ON etf_info(tracking_index_code);

-- 4. 指数-ETF 关联映射表（支持多对多）
CREATE TABLE IF NOT EXISTS index_etf_mapping (
    id BIGSERIAL PRIMARY KEY,
    index_code VARCHAR(20) NOT NULL,
    etf_code VARCHAR(20) NOT NULL,
    relation_type VARCHAR(20) DEFAULT 'track',    -- 关系类型：track（跟踪）/ related（相关）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_index_etf_mapping UNIQUE (index_code, etf_code, relation_type)
);

CREATE INDEX idx_iem_index ON index_etf_mapping(index_code);
CREATE INDEX idx_iem_etf ON index_etf_mapping(etf_code);
