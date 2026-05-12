-- ============================================
-- V7__add_login_log_table.sql
-- 登录日志表
-- ============================================

CREATE TABLE IF NOT EXISTS tb_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) REFERENCES tb_user(id) ON DELETE SET NULL,
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    details VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_login_log_user_id ON tb_login_log(user_id);
CREATE INDEX IF NOT EXISTS idx_tb_login_log_created_at ON tb_login_log(created_at);
CREATE INDEX IF NOT EXISTS idx_tb_login_log_action ON tb_login_log(action);
