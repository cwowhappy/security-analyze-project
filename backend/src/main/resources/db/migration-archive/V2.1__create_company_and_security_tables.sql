-- ============================================================
-- V2: 公司-证券数据模型拆分
-- 将单表 company 拆分为 company（公司法人实体）+ company_security（上市证券）
-- ============================================================

-- 1. 将旧表重命名为 company_legacy，保留完整数据用于回滚
ALTER TABLE IF EXISTS company RENAME TO company_legacy;
ALTER INDEX IF EXISTS idx_stock_code RENAME TO idx_legacy_stock_code;
ALTER INDEX IF EXISTS idx_stock_name RENAME TO idx_legacy_stock_name;
ALTER INDEX IF EXISTS idx_market RENAME TO idx_legacy_market;

-- 2. 创建 company 表（公司法人实体）
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    unified_code VARCHAR(50) UNIQUE,              -- 统一社会信用代码（预留，初期可为空）
    company_name VARCHAR(200) NOT NULL,           -- 公司全称
    short_name VARCHAR(100),                      -- 公司简称
    industry VARCHAR(100),                        -- 所属行业
    region VARCHAR(50),                           -- 地区（省份/直辖市）
    establish_date DATE,                          -- 成立日期
    registered_capital DECIMAL(20,4),             -- 注册资本（万元）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 创建 company_security 表（上市证券）
CREATE TABLE IF NOT EXISTS company_security (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL UNIQUE,       -- 股票代码，全局唯一
    stock_name VARCHAR(100) NOT NULL,             -- 证券简称
    market VARCHAR(10),                           -- 市场板块：SH / SZ / BJ / HK
    security_type VARCHAR(20),                    -- 证券类型：A股 / B股 / H股 / 优先股 / ADR
    listing_date DATE,                            -- 在该市场的上市日期
    listing_status VARCHAR(20) DEFAULT 'listed',  -- 上市状态：listed / suspended / delisted
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 索引
CREATE INDEX idx_company_unified_code ON company(unified_code);
CREATE INDEX idx_company_name ON company(company_name);
CREATE INDEX idx_company_industry ON company(industry);
CREATE INDEX idx_company_region ON company(region);

CREATE INDEX idx_cs_stock_code ON company_security(stock_code);
CREATE INDEX idx_cs_company_id ON company_security(company_id);
CREATE INDEX idx_cs_market ON company_security(market);
CREATE INDEX idx_cs_security_type ON company_security(security_type);
CREATE INDEX idx_cs_listing_status ON company_security(listing_status);

-- 5. 数据迁移：将旧表数据一对一拆分到新两张表
-- 说明：旧表中没有公司统一标识，因此每条旧记录生成一个独立的 company 记录。
--       后续采集模块重写后，将通过 company_name 精确匹配将同一公司的多证券归并到同一 company 下。
DO $$
DECLARE
    old_record RECORD;
    new_company_id BIGINT;
BEGIN
    FOR old_record IN
        SELECT id, stock_code, stock_name, industry, region,
               establish_date, registered_capital, listing_date, market,
               created_at, updated_at
        FROM company_legacy
        ORDER BY id
    LOOP
        -- 插入 company（公司法人实体）
        INSERT INTO company (
            company_name, short_name, industry, region,
            establish_date, registered_capital, created_at, updated_at
        ) VALUES (
            old_record.stock_name,           -- company_name 暂用 stock_name（旧数据无全称）
            old_record.stock_name,           -- short_name
            old_record.industry,
            old_record.region,
            old_record.establish_date,
            old_record.registered_capital,
            old_record.created_at,
            old_record.updated_at
        )
        RETURNING id INTO new_company_id;

        -- 插入 company_security（上市证券）
        INSERT INTO company_security (
            company_id, stock_code, stock_name, market,
            listing_date, created_at, updated_at
        ) VALUES (
            new_company_id,
            old_record.stock_code,
            old_record.stock_name,
            old_record.market,
            old_record.listing_date,
            old_record.created_at,
            old_record.updated_at
        );
    END LOOP;
END $$;
