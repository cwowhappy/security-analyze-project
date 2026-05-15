-- ============================================
-- V9__create_financial_statement_tables.sql
-- 创建财务三表：利润表、资产负债表、现金流量表
-- ============================================

-- 1. 利润表
CREATE TABLE IF NOT EXISTS tb_financial_income (
    id                  VARCHAR(32) PRIMARY KEY,
    stock_code          VARCHAR(10)  NOT NULL,
    report_date         DATE         NOT NULL,
    report_type         VARCHAR(10)  NOT NULL DEFAULT 'Y',
    basic_eps           DECIMAL(18, 4),
    diluted_eps         DECIMAL(18, 4),
    total_revenue       DECIMAL(18, 2),
    revenue             DECIMAL(18, 2),
    operating_cost      DECIMAL(18, 2),
    gross_profit        DECIMAL(18, 2),
    selling_expense     DECIMAL(18, 2),
    admin_expense       DECIMAL(18, 2),
    rd_expense          DECIMAL(18, 2),
    financial_expense   DECIMAL(18, 2),
    operating_profit    DECIMAL(18, 2),
    total_profit        DECIMAL(18, 2),
    net_profit          DECIMAL(18, 2),
    np_parent_company   DECIMAL(18, 2),
    np_excl_nonrecurring DECIMAL(18, 2),
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tb_financial_income UNIQUE (stock_code, report_date, report_type)
);

COMMENT ON TABLE tb_financial_income IS '利润表：存储上市公司利润表数据';

CREATE INDEX IF NOT EXISTS idx_tb_financial_income_stock_date ON tb_financial_income(stock_code, report_date DESC);
CREATE INDEX IF NOT EXISTS idx_tb_financial_income_report_type ON tb_financial_income(report_type);

-- 2. 资产负债表
CREATE TABLE IF NOT EXISTS tb_financial_balance (
    id                      VARCHAR(32) PRIMARY KEY,
    stock_code              VARCHAR(10)  NOT NULL,
    report_date             DATE         NOT NULL,
    report_type             VARCHAR(10)  NOT NULL DEFAULT 'Y',
    total_assets            DECIMAL(18, 2),
    total_liabilities       DECIMAL(18, 2),
    total_equity            DECIMAL(18, 2),
    equity_parent_company   DECIMAL(18, 2),
    current_assets          DECIMAL(18, 2),
    non_current_assets      DECIMAL(18, 2),
    cash_equivalents        DECIMAL(18, 2),
    accounts_receivable     DECIMAL(18, 2),
    inventories             DECIMAL(18, 2),
    current_liabilities     DECIMAL(18, 2),
    non_current_liabilities DECIMAL(18, 2),
    accounts_payable        DECIMAL(18, 2),
    short_term_borrowings   DECIMAL(18, 2),
    long_term_borrowings    DECIMAL(18, 2),
    goodwill                DECIMAL(18, 2),
    updated_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tb_financial_balance UNIQUE (stock_code, report_date, report_type)
);

COMMENT ON TABLE tb_financial_balance IS '资产负债表：存储上市公司资产负债表数据';

CREATE INDEX IF NOT EXISTS idx_tb_financial_balance_stock_date ON tb_financial_balance(stock_code, report_date DESC);
CREATE INDEX IF NOT EXISTS idx_tb_financial_balance_report_type ON tb_financial_balance(report_type);

-- 3. 现金流量表
CREATE TABLE IF NOT EXISTS tb_financial_cashflow (
    id                      VARCHAR(32) PRIMARY KEY,
    stock_code              VARCHAR(10)  NOT NULL,
    report_date             DATE         NOT NULL,
    report_type             VARCHAR(10)  NOT NULL DEFAULT 'Y',
    cf_operating            DECIMAL(18, 2),
    cf_investing            DECIMAL(18, 2),
    cf_financing            DECIMAL(18, 2),
    net_cash_flow           DECIMAL(18, 2),
    free_cash_flow          DECIMAL(18, 2),
    capex                   DECIMAL(18, 2),
    cash_received_operating DECIMAL(18, 2),
    tax_paid                DECIMAL(18, 2),
    updated_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tb_financial_cashflow UNIQUE (stock_code, report_date, report_type)
);

COMMENT ON TABLE tb_financial_cashflow IS '现金流量表：存储上市公司现金流量表数据';

CREATE INDEX IF NOT EXISTS idx_tb_financial_cashflow_stock_date ON tb_financial_cashflow(stock_code, report_date DESC);
CREATE INDEX IF NOT EXISTS idx_tb_financial_cashflow_report_type ON tb_financial_cashflow(report_type);
