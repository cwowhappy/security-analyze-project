-- ============================================================
-- V4: 扩展 company_security 表，新增股本与市值字段
-- 为阶段C估值分析（PE/PB/PS计算）提供总股本数据
-- ============================================================

ALTER TABLE company_security
    ADD COLUMN IF NOT EXISTS total_shares DECIMAL(20,4),        -- 总股本（股）
    ADD COLUMN IF NOT EXISTS circulating_shares DECIMAL(20,4),  -- 流通股本（股）
    ADD COLUMN IF NOT EXISTS market_cap DECIMAL(20,4);          -- 总市值（元），可选缓存

-- 新增索引支持按市值范围筛选
CREATE INDEX IF NOT EXISTS idx_cs_total_shares ON company_security(total_shares);

COMMENT ON COLUMN company_security.total_shares IS '总股本（股），用于计算市值和估值指标';
COMMENT ON COLUMN company_security.circulating_shares IS '流通股本（股）';
COMMENT ON COLUMN company_security.market_cap IS '总市值（元），可由股价×总股本实时计算，也可缓存';
