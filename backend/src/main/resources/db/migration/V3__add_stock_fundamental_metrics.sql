-- ============================================================
-- V3: 新增股票基本面衍生指标物化表
--
-- 目的：将需要跨期/跨表计算的衍生指标（同比增长率、效率指标、
--       偿债指标、盈利质量）预计算并物化，提升查询性能，
--       为阶段B的同比箭头、杜邦分析、行业排名提供数据基础。
-- ============================================================

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

-- 股票代码索引（按股票查询历史指标）
CREATE INDEX IF NOT EXISTS idx_sfm_stock_code ON stock_fundamental_metrics(stock_code);

-- 报告年度索引（按年度批量查询）
CREATE INDEX IF NOT EXISTS idx_sfm_report_year ON stock_fundamental_metrics(report_year);

-- 逻辑删除过滤索引
CREATE INDEX IF NOT EXISTS idx_sfm_is_deleted ON stock_fundamental_metrics(is_deleted);

-- 联合索引：用于行业排名（需 JOIN company_security → company 获取行业）
-- 实际查询路径：先按 stock_code 定位，再过滤 is_deleted；此处预留便于后续扩展
CREATE INDEX IF NOT EXISTS idx_sfm_stock_year_deleted ON stock_fundamental_metrics(stock_code, report_year, is_deleted);

-- 表注释
COMMENT ON TABLE stock_fundamental_metrics IS '股票基本面衍生指标物化表（阶段B预计算）';
COMMENT ON COLUMN stock_fundamental_metrics.stock_code IS '股票代码';
COMMENT ON COLUMN stock_fundamental_metrics.report_year IS '报告年度';
COMMENT ON COLUMN stock_fundamental_metrics.revenue_yoy IS '营业收入同比增长率 %';
COMMENT ON COLUMN stock_fundamental_metrics.profit_yoy IS '归母净利润同比增长率 %';
COMMENT ON COLUMN stock_fundamental_metrics.asset_growth_rate IS '总资产同比增长率 %';
COMMENT ON COLUMN stock_fundamental_metrics.roe IS '净资产收益率 ROE %';
COMMENT ON COLUMN stock_fundamental_metrics.roa IS '总资产收益率 ROA %';
COMMENT ON COLUMN stock_fundamental_metrics.asset_turnover IS '总资产周转率（次）';
COMMENT ON COLUMN stock_fundamental_metrics.equity_multiplier IS '权益乘数';
COMMENT ON COLUMN stock_fundamental_metrics.current_ratio IS '流动比率';
COMMENT ON COLUMN stock_fundamental_metrics.quick_ratio IS '速动比率';
COMMENT ON COLUMN stock_fundamental_metrics.cashflow_profit_ratio IS '经营现金流/净利润比 %';
COMMENT ON COLUMN stock_fundamental_metrics.period_expense_rate IS '期间费用率 %';
