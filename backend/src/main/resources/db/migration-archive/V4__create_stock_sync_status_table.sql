-- ============================================================
-- V4: 创建财务报告采集同步状态表
-- 记录每家上市公司已采集到的最新报告期，支持增量更新
-- ============================================================

CREATE TABLE IF NOT EXISTS collector_stock_sync_status (
    stock_code VARCHAR(20) PRIMARY KEY,
    latest_report_date DATE,              -- 该股票已采集的最新报告期
    report_count INTEGER DEFAULT 0,       -- 累计采集报告数
    last_sync_at TIMESTAMP,               -- 最后同步时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_csss_last_sync ON collector_stock_sync_status(last_sync_at);
