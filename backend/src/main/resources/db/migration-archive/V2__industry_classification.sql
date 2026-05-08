-- 行业分类标准字典
CREATE TABLE IF NOT EXISTS industry_classification_standard (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(50)  NOT NULL,
    level_count INT          NOT NULL,
    description VARCHAR(200)
);

INSERT INTO industry_classification_standard (code, name, level_count, description) VALUES
('SW', '申万行业分类', 2, '申万宏源研究所发布的行业分类标准，含一级31个、二级124个'),
('EM', '东方财富行业分类', 1, '东方财富网行业板块分类，以二级为主');

-- 行业分类维度表
CREATE TABLE IF NOT EXISTS industry_category (
    id              BIGSERIAL PRIMARY KEY,
    standard_code   VARCHAR(20)  NOT NULL REFERENCES industry_classification_standard(code),
    level           INT          NOT NULL CHECK (level IN (1, 2)),
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    parent_code     VARCHAR(20),
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (standard_code, level, code)
);

CREATE INDEX idx_industry_category_standard ON industry_category(standard_code);
CREATE INDEX idx_industry_category_standard_level ON industry_category(standard_code, level);
CREATE INDEX idx_industry_category_parent ON industry_category(standard_code, parent_code);

-- 公司与行业分类映射表（支持一对多）
CREATE TABLE IF NOT EXISTS company_industry_mapping (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    standard_code   VARCHAR(20)  NOT NULL REFERENCES industry_classification_standard(code),
    level1_code     VARCHAR(20)  NOT NULL,
    level2_code     VARCHAR(20),
    is_primary      BOOLEAN      DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, standard_code, level2_code)
);

CREATE INDEX idx_cim_company ON company_industry_mapping(company_id);
CREATE INDEX idx_cim_standard_l1 ON company_industry_mapping(standard_code, level1_code);
CREATE INDEX idx_cim_standard_l2 ON company_industry_mapping(standard_code, level2_code);
CREATE INDEX idx_cim_company_standard ON company_industry_mapping(company_id, standard_code);
