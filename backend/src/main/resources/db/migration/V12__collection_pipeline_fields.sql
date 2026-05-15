-- 扩展 tb_collection_task 表
ALTER TABLE tb_collection_task
    ADD COLUMN IF NOT EXISTS mode VARCHAR(20) DEFAULT 'full',
    ADD COLUMN IF NOT EXISTS source_priority JSONB;

-- 创建 stock 级采集状态表
CREATE TABLE IF NOT EXISTS tb_collection_stock_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id VARCHAR(32) REFERENCES tb_collection_task(id),
    stock_code VARCHAR(20) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'success', 'failed', 'skipped')),
    error_message TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(task_id, stock_code, task_type)
);

CREATE INDEX IF NOT EXISTS idx_collection_stock_state_lookup
    ON tb_collection_stock_state(task_id, stock_code, task_type);

CREATE INDEX IF NOT EXISTS idx_collection_stock_state_updated
    ON tb_collection_stock_state(updated_at);
