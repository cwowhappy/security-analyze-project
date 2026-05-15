-- ============================================
-- V11__create_financial_report_analysis_table.sql
-- 创建 AI 财报解读报告表
-- ============================================

CREATE TABLE IF NOT EXISTS tb_financial_report_analysis (
    id                      VARCHAR(32) PRIMARY KEY,
    stock_code              VARCHAR(10)  NOT NULL,
    stock_name              VARCHAR(100),
    report_date             DATE         NOT NULL,
    report_type             VARCHAR(10)  NOT NULL DEFAULT 'Y',

    -- AI 评分（1-10 分）
    score_profitability     DECIMAL(4, 2),
    score_growth            DECIMAL(4, 2),
    score_cashflow          DECIMAL(4, 2),
    score_financial_health  DECIMAL(4, 2),
    score_overall           DECIMAL(4, 2),

    -- 报告内容
    report_content          TEXT,
    summary                 TEXT,
    risk_signals            JSONB,

    -- AI 模型信息
    ai_model                VARCHAR(50),
    ai_tokens_used          INTEGER,

    -- 处理状态
    status                  VARCHAR(20)  DEFAULT 'PENDING',

    updated_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tb_financial_report_analysis UNIQUE (stock_code, report_date, report_type)
);

COMMENT ON TABLE tb_financial_report_analysis IS 'AI 财报解读报告表：存储大模型生成的财报分析报告';

CREATE INDEX IF NOT EXISTS idx_tb_financial_report_analysis_stock_date ON tb_financial_report_analysis(stock_code, report_date DESC);
CREATE INDEX IF NOT EXISTS idx_tb_financial_report_analysis_status ON tb_financial_report_analysis(status);
