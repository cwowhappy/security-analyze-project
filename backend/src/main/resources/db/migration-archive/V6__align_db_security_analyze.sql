-- ============================================================
-- V6 Align: 对齐 db-security-analyze 测试库 schema
-- 将历史遗留偏差修复为与 v1.0.0 标准一致
-- ============================================================

-- 1. 删除历史遗留表 company_legacy（无数据、无外键引用）
DROP TABLE IF EXISTS company_legacy CASCADE;

-- 2. 修复 industry_classification_standard.id 类型（int4 → int8）
ALTER SEQUENCE industry_classification_standard_id_seq AS bigint;
ALTER TABLE industry_classification_standard ALTER COLUMN id TYPE bigint;

-- 3. 修复 industry_category 时间戳字段（nullable + now() → NOT NULL + CURRENT_TIMESTAMP）
ALTER TABLE industry_category ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE industry_category ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE industry_category ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE industry_category ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- 4. 修复 company_industry_mapping.created_at（nullable + now() → NOT NULL + CURRENT_TIMESTAMP）
ALTER TABLE company_industry_mapping ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE company_industry_mapping ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- 5. 删除测试库中冗余的重复索引（已有 UNIQUE 约束自动索引）
DROP INDEX IF EXISTS idx_etf_code;
DROP INDEX IF EXISTS idx_index_code;

-- 6. 补全表注释（与 v1.0.0 标准一致）
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

-- 7. 修复 company 表历史重建导致的约束/序列名后缀
ALTER SEQUENCE company_id_seq1 RENAME TO company_id_seq;
ALTER INDEX company_pkey1 RENAME TO company_pkey;
ALTER TABLE company RENAME CONSTRAINT company_created_at_not_null1 TO company_created_at_not_null;
ALTER TABLE company RENAME CONSTRAINT company_id_not_null1 TO company_id_not_null;
ALTER TABLE company RENAME CONSTRAINT company_updated_at_not_null1 TO company_updated_at_not_null;
