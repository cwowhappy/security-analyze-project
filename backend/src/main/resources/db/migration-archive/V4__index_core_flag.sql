-- ============================================================
-- V4 Index Core Flag: 为核心指数增加标记字段
-- ============================================================

ALTER TABLE index_info ADD COLUMN IF NOT EXISTS is_core BOOLEAN DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_index_core ON index_info(is_core, index_type);

-- 初始化核心指数标记（宽基）
UPDATE index_info SET is_core = TRUE WHERE index_code IN (
    '000001',  -- 上证指数
    '399001',  -- 深证成指
    '399006',  -- 创业板指
    '000300',  -- 沪深300
    '000016',  -- 上证50
    '000905',  -- 中证500
    '000852',  -- 中证1000
    '000688',  -- 科创50
    '399005',  -- 中小板指
    '399673'   -- 创业板50
);

-- 初始化核心指数标记（行业）
UPDATE index_info SET is_core = TRUE WHERE index_code IN (
    '399989',  -- 中证医疗
    '399997',  -- 中证白酒
    '399998',  -- 中证煤炭
    '399995',  -- 中证基建
    '399991',  -- 中证一带一路
    '399993',  -- 中证生物科技
    '399996',  -- 中证智能家居
    '399994'   -- 中证信息安全
);

-- 初始化核心指数标记（主题）
UPDATE index_info SET is_core = TRUE WHERE index_code IN (
    '000021',  -- 180治理
    '000022',  -- 上证公司债
    '000042',  -- 上证央企
    '000043',  -- 上证超大盘
    '000044'   -- 上证中盘
);
