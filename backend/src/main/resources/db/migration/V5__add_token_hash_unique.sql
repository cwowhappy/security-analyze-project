-- ============================================
-- V5__add_token_hash_unique.sql
-- 为 tb_user_session 的 token_hash 添加唯一约束
-- ============================================

ALTER TABLE tb_user_session
    ADD CONSTRAINT uk_tb_user_session_token_hash UNIQUE (token_hash);
