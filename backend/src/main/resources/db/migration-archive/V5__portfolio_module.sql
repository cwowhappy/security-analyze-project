-- ============================================================
-- V5 Portfolio Module: 持仓管理模块数据库 Schema
-- ============================================================

-- 1. 组合类型枚举
CREATE TYPE portfolio_type AS ENUM ('REAL', 'SIMULATION');

-- 2. 交易类型枚举
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

-- 3. 组合 / 证券账户表
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

CREATE INDEX idx_portfolio_user ON portfolio(user_id);
CREATE INDEX idx_portfolio_deleted ON portfolio(is_deleted);

-- 4. 成交记录表
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

CREATE INDEX idx_tx_portfolio ON transaction_record(portfolio_id);
CREATE INDEX idx_tx_stock ON transaction_record(stock_code);
CREATE INDEX idx_tx_date ON transaction_record(trade_date);
CREATE INDEX idx_tx_portfolio_date ON transaction_record(portfolio_id, trade_date);
CREATE INDEX idx_tx_deleted ON transaction_record(is_deleted);

-- 5. 持仓汇总快照表
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

CREATE UNIQUE INDEX idx_position_unique_active ON position(portfolio_id, stock_code) WHERE is_deleted = FALSE;

CREATE INDEX idx_position_portfolio ON position(portfolio_id);
CREATE INDEX idx_position_stock ON position(stock_code);
CREATE INDEX idx_position_deleted ON position(is_deleted);

-- 6. 日行情表（盘后同步，P2 阶段启用采集）
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

CREATE INDEX idx_quote_date ON daily_quote(trade_date);
CREATE INDEX idx_quote_stock ON daily_quote(stock_code);

-- 7. 权限说明
-- 执行本脚本的数据库用户须与后端 application.yml / 采集模块 .env 中配置的应用用户一致。
-- 若表由其他超级用户创建，请另行授予应用用户对该 schema 下表、序列、类型的访问权限。
