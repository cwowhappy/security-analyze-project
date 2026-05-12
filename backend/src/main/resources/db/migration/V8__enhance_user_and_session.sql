-- ============================================
-- V8__enhance_user_and_session.sql
-- 扩展用户表和会话表，支持密码过期与会话管理
-- ============================================

-- 1. 用户表增加密码过期字段
ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS password_expired_at TIMESTAMP;

-- 2. 会话表增加 IP 和 User-Agent 字段
ALTER TABLE tb_user_session
    ADD COLUMN IF NOT EXISTS ip VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);
