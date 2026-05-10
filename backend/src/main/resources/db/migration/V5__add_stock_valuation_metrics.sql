-- ============================================================
-- V5: 新增 stock_valuation_metrics 估值指标表
-- 存储每日估值指标（PE/PB/PS）及历史分位数，支持阶段C估值分析
-- ============================================================

CREATE TABLE IF NOT EXISTS stock_valuation_metrics (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,          -- 行情日期
    close_price DECIMAL(18,4),         -- 当日收盘价

    -- 估值指标
    pe_ttm DECIMAL(10,4),              -- 滚动市盈率（总市值 / 最近4个季度归母净利润）
    pe_lyr DECIMAL(10,4),              -- 静态市盈率（总市值 / 最近年报归母净利润）
    pb DECIMAL(10,4),                  -- 市净率
    ps_ttm DECIMAL(10,4),              -- 滚动市销率

    -- 历史分位数（基于该股票自身历史每日估值数据计算，近5年）
    pe_ttm_percentile DECIMAL(5,4),    -- PE_TTM 历史分位 0~1
    pb_percentile DECIMAL(5,4),        -- PB 历史分位
    ps_ttm_percentile DECIMAL(5,4),    -- PS_TTM 历史分位

    -- DCF 近似（基于年报经营现金流，默认参数下）
    dcf_fair_price DECIMAL(18,4),      -- 简易DCF公允价

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_valuation_metrics UNIQUE (stock_code, trade_date)
);

-- 支持按股票查询历史估值序列
CREATE INDEX IF NOT EXISTS idx_val_stock ON stock_valuation_metrics(stock_code);
-- 支持按日期批量查询
CREATE INDEX IF NOT EXISTS idx_val_date ON stock_valuation_metrics(trade_date);
-- 支持按股票+日期范围查询
CREATE INDEX IF NOT EXISTS idx_val_stock_date ON stock_valuation_metrics(stock_code, trade_date);

COMMENT ON TABLE stock_valuation_metrics IS '股票估值指标表（阶段C），存储每日PE/PB/PS及历史分位数';
COMMENT ON COLUMN stock_valuation_metrics.pe_ttm IS '滚动市盈率 TTM';
COMMENT ON COLUMN stock_valuation_metrics.pe_lyr IS '静态市盈率 LYR（最近年报）';
COMMENT ON COLUMN stock_valuation_metrics.pb IS '市净率';
COMMENT ON COLUMN stock_valuation_metrics.ps_ttm IS '滚动市销率 TTM';
COMMENT ON COLUMN stock_valuation_metrics.pe_ttm_percentile IS 'PE_TTM 近5年历史分位数 0~1';
COMMENT ON COLUMN stock_valuation_metrics.pb_percentile IS 'PB 近5年历史分位数 0~1';
COMMENT ON COLUMN stock_valuation_metrics.ps_ttm_percentile IS 'PS_TTM 近5年历史分位数 0~1';
COMMENT ON COLUMN stock_valuation_metrics.dcf_fair_price IS '简易DCF默认参数下的公允价';
