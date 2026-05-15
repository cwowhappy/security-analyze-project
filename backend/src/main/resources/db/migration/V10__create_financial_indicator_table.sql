-- ============================================
-- V10__create_financial_indicator_table.sql
-- 创建财务指标计算结果表
-- ============================================

CREATE TABLE IF NOT EXISTS tb_financial_indicator (
    id                      VARCHAR(32) PRIMARY KEY,
    stock_code              VARCHAR(10)  NOT NULL,
    report_date             DATE         NOT NULL,
    report_type             VARCHAR(10)  NOT NULL DEFAULT 'Y',

    -- 盈利能力
    roe                     DECIMAL(10, 4),
    roa                     DECIMAL(10, 4),
    roic                    DECIMAL(10, 4),
    gross_margin            DECIMAL(10, 4),
    net_margin              DECIMAL(10, 4),
    net_margin_excl         DECIMAL(10, 4),

    -- 偿债能力
    debt_ratio              DECIMAL(10, 4),
    current_ratio           DECIMAL(10, 4),
    quick_ratio             DECIMAL(10, 4),
    net_debt_ratio          DECIMAL(10, 4),
    equity_ratio            DECIMAL(10, 4),

    -- 运营效率
    dso                     DECIMAL(10, 2),
    dio                     DECIMAL(10, 2),
    dpo                     DECIMAL(10, 2),
    ccc                     DECIMAL(10, 2),
    asset_turnover          DECIMAL(10, 4),
    fixed_asset_turnover    DECIMAL(10, 4),

    -- 成长性（YoY 增速 %）
    revenue_growth          DECIMAL(10, 4),
    np_parent_growth        DECIMAL(10, 4),
    np_excl_growth          DECIMAL(10, 4),
    cfo_growth              DECIMAL(10, 4),
    equity_growth           DECIMAL(10, 4),
    asset_growth            DECIMAL(10, 4),

    -- 估值
    pe                      DECIMAL(18, 4),
    pb                      DECIMAL(18, 4),
    ps                      DECIMAL(18, 4),
    peg                     DECIMAL(10, 4),
    ev_ebitda               DECIMAL(10, 4),
    dividend_yield          DECIMAL(10, 4),
    market_cap              DECIMAL(18, 2),

    -- 现金流质量
    cfo_to_np               DECIMAL(10, 4),

    -- 元数据
    data_source             VARCHAR(20),
    updated_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tb_financial_indicator UNIQUE (stock_code, report_date, report_type)
);

COMMENT ON TABLE tb_financial_indicator IS '财务指标表：存储计算后的财务分析指标';

CREATE INDEX IF NOT EXISTS idx_tb_financial_indicator_stock_date ON tb_financial_indicator(stock_code, report_date DESC);
CREATE INDEX IF NOT EXISTS idx_tb_financial_indicator_report_type ON tb_financial_indicator(report_type);
