-- ============================================================
-- V6: 增加采集任务 Session 故障恢复支持
-- 1. 扩展 collector_task_log 增加 session_id 与 params
-- 2. 新建 collector_task_progress 记录逐只股票处理进度
-- ============================================================

-- 扩展任务日志表：增加 Session 标识与参数快照
ALTER TABLE collector_task_log
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(36) UNIQUE,
    ADD COLUMN IF NOT EXISTS params JSONB;

CREATE INDEX IF NOT EXISTS idx_task_log_session_id ON collector_task_log(session_id);

-- 新建任务进度表：按 session + stock_code 记录处理状态
CREATE TABLE IF NOT EXISTS collector_task_progress (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    rows_created INT DEFAULT 0,
    rows_updated INT DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_progress_session_stock UNIQUE (session_id, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_task_progress_session ON collector_task_progress(session_id);
CREATE INDEX IF NOT EXISTS idx_task_progress_status ON collector_task_progress(status);
