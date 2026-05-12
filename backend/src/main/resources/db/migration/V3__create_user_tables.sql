-- ============================================
-- V3__create_user_tables.sql
-- 创建用户表与会话表，支持登录注册功能
-- ============================================

CREATE TABLE IF NOT EXISTS tb_user (
    id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'viewer',
    avatar_initial CHAR(1),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_user_username ON tb_user(username);
CREATE INDEX IF NOT EXISTS idx_tb_user_email ON tb_user(email);

CREATE TABLE IF NOT EXISTS tb_user_session (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_user_session_user_id ON tb_user_session(user_id);
CREATE INDEX IF NOT EXISTS idx_tb_user_session_token_hash ON tb_user_session(token_hash);
