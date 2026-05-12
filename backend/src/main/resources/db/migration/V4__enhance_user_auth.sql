-- ============================================
-- V4__enhance_user_auth.sql
-- 增强用户认证功能：邮箱验证、登录锁定、Token 黑名单
-- ============================================

-- 1. tb_user 表扩展字段
ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false;

-- 2. 邮箱验证码表
CREATE TABLE IF NOT EXISTS tb_email_verification (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    verification_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_tb_email_verification_user_id ON tb_email_verification(user_id);
CREATE INDEX IF NOT EXISTS idx_tb_email_verification_code ON tb_email_verification(verification_code);

-- 3. 确保 tb_user_session 表存在（如 V3 未创建则补充）
CREATE TABLE IF NOT EXISTS tb_user_session (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_user_session_user_id ON tb_user_session(user_id);
CREATE INDEX IF NOT EXISTS idx_tb_user_session_token_hash ON tb_user_session(token_hash);
