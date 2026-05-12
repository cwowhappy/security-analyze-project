-- ============================================
-- V2__simplify_stock_company_model.sql
-- 简化股票与公司数据模型 v2.0
-- 变更：
--   1. tb_stock_basic 增加 company_id 外键
--   2. 删除 tb_relation_stock_company 关联表
--   3. 删除 tb_collection_task_schedule 定时规则表
--   4. tb_collection_task 去除 scheduled_at，增加 created_at 索引
-- ============================================

-- 1. 股票表增加 company_id 外键
ALTER TABLE tb_stock_basic
    ADD COLUMN IF NOT EXISTS company_id VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_tb_stock_basic_company_id ON tb_stock_basic(company_id);

-- 2. 删除关联表（数据已废弃）
DROP TABLE IF EXISTS tb_relation_stock_company;

-- 3. 删除定时规则表
DROP TABLE IF EXISTS tb_collection_task_schedule;

-- 4. 采集任务表去除定时字段并优化索引
ALTER TABLE tb_collection_task DROP COLUMN IF EXISTS scheduled_at;
DROP INDEX IF EXISTS idx_tb_collection_task_scheduled_at;
CREATE INDEX IF NOT EXISTS idx_tb_collection_task_created_at ON tb_collection_task(created_at);
