-- ============================================================
-- V3: 财务报表数据模型
-- 支持资产负债表、利润表、现金流量表的核心指标结构化存储
-- 完整原始数据通过 JSONB 保留
-- ============================================================

CREATE TABLE IF NOT EXISTS financial_report (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL,
    report_year INTEGER NOT NULL,
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

-- 索引
CREATE INDEX idx_fin_report_stock_code ON financial_report(stock_code);
CREATE INDEX idx_fin_report_date ON financial_report(report_date);
CREATE INDEX idx_fin_report_type ON financial_report(report_type);
CREATE INDEX idx_fin_report_year ON financial_report(report_year);
CREATE INDEX idx_fin_report_notice ON financial_report(notice_date);
