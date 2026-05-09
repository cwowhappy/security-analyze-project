-- ============================================================
-- V2: 为核心业务表添加逻辑删除字段
-- ============================================================

-- company 表
ALTER TABLE company
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_company_is_deleted ON company(is_deleted);

-- company_security 表
ALTER TABLE company_security
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_cs_is_deleted ON company_security(is_deleted);

-- financial_report 表
ALTER TABLE financial_report
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_fin_report_is_deleted ON financial_report(is_deleted);

-- 补充：financial_report 股票代码+报告类型+报告日期复合索引（基本面分析常用）
CREATE INDEX IF NOT EXISTS idx_fin_report_stock_type_date
    ON financial_report(stock_code, report_type, report_date);
