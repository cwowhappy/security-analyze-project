CREATE TABLE IF NOT EXISTS collector_task_log (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status VARCHAR(16) NOT NULL,
    rows_affected INT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_log_started_at ON collector_task_log(started_at);
CREATE INDEX idx_task_log_task_type ON collector_task_log(task_type);
CREATE INDEX idx_task_log_status ON collector_task_log(status);

CREATE TABLE IF NOT EXISTS collector_data_status (
    id BIGSERIAL PRIMARY KEY,
    data_type VARCHAR(32) NOT NULL UNIQUE,
    total_rows INT NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP,
    last_task_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_data_status_type ON collector_data_status(data_type);
