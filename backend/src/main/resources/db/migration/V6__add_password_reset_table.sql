-- ============================================
-- V6__add_password_reset_table.sql
-- 密码重置令牌表
-- ============================================

CREATE TABLE IF NOT EXISTS tb_password_reset (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    reset_token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_password_reset_token ON tb_password_reset(reset_token);
CREATE INDEX IF NOT EXISTS idx_tb_password_reset_user_id ON tb_password_reset(user_id);
