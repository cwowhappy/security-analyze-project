# 财务报告分析模块 · 功能详细设计

> 版本：v1.0 | 日期：2026-05-12 | 作者：基于《个人投研系统·产品功能文档》设计

---

## 一、模块定位与目标

### 1.1 模块定位

财务报告分析模块是**个人投研系统的核心分析引擎**，连接数据采集层与 AI 研究层，向上承载投资决策所需的基本面数据，向下驱动 AI 财报解读与公司研究 Agent。

```
┌─────────────────────────────────────────────────────────────────────┐
│                        个人投研系统架构                              │
│                                                                     │
│  Layer 1: 信息采集  →  Layer 2: 数据中台  →  Layer 3: AI 研究引擎  │
│                              ↑                   ↑                 │
│                       ┌──────────────────────────────┐              │
│                       │   财务报告分析模块（本模块）   │              │
│                       │   1. 财报数据展示            │              │
│                       │   2. 财务指标计算            │              │
│                       │   3. 财务趋势分析            │              │
│                       │   4. AI 财报解读             │              │
│                       └──────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 模块目标

| 目标 | 衡量标准 | 优先级 |
|------|---------|--------|
| **财报数据完整展示** | 覆盖 A 股全市场财务三表（利润表/资产负债表/现金流量表）| P0 |
| **核心指标自动化计算** | ROE/ROA/毛利率/净利率/营收增速等 30+ 指标 | P0 |
| **历史趋势可视化** | 支持近 8-12 期财务数据趋势图表 | P1 |
| **AI 财报智能解读** | 财报发布后 24 小时内自动生成解读摘要 | P0 |
| **同业横向对比** | 同行业公司关键财务指标对比分析 | P1 |

---

## 二、功能架构总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                     财务报告分析模块功能架构                          │
│                                                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐ │
│  │  财报数据    │  │  财务指标    │  │  趋势分析    │  │  AI 解读   │ │
│  │  展示子模块  │  │  计算子模块  │  │  子模块      │  │  子模块    │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘ │
│         │                │                │                │         │
│  ┌──────┴────────────────┴────────────────┴────────────────┴───────┐ │
│  │                    共享数据层（PostgreSQL）                      │ │
│  │  tb_financial_income / tb_financial_balance / tb_financial_cash │ │
│  │  tb_financial_indicator / tb_company_basic / tb_stock_basic     │ │
│  └─────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 三、子模块详细设计

### 3.1 财报数据展示子模块

#### 3.1.1 财务三表数据模型

**利润表（Income Statement）**

| 字段名 | 中文名 | 数据类型 | 说明 |
|--------|--------|----------|------|
| `id` | 主键 | BIGINT | 自增主键 |
| `stock_code` | 股票代码 | VARCHAR(10) | 如 `000001` |
| `report_date` | 报告期 | DATE | 如 `2024-12-31` |
| `report_type` | 报告类型 | VARCHAR(10) | Q1/Q2(Q1)/Q3(累计)/Q4(累计)/Y |
| `basic_eps` | 基本每股收益 | DECIMAL(18,4) | 单位：元 |
| `diluted_eps` | 稀释每股收益 | DECIMAL(18,4) | 单位：元 |
| `total_revenue` | 营业总收入 | DECIMAL(18,2) | 单位：元 |
| `revenue` | 营业收入 | DECIMAL(18,2) | 单位：元 |
| `operating_cost` | 营业成本 | DECIMAL(18,2) | 单位：元 |
| `gross_profit` | 毛利 | DECIMAL(18,2) | 营业收入 - 营业成本 |
| `selling_expense` | 销售费用 | DECIMAL(18,2) | 单位：元 |
| `admin_expense` | 管理费用 | DECIMAL(18,2) | 单位：元 |
| `rd_expense` | 研发费用 | DECIMAL(18,2) | 单位：元 |
| `financial_expense` | 财务费用 | DECIMAL(18,2) | 单位：元 |
| `operating_profit` | 营业利润 | DECIMAL(18,2) | 单位：元 |
| `total_profit` | 利润总额 | DECIMAL(18,2) | 单位：元 |
| `net_profit` | 净利润 | DECIMAL(18,2) | 单位：元 |
| `np_parent_company` | 归母净利润 | DECIMAL(18,2) | 单位：元 |
| `np_excl_nonrecurring` | 扣非净利润 | DECIMAL(18,2) | 单位：元 |
| `created_at` | 创建时间 | TIMESTAMP | |
| `updated_at` | 更新时间 | TIMESTAMP | |

**资产负债表（Balance Sheet）**

| 字段名 | 中文名 | 数据类型 | 说明 |
|--------|--------|----------|------|
| `id` | 主键 | BIGINT | 自增主键 |
| `stock_code` | 股票代码 | VARCHAR(10) | 如 `000001` |
| `report_date` | 报告期 | DATE | 如 `2024-12-31` |
| `report_type` | 报告类型 | VARCHAR(10) | Q1/Q2(Q1)/Q3(累计)/Q4(累计)/Y |
| `total_assets` | 总资产 | DECIMAL(18,2) | 单位：元 |
| `total_liabilities` | 总负债 | DECIMAL(18,2) | 单位：元 |
| `total_equity` | 股东权益 | DECIMAL(18,2) | 单位：元 |
| `equity_parent_company` | 归母股东权益 | DECIMAL(18,2) | 单位：元 |
| `current_assets` | 流动资产 | DECIMAL(18,2) | 单位：元 |
| `non_current_assets` | 非流动资产 | DECIMAL(18,2) | 单位：元 |
| `cash_equivalents` | 货币资金 | DECIMAL(18,2) | 单位：元 |
| `accounts_receivable` | 应收账款 | DECIMAL(18,2) | 单位：元 |
| `inventories` | 存货 | DECIMAL(18,2) | 单位：元 |
| `current_liabilities` | 流动负债 | DECIMAL(18,2) | 单位：元 |
| `non_current_liabilities` | 非流动负债 | DECIMAL(18,2) | 单位：元 |
| `accounts_payable` | 应付账款 | DECIMAL(18,2) | 单位：元 |
| `short_term_borrowings` | 短期借款 | DECIMAL(18,2) | 单位：元 |
| `long_term_borrowings` | 长期借款 | DECIMAL(18,2) | 单位：元 |
| `goodwill` | 商誉 | DECIMAL(18,2) | 单位：元 |
| `created_at` | 创建时间 | TIMESTAMP | |
| `updated_at` | 更新时间 | TIMESTAMP | |

**现金流量表（Cash Flow Statement）**

| 字段名 | 中文名 | 数据类型 | 说明 |
|--------|--------|----------|------|
| `id` | 主键 | BIGINT | 自增主键 |
| `stock_code` | 股票代码 | VARCHAR(10) | 如 `000001` |
| `report_date` | 报告期 | DATE | 如 `2024-12-31` |
| `report_type` | 报告类型 | VARCHAR(10) | Q1/Q2(Q1)/Q3(累计)/Q4(累计)/Y |
| `cf_operating` | 经营活动现金流 | DECIMAL(18,2) | 单位：元 |
| `cf_investing` | 投资活动现金流 | DECIMAL(18,2) | 单位：元 |
| `cf_financing` | 筹资活动现金流 | DECIMAL(18,2) | 单位：元 |
| `net_cash_flow` | 净现金流 | DECIMAL(18,2) | 三项合计 |
| `free_cash_flow` | 自由现金流 | DECIMAL(18,2) | 经营活动现金流 - 资本开支 |
| `capex` | 资本开支 | DECIMAL(18,2) | 购建固定资产等支出 |
| `cash_received_operating` | 销售商品提供劳务收到的现金 | DECIMAL(18,2) | 单位：元 |
| `tax_paid` | 支付的各项税费 | DECIMAL(18,2) | 单位：元 |
| `created_at` | 创建时间 | TIMESTAMP | |
| `updated_at` | 更新时间 | TIMESTAMP | |

#### 3.1.2 功能清单

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 财务三表查看 | 利润表/资产负债表/现金流量表完整展示 | P0 |
| 报告期筛选 | 按年度/季度筛选，支持跨年对比 | P0 |
| 报告类型说明 | 标注季报/半年报/年报，季报区分累计与单季 | P1 |
| 数值单位切换 | 元/万元/亿元单位切换展示 | P1 |
| 数据来源标注 | 标注数据来源（Tushare/AKShare）| P2 |

#### 3.1.3 数据库表 DDL

```sql
-- 利润表
CREATE TABLE tb_financial_income (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL DEFAULT 'Y',  -- Y, Q1, Q2, Q3, Q4
    basic_eps DECIMAL(18, 4),
    diluted_eps DECIMAL(18, 4),
    total_revenue DECIMAL(18, 2),
    revenue DECIMAL(18, 2),
    operating_cost DECIMAL(18, 2),
    gross_profit DECIMAL(18, 2),
    selling_expense DECIMAL(18, 2),
    admin_expense DECIMAL(18, 2),
    rd_expense DECIMAL(18, 2),
    financial_expense DECIMAL(18, 2),
    operating_profit DECIMAL(18, 2),
    total_profit DECIMAL(18, 2),
    net_profit DECIMAL(18, 2),
    np_parent_company DECIMAL(18, 2),
    np_excl_nonrecurring DECIMAL(18, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stock_code, report_date, report_type)
);

CREATE INDEX idx_income_stock_date ON tb_financial_income(stock_code, report_date DESC);

-- 资产负债表
CREATE TABLE tb_financial_balance (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL DEFAULT 'Y',
    total_assets DECIMAL(18, 2),
    total_liabilities DECIMAL(18, 2),
    total_equity DECIMAL(18, 2),
    equity_parent_company DECIMAL(18, 2),
    current_assets DECIMAL(18, 2),
    non_current_assets DECIMAL(18, 2),
    cash_equivalents DECIMAL(18, 2),
    accounts_receivable DECIMAL(18, 2),
    inventories DECIMAL(18, 2),
    current_liabilities DECIMAL(18, 2),
    non_current_liabilities DECIMAL(18, 2),
    accounts_payable DECIMAL(18, 2),
    short_term_borrowings DECIMAL(18, 2),
    long_term_borrowings DECIMAL(18, 2),
    goodwill DECIMAL(18, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stock_code, report_date, report_type)
);

CREATE INDEX idx_balance_stock_date ON tb_financial_balance(stock_code, report_date DESC);

-- 现金流量表
CREATE TABLE tb_financial_cashflow (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL DEFAULT 'Y',
    cf_operating DECIMAL(18, 2),
    cf_investing DECIMAL(18, 2),
    cf_financing DECIMAL(18, 2),
    net_cash_flow DECIMAL(18, 2),
    free_cash_flow DECIMAL(18, 2),
    capex DECIMAL(18, 2),
    cash_received_operating DECIMAL(18, 2),
    tax_paid DECIMAL(18, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stock_code, report_date, report_type)
);

CREATE INDEX idx_cashflow_stock_date ON tb_financial_cashflow(stock_code, report_date DESC);
```

---

### 3.2 财务指标计算子模块

#### 3.2.1 指标体系

**盈利能力指标**

| 指标名 | 代码 | 计算公式 | 说明 |
|--------|------|---------|------|
| 净资产收益率 | ROE | 归母净利润 ÷ 平均归母股东权益 × 100% | 核心指标，P0 |
| 总资产收益率 | ROA | 净利润 ÷ 平均总资产 × 100% | 资产运营效率 |
| 投入资本回报率 | ROIC | EBIT×(1-税率) ÷ (股东权益+有息负债) | 实际投入资本回报 |
| 毛利率 | GrossMargin | (营收 - 营业成本) ÷ 营收 × 100% | 主营业务盈利空间 |
| 净利率 | NetMargin | 净利润 ÷ 营收 × 100% | 最终盈利能力 |
| 扣非净利率 | NetMarginExcl | 扣非净利润 ÷ 营收 × 100% | 剔除非经常损益 |

**偿债能力指标**

| 指标名 | 代码 | 计算公式 | 说明 |
|--------|------|---------|------|
| 资产负债率 | DebtRatio | 总负债 ÷ 总资产 × 100% | < 60% 通常较健康 |
| 流动比率 | CurrentRatio | 流动资产 ÷ 流动负债 | > 1.5 较安全 |
| 速动比率 | QuickRatio | (流动资产 - 存货) ÷ 流动负债 | 更严格的流动性指标 |
| 净负债率 | NetDebtRatio | (有息负债 - 现金) ÷ 净资产 × 100% | 净负债水平 |

**运营效率指标**

| 指标名 | 代码 | 计算公式 | 说明 |
|--------|------|---------|------|
| 应收账款周转天数 | DSO | 平均应收账款 ÷ 营收 × 360 天 | 回款速度，越小越好 |
| 存货周转天数 | DIO | 平均存货 ÷ 营业成本 × 360 天 | 存货消化速度 |
| 应付账款周转天数 | DPO | 平均应付账款 ÷ 营业成本 × 360 天 | 对供应商占款能力 |
| 现金转换周期 | CCC | DSO + DIO - DPO | 营运资本效率 |

**成长性指标**

| 指标名 | 代码 | 计算公式 | 说明 |
|--------|------|---------|------|
| 营收增速 | RevenueGrowth | (本期营收 - 上期营收) ÷ 上期营收 × 100% | YoY |
| 归母净利润增速 | NPParentGrowth | (本期归母净利润 - 上期) ÷ 上期 × 100% | YoY |
| 扣非净利润增速 | NPExclGrowth | (本期扣非净利润 - 上期) ÷ 上期 × 100% | 剔非后成长性 |
| 经营现金流增速 | CFOGrowth | (本期经营现金流 - 上期) ÷ 上期 × 100% | 现金流验证利润质量 |

**估值指标**

| 指标名 | 代码 | 计算公式 | 说明 |
|--------|------|---------|------|
| 市盈率 | PE | 市值 ÷ 归母净利润（TTM）| P0 |
| 市净率 | PB | 市值 ÷ 净资产 | P0 |
| 市销率 | PS | 市值 ÷ 营收（TTM）| 亏损/早期成长股 |
| 股息率 | DividendYield | 股息 ÷ 股价 × 100% | 高股息策略 |

#### 3.2.2 财务指标表

```sql
-- 财务指标表（计算结果存储）
CREATE TABLE tb_financial_indicator (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL,
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL DEFAULT 'Y',

    -- 盈利能力
    roe DECIMAL(10, 4),           -- 净资产收益率 %
    roa DECIMAL(10, 4),           -- 总资产收益率 %
    roic DECIMAL(10, 4),          -- 投入资本回报率 %
    gross_margin DECIMAL(10, 4),  -- 毛利率 %
    net_margin DECIMAL(10, 4),    -- 净利率 %
    net_margin_excl DECIMAL(10, 4), -- 扣非净利率 %

    -- 偿债能力
    debt_ratio DECIMAL(10, 4),   -- 资产负债率 %
    current_ratio DECIMAL(10, 4), -- 流动比率
    quick_ratio DECIMAL(10, 4),   -- 速动比率
    net_debt_ratio DECIMAL(10, 4),-- 净负债率 %

    -- 运营效率
    dso DECIMAL(10, 2),           -- 应收账款周转天数
    dio DECIMAL(10, 2),           -- 存货周转天数
    dpo DECIMAL(10, 2),           -- 应付账款周转天数
    ccc DECIMAL(10, 2),           -- 现金转换周期
    asset_turnover DECIMAL(10, 4),-- 总资产周转率

    -- 成长性（YoY 增速 %）
    revenue_growth DECIMAL(10, 4),
    np_parent_growth DECIMAL(10, 4),
    np_excl_growth DECIMAL(10, 4),
    cfo_growth DECIMAL(10, 4),

    -- 估值（实时计算或从 Tushare 获取）
    pe DECIMAL(18, 4),            -- 市盈率 TTM
    pb DECIMAL(18, 4),            -- 市净率
    ps DECIMAL(18, 4),            -- 市销率 TTM
    dividend_yield DECIMAL(10, 4),-- 股息率 %
    market_cap DECIMAL(18, 2),    -- 市值（元）

    -- 元数据
    data_source VARCHAR(20),      -- 数据来源：TUSHARE/AKSHARE/CALCULATED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stock_code, report_date, report_type)
);

CREATE INDEX idx_indicator_stock_date ON tb_financial_indicator(stock_code, report_date DESC);
```

#### 3.2.3 指标计算服务

```python
# src/financial_analyzer/services/indicator_calculator.py

from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from typing import Optional

@dataclass
class FinancialData:
    """财务数据容器"""
    # 利润表
    revenue: Optional[Decimal]
    gross_profit: Optional[Decimal]
    operating_profit: Optional[Decimal]
    net_profit: Optional[Decimal]
    np_parent_company: Optional[Decimal]
    np_excl_nonrecurring: Optional[Decimal]

    # 资产负债表
    total_assets: Optional[Decimal]
    total_liabilities: Optional[Decimal]
    total_equity: Optional[Decimal]
    equity_parent_company: Optional[Decimal]
    current_assets: Optional[Decimal]
    current_liabilities: Optional[Decimal]
    cash_equivalents: Optional[Decimal]
    accounts_receivable: Optional[Decimal]
    inventories: Optional[Decimal]
    accounts_payable: Optional[Decimal]
    short_term_borrowings: Optional[Decimal]
    long_term_borrowings: Optional[Decimal]

    # 现金流量表
    cf_operating: Optional[Decimal]
    capex: Optional[Decimal]

    # 行情数据
    market_cap: Optional[Decimal]
    share_price: Optional[Decimal]
    total_shares: Optional[Decimal]  # 总股本


class IndicatorCalculator:
    """财务指标计算器"""

    def calculate_profitability(self, current: FinancialData, prev: FinancialData) -> dict:
        """计算盈利能力指标"""
        results = {}

        # ROE = 归母净利润 ÷ 平均归母股东权益
        if current.np_parent_company and current.equity_parent_company:
            avg_equity = (current.equity_parent_company + (prev.equity_parent_company or current.equity_parent_company)) / 2
            if avg_equity and avg_equity > 0:
                results['roe'] = (current.np_parent_company / avg_equity) * 100

        # ROA = 净利润 ÷ 平均总资产
        if current.net_profit and current.total_assets:
            avg_assets = (current.total_assets + (prev.total_assets or current.total_assets)) / 2
            if avg_assets and avg_assets > 0:
                results['roa'] = (current.net_profit / avg_assets) * 100

        # 毛利率 = (营收 - 营业成本) / 营收
        if current.gross_profit and current.revenue and current.revenue > 0:
            results['gross_margin'] = (current.gross_profit / current.revenue) * 100

        # 净利率 = 净利润 / 营收
        if current.net_profit and current.revenue and current.revenue > 0:
            results['net_margin'] = (current.net_profit / current.revenue) * 100

        return results

    def calculate_debt_ratio(self, current: FinancialData) -> dict:
        """计算偿债能力指标"""
        results = {}

        if current.total_liabilities and current.total_assets and current.total_assets > 0:
            results['debt_ratio'] = (current.total_liabilities / current.total_assets) * 100

        if current.current_assets and current.current_liabilities and current.current_liabilities > 0:
            results['current_ratio'] = current.current_assets / current.current_liabilities
            quick_assets = current.current_assets - (current.inventories or 0)
            results['quick_ratio'] = quick_assets / current.current_liabilities

        # 净负债率 = (有息负债 - 现金) / 净资产
        if current.total_liabilities and current.total_equity and current.total_equity > 0:
            interest_bearing_debt = (current.short_term_borrowings or 0) + (current.long_term_borrowings or 0)
            net_debt = interest_bearing_debt - (current.cash_equivalents or 0)
            results['net_debt_ratio'] = (net_debt / current.total_equity) * 100

        return results

    def calculate_operation_efficiency(self, current: FinancialData, prev: FinancialData) -> dict:
        """计算运营效率指标"""
        results = {}
        days = 360  # 年度计算

        # DSO = 平均应收账款 / 营收 * 360
        if current.accounts_receivable and current.revenue:
            avg_ar = (current.accounts_receivable + (prev.accounts_receivable or current.accounts_receivable)) / 2
            if avg_ar > 0 and current.revenue > 0:
                results['dso'] = (avg_ar / current.revenue) * days

        # DIO = 平均存货 / 营业成本 * 360
        if current.inventories and current.gross_profit and current.revenue:
            # 营业成本 = 营收 - 毛利
            operating_cost = current.revenue - current.gross_profit
            avg_inv = (current.inventories + (prev.inventories or current.inventories)) / 2
            if avg_inv > 0 and operating_cost > 0:
                results['dio'] = (avg_inv / operating_cost) * days

        # DPO = 平均应付账款 / 营业成本 * 360
        if current.accounts_payable and current.gross_profit and current.revenue:
            operating_cost = current.revenue - current.gross_profit
            avg_ap = (current.accounts_payable + (prev.accounts_payable or current.accounts_payable)) / 2
            if avg_ap > 0 and operating_cost > 0:
                results['dpo'] = (avg_ap / operating_cost) * days

        # CCC = DSO + DIO - DPO
        if all(k in results for k in ('dso', 'dio', 'dpo')):
            results['ccc'] = results['dso'] + results['dio'] - results['dpo']

        return results

    def calculate_growth(self, current: FinancialData, prev: FinancialData) -> dict:
        """计算成长性指标（YoY）"""
        results = {}

        def calc_growth(curr, prev_key):
            if curr and prev and prev > 0:
                return ((curr - prev) / prev) * 100
            return None

        results['revenue_growth'] = calc_growth(current.revenue, prev.revenue)
        results['np_parent_growth'] = calc_growth(current.np_parent_company, prev.np_parent_company)
        results['np_excl_growth'] = calc_growth(current.np_excl_nonrecurring, prev.np_excl_nonrecurring)
        results['cfo_growth'] = calc_growth(current.cf_operating, prev.cf_operating)

        return {k: v for k, v in results.items() if v is not None}

    def calculate_valuation(self, current: FinancialData) -> dict:
        """计算估值指标"""
        results = {}

        if current.market_cap and current.np_parent_company:
            # PE = 市值 / 归母净利润（需处理负值）
            if current.np_parent_company > 0:
                results['pe'] = current.market_cap / current.np_parent_company

        if current.market_cap and current.total_equity:
            # PB = 市值 / 净资产
            if current.total_equity > 0:
                results['pb'] = current.market_cap / current.total_equity

        if current.market_cap and current.revenue:
            # PS = 市值 / 营收
            if current.revenue > 0:
                results['ps'] = current.market_cap / current.revenue

        results['market_cap'] = current.market_cap

        return {k: v for k, v in results.items() if v is not None}
```

---

### 3.3 趋势分析子模块

#### 3.3.1 功能清单

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 近 8-12 期趋势图 | 核心指标（营收/净利润/ROE/毛利率）历史趋势可视化 | P1 |
| 同比/环比对比表 | 报告期同比（YoY）、季度环比（QoQ）| P0 |
| 杜邦分析 | ROE 三因素分解（净利率 × 资产周转率 × 权益乘数）| P2 |
| 同行业对比 | 与同行业平均/龙头公司关键指标横向对比 | P1 |
| 估值历史分位 | PE/PB 当前值在历史区间的百分位 | P1 |

#### 3.3.2 杜邦分析模型

```
ROE = 净利率 × 资产周转率 × 权益乘数
    = (净利润 / 营收) × (营收 / 总资产) × (总资产 / 净资产)

分解：
- 净利率：反映盈利能力
- 资产周转率：反映运营效率
- 权益乘数：反映财务杠杆
```

#### 3.3.3 前端组件设计

```
┌─────────────────────────────────────────────────────────────────┐
│                    财务分析详情页                                  │
│                                                                 │
│  ┌──────────────────────┐  ┌──────────────────────┐             │
│  │  [平安银行 000001]    │  │  报告期: [2024年报 ▼]│             │
│  └──────────────────────┘  └──────────────────────┘             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  标签页: [基本面分析] [财务报表] [AI 解读]                   ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │                                                             ││
│  │  ┌─────────────────────────────────────────────────────┐   ││
│  │  │         核心指标看板（4x3 网格）                     │   ││
│  │  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐       │   ││
│  │  │  │ ROE    │ │ 毛利率 │ │ 净利率 │ │ 营收增速│       │   ││
│  │  │  │ 12.5%  │ │ 35.2%  │ │ 18.7%  │ │ +16.9% │       │   ││
│  │  │  └────────┘ └────────┘ └────────┘ └────────┘       │   ││
│  │  └─────────────────────────────────────────────────────┘   ││
│  │                                                             ││
│  │  ┌─────────────────────────────────────────────────────┐   ││
│  │  │         营收 & 净利润趋势图（近8期）                 │   ││
│  │  └─────────────────────────────────────────────────────┘   ││
│  │                                                             ││
│  │  ┌─────────────────────────┐ ┌─────────────────────────┐   ││
│  │  │ 杜邦分析                 │ │ 同业对比                 │   ││
│  │  │ ROE = 净利率 × 周转 ×杠杆│ │ 行业均值/中位数/排名     │   ││
│  │  └─────────────────────────┘ └─────────────────────────┘   ││
│  │                                                             ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  [导出报告]  [数据刷新]                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.4 AI 财报解读子模块

#### 3.4.1 功能定位

财报解读 Agent 是**财报发布后自动触发的 AI 分析流程**，核心价值：

1. **第一时间解读**：财报发布后 24 小时内自动分析
2. **核心变化提取**：识别营收/利润/毛利率等关键指标的同比/环比变化
3. **异常预警**：识别大幅波动、利润质量变化等异常信号
4. **同业对比**：与同行业公司对比，判断相对表现

#### 3.4.2 Agent 工作流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                     财报解读 Agent 工作流程                           │
│                                                                     │
│  ┌──────────────┐                                                   │
│  │ 1. 触发条件   │ 财报数据入库 / 用户主动查询                        │
│  └──────┬───────┘                                                   │
│         ↓                                                            │
│  ┌──────────────┐                                                   │
│  │ 2. 数据准备   │ 获取本期 + 上期 + 去年同期三组数据                   │
│  └──────┬───────┘                                                   │
│         ↓                                                            │
│  ┌──────────────┐                                                   │
│  │ 3. AI 分析   │ 调用 DeepSeek 分析                                │
│  │              │ - 核心指标变化解读                                  │
│  │              │ - 盈利能力评估                                      │
│  │              │ - 现金流质量分析                                    │
│  │              │ - 风险信号识别                                      │
│  └──────┬───────┘                                                   │
│         ↓                                                            │
│  ┌──────────────┐                                                   │
│  │ 4. 报告生成   │ 生成结构化解读报告                                  │
│  └──────┬───────┘                                                   │
│         ↓                                                            │
│  ┌──────────────┐                                                   │
│  │ 5. 推送通知   │ 微信/飞书推送摘要 + 链接                            │
│  └──────────────┘                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.4.3 AI Prompt 设计

```python
# src/financial_analyzer/prompts/financial_report_analysis.py

FINANCIAL_REPORT_ANALYSIS_PROMPT = """
## 角色
你是一位专业的A股财务分析师，负责解读上市公司财务报告，识别关键变化和潜在风险。

## 输入信息
### 股票信息
- 股票代码：{stock_code}
- 股票名称：{stock_name}
- 所属行业：{industry}

### 财务数据（单位：万元）

#### 本期数据 ({current_period})
| 指标 | 本期 | 上期 | 去年同期 | 上年同期 |
|------|------|------|----------|----------|
| 营业收入 | {revenue} | {prev_revenue} | {yoy_revenue} | {prev_yoy_revenue} |
| 归母净利润 | {np_parent} | {prev_np_parent} | {yoy_np_parent} | {prev_yoy_np_parent} |
| 扣非净利润 | {np_excl} | {prev_np_excl} | {yoy_np_excl} | {prev_yoy_np_excl} |
| 毛利率 | {gross_margin}% | {prev_gross_margin}% | {yoy_gross_margin}% | {prev_yoy_gross_margin}% |
| 净利率 | {net_margin}% | {prev_net_margin}% | {yoy_net_margin}% | {prev_yoy_net_margin}% |
| ROE | {roe}% | {prev_roe}% | {yoy_roe}% | {prev_yoy_roe}% |
| 经营现金流 | {cfo} | {prev_cfo} | {yoy_cfo} | {prev_yoy_cfo} |
| 资产负债率 | {debt_ratio}% | {prev_debt_ratio}% | - | - |

## 输出要求

请按以下结构生成分析报告：

### 一、本期业绩概览
- 简要总结本期业绩表现
- 指出最重要的变化

### 二、核心指标解读

#### 2.1 盈利能力
- 营收/利润增速分析（同比/环比）
- 毛利率变化及原因推断
- ROE 变化及驱动因素

#### 2.2 成长性评估
- 营收增速趋势判断
- 利润增速与营收增速匹配度（内生增长 vs 外延增长）

#### 2.3 现金流质量
- 经营现金流与净利润对比（利润质量）
- 现金流趋势判断

### 三、风险信号
- 列出需要关注的异常变化或潜在风险
- 每个风险给出简短说明

### 四、同业对比要点
- 与行业平均水平的对比
- 相对优劣势判断

### 五、综合评分
对以下维度给出 1-10 分评分：
1. 盈利能力：X/10
2. 成长性：X/10
3. 现金流质量：X/10
4. 财务健康度：X/10
5. 综合评分：X/10

---
**注意**：
1. 所有分析应基于提供的财务数据
2. 原因推断使用"可能"、"推断"等词汇，不确定时不臆测
3. 风险信号要具体，指出具体指标和变化幅度
"""

def build_financial_report_prompt(stock_info: dict, financial_data: dict) -> str:
    """构建财报分析 prompt"""
    return FINANCIAL_REPORT_ANALYSIS_PROMPT.format(
        stock_code=stock_info['stock_code'],
        stock_name=stock_info['stock_name'],
        industry=stock_info['industry'],
        # 本期数据
        current_period=financial_data['current_period'],
        revenue=financial_data['revenue'],
        np_parent=financial_data['np_parent'],
        np_excl=financial_data['np_excl'],
        gross_margin=financial_data['gross_margin'],
        net_margin=financial_data['net_margin'],
        roe=financial_data['roe'],
        cfo=financial_data['cfo'],
        debt_ratio=financial_data['debt_ratio'],
        # 上期数据（环比）
        prev_revenue=financial_data['prev_revenue'],
        prev_np_parent=financial_data['prev_np_parent'],
        prev_np_excl=financial_data['prev_np_excl'],
        prev_gross_margin=financial_data['prev_gross_margin'],
        prev_net_margin=financial_data['prev_net_margin'],
        prev_roe=financial_data['prev_roe'],
        prev_cfo=financial_data['prev_cfo'],
        prev_debt_ratio=financial_data['prev_debt_ratio'],
        # 去年同期（同比）
        yoy_revenue=financial_data['yoy_revenue'],
        yoy_np_parent=financial_data['yoy_np_parent'],
        yoy_np_excl=financial_data['yoy_np_excl'],
        yoy_gross_margin=financial_data['yoy_gross_margin'],
        yoy_net_margin=financial_data['yoy_net_margin'],
        yoy_roe=financial_data['yoy_roe'],
        yoy_cfo=financial_data['yoy_cfo'],
        # 上年同期
        prev_yoy_revenue=financial_data['prev_yoy_revenue'],
        prev_yoy_np_parent=financial_data['prev_yoy_np_parent'],
        prev_yoy_np_excl=financial_data['prev_yoy_np_excl'],
        prev_yoy_gross_margin=financial_data['prev_yoy_gross_margin'],
        prev_yoy_net_margin=financial_data['prev_yoy_net_margin'],
        prev_yoy_roe=financial_data['prev_yoy_roe'],
        prev_yoy_cfo=financial_data['prev_yoy_cfo'],
    )
```

#### 3.4.4 财报解读报告存储

```sql
-- 财报解读报告表
CREATE TABLE tb_financial_report_analysis (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL,
    stock_name VARCHAR(100),
    report_date DATE NOT NULL,
    report_type VARCHAR(10) NOT NULL,

    -- AI 评分
    score_profitability DECIMAL(4, 2),   -- 盈利能力 1-10
    score_growth DECIMAL(4, 2),         -- 成长性 1-10
    score_cashflow DECIMAL(4, 2),       -- 现金流质量 1-10
    score_financial_health DECIMAL(4, 2),-- 财务健康度 1-10
    score_overall DECIMAL(4, 2),         -- 综合评分 1-10

    -- 报告内容（Markdown 格式）
    report_content TEXT,

    -- 关键变化摘要（用于推送）
    summary TEXT,

    -- 风险信号（JSON 数组）
    risk_signals JSONB,

    -- AI 模型信息
    ai_model VARCHAR(50),
    ai_tokens_used INTEGER,

    -- 处理状态
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PROCESSING, COMPLETED, FAILED

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stock_code, report_date, report_type)
);

CREATE INDEX idx_analysis_stock_date ON tb_financial_report_analysis(stock_code, report_date DESC);
CREATE INDEX idx_analysis_status ON tb_financial_report_analysis(status);
```

---

## 四、API 接口设计

### 4.1 RESTful API 设计

#### 财务报表接口

```
GET  /api/v1/stocks/{stockCode}/financial/income
     查询参数: period=2024-Q4&from=2022-01-01&to=2024-12-31
     响应: { data: [...], meta: { total: 12 } }

GET  /api/v1/stocks/{stockCode}/financial/balance
     同上

GET  /api/v1/stocks/{stockCode}/financial/cashflow
     同上

GET  /api/v1/stocks/{stockCode}/financial/indicator
     查询参数: period=2024-Q4&from=2022-01-01&to=2024-12-31
     响应: { data: [...], meta: { ... } }
```

#### 财务指标接口

```
GET  /api/v1/stocks/{stockCode}/indicators/profitability
     查询参数: period=2024-Q4
     响应: { roe, roa, gross_margin, net_margin, ... }

GET  /api/v1/stocks/{stockCode}/indicators/debt-ratio
GET  /api/v1/stocks/{stockCode}/indicators/operation
GET  /api/v1/stocks/{stockCode}/indicators/growth
GET  /api/v1/stocks/{stockCode}/indicators/valuation
```

#### 趋势分析接口

```
GET  /api/v1/stocks/{stockCode}/financial/trend
     查询参数: metric=revenue,np_parent,roe&periods=8
     响应: { data: [{ date, revenue, np_parent, roe }, ...] }

GET  /api/v1/stocks/{stockCode}/financial/comparison
     查询参数: metric=roe&peer=industry|selfdefined
     响应: { data: { stock: x, industry_avg: y, industry_leader: z } }

GET  /api/v1/stocks/{stockCode}/financial/dupont
     响应: { roe, factors: { net_margin, asset_turnover, equity_multiplier } }
```

#### 财报解读接口

```
GET  /api/v1/stocks/{stockCode}/financial/report-analysis
     查询参数: report_date=2024-12-31
     响应: { analysis: {...}, summary: "..." }

POST /api/v1/stocks/{stockCode}/financial/report-analysis
     Body: { report_date: "2024-12-31" }
     响应: { task_id: "xxx", status: "PROCESSING" }

GET  /api/v1/tasks/{taskId}/status
     响应: { status: "COMPLETED", result_id: 123 }
```

### 4.2 后端 Service 接口定义

```java
// src/main/java/org/cwowhappy/securityanalyze/service/FinancialReportService.java

package org.cwowhappy.securityanalyze.service;

import org.cwowhappy.securityanalyze.dto.*;
import java.util.List;

public interface FinancialReportService {

    /**
     * 获取利润表数据
     */
    List<FinancialIncomeDTO> getIncomeStatement(String stockCode, Date from, Date to);

    /**
     * 获取资产负债表数据
     */
    List<FinancialBalanceDTO> getBalanceSheet(String stockCode, Date from, Date to);

    /**
     * 获取现金流量表数据
     */
    List<FinancialCashflowDTO> getCashflowStatement(String stockCode, Date from, Date to);

    /**
     * 获取财务指标
     */
    List<FinancialIndicatorDTO> getFinancialIndicators(String stockCode, Date from, Date to);

    /**
     * 计算并获取指定报告期的财务指标
     */
    FinancialIndicatorDTO calculateIndicators(String stockCode, Date reportDate);

    /**
     * 获取核心指标趋势数据
     */
    TrendDataDTO getIndicatorTrend(String stockCode, List<String> metrics, int periods);

    /**
     * 获取同业对比数据
     */
    PeerComparisonDTO getPeerComparison(String stockCode, String metric);

    /**
     * 获取杜邦分析
     */
    DupontAnalysisDTO getDupontAnalysis(String stockCode, Date reportDate);
}
```

---

## 五、前端页面结构

### 5.1 页面路由

```
/financial
├── /:stockCode                    # 财务分析主页（默认展示基本面分析）
│   ├── ?tab=analysis              # 基本面分析（指标卡片 + 趋势图 + 杜邦 + 同业）
│   ├── ?tab=statements            # 财务报表（内含利润表/资产负债表/现金流量表子标签）
│   └── ?tab=ai-report             # AI 财报解读（待实现）
```

### 5.2 核心组件

```
src/views/financial/
├── FinancialAnalysisView.vue      # 财务分析主容器
├── components/
│   ├── FinancialTabs.vue          # 顶层标签页切换（基本面分析/财务报表/AI解读）
│   ├── StatementTabs.vue          # 财务报表子标签（利润表/资产负债表/现金流量表）
│   ├── IncomeStatement.vue        # 利润表展示组件
│   ├── BalanceSheet.vue           # 资产负债表展示组件
│   ├── CashflowStatement.vue      # 现金流量表展示组件
│   ├── IndicatorCard.vue          # 指标卡片组件
│   ├── IndicatorGrid.vue          # 指标网格布局
│   ├── IndicatorTrendChart.vue    # 指标趋势图表组件
│   ├── YoYComparisonTable.vue     # 同比对比表格组件（待实现）
│   ├── PeerComparisonChart.vue    # 同业对比图表
│   ├── DupontAnalysis.vue         # 杜邦分析展示
│   └── AIReportAnalysis.vue       # AI 财报解读展示（待实现）
└── composables/
    └── useFinancialFormatter.ts   # 金额/百分比格式化
```

### 5.3 Vue 组件设计示例

```vue
<!-- src/views/financial/components/IncomeStatement.vue -->

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FinancialIncomeDTO } from '@/types/financial'

const props = defineProps<{
  data: FinancialIncomeDTO[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'periodChange', period: string): void
}>()

// 单位切换
const unit = ref<'yuan' | 'wan' | 'yi'>('wan')
const unitFactor = computed(() => unit.value === 'yuan' ? 1 : unit.value === 'wan' ? 10000 : 100000000)

const formatValue = (value: number | null): string => {
  if (value === null || value === undefined) return '-'
  const formatted = (value / unitFactor.value).toFixed(2)
  return `${formatted} ${unit.value === 'wan' ? '万' : unit.value === 'yi' ? '亿' : '元'}`
}

const formatGrowth = (current: number, prev: number): string => {
  if (!prev) return '-'
  const growth = ((current - prev) / Math.abs(prev) * 100).toFixed(2)
  const arrow = growth >= 0 ? '↑' : '↓'
  return `${arrow} ${Math.abs(Number(growth))}%`
}

// 报告期筛选
const reportTypes = ['Y', 'Q1', 'Q2', 'Q3', 'Q4']
const selectedType = ref('Y')

const filteredData = computed(() => {
  return props.data.filter(item => item.reportType === selectedType.value)
})
</script>

<template>
  <div class="income-statement">
    <div class="toolbar">
      <el-radio-group v-model="unit">
        <el-radio-button value="yuan">元</el-radio-button>
        <el-radio-button value="wan">万元</el-radio-button>
        <el-radio-button value="yi">亿元</el-radio-button>
      </el-radio-group>

      <el-radio-group v-model="selectedType" @change="emit('periodChange', $event)">
        <el-radio-button v-for="type in reportTypes" :key="type" :value="type">
          {{ type === 'Y' ? '年报' : `${type}季报` }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="filteredData" v-loading="loading" stripe>
      <el-table-column prop="reportDate" label="报告期" width="120" />
      <el-table-column prop="revenue" label="营业收入" :formatter="(row) => formatValue(row.revenue)" align="right" />
      <el-table-column prop="grossProfit" label="毛利" :formatter="(row) => formatValue(row.grossProfit)" align="right" />
      <el-table-column prop="grossMargin" label="毛利率" :formatter="(row) => `${row.grossMargin?.toFixed(2)}%`" align="right" />
      <el-table-column prop="npParentCompany" label="归母净利润" :formatter="(row) => formatValue(row.npParentCompany)" align="right" />
      <el-table-column prop="netMargin" label="净利率" :formatter="(row) => `${row.netMargin?.toFixed(2)}%`" align="right" />
    </el-table>
  </div>
</template>
```

---

## 六、数据采集设计

### 6.1 采集任务规划

| 任务名称 | 数据范围 | 触发方式 | 执行频率 |
|---------|---------|---------|---------|
| `financial_income` | 利润表全量 | 定时 + 事件 | 财报披露后立即 + 每日增量 |
| `financial_balance` | 资产负债表全量 | 定时 + 事件 | 财报披露后立即 + 每日增量 |
| `financial_cashflow` | 现金流量表全量 | 定时 + 事件 | 财报披露后立即 + 每日增量 |
| `financial_indicator` | 财务指标计算 | 财务三表入库后 | 自动触发 |

### 6.2 采集数据源

| 数据类型 | 主数据源 | 备用数据源 | 说明 |
|---------|---------|----------|------|
| 财务三表 | Tushare Pro | AKShare | Tushare 字段完整，标准化好 |
| 财务指标 | Tushare Pro `fina_indicator` | 本地计算 | 150+ 预计算指标 |
| 估值指标 | Tushare Pro `daily_basic` | AKShare | PE/PB/PS 等每日更新 |

### 6.3 采集脚本结构

```python
# src/data_collector/scripts/financial_full.py

"""
全量财务数据采集脚本
支持 Tushare Pro 数据源，具备降级到 AKShare 的能力
"""

from typing import Optional
from pydantic import BaseModel
from datetime import date
from data_collector.core.domain import Stock, CollectionTask
from data_collector.scripts.tushare_client import TushareClient
from data_collector.scripts.akshare_client import AkshareClient
from data_collector.infrastructure.db_pool import DbPool


class FinancialDataConfig(BaseModel):
    max_workers: int = 1  # 财务数据建议单线程，避免触发限流
    request_delay_min: float = 1.0
    request_delay_max: float = 3.0
    batch_size: int = 100


class FinancialFullCollector:
    """财务数据全量采集器"""

    def __init__(
        self,
        tushare_client: TushareClient,
        akshare_client: Optional[AkshareClient] = None,
        db_pool: DbPool,
        config: FinancialDataConfig = FinancialDataConfig()
    ):
        self.tushare = tushare_client
        self.akshare = akshare_client
        self.db = db_pool
        self.config = config

    def collect_income(self, stock: Stock, start_date: date, end_date: date) -> int:
        """采集利润表数据"""
        try:
            # 主数据源：Tushare Pro
            df = self.tushare.get_income(
                ts_code=stock.tushare_code,
                start_date=start_date.strftime('%Y%m%d'),
                end_date=end_date.strftime('%Y%m%d'),
                fields=[
                    'ts_code', 'ann_date', 'f_ann_date', 'end_date', 'report_type',
                    'basic_eps', 'diluted_eps', 'total_revenue', 'revenue',
                    'operating_cost', 'gross_profit', 'selling_expense', 'admin_expense',
                    'rd_expense', 'financial_expense', 'operating_profit', 'total_profit',
                    'net_profit', 'np_parent_company', 'np_excl_nonrecurring'
                ]
            )
            return self._save_income_data(stock.code, df)

        except SourceUnavailableError as e:
            # 降级到 AKShare
            self._log.warning(f"Tushare unavailable, falling back to AKShare: {e}")
            return self._collect_income_akshare(stock, start_date, end_date)

    def _save_income_data(self, stock_code: str, df) -> int:
        """保存利润表数据到数据库"""
        saved = 0
        with self.db.connection() as conn:
            for _, row in df.iterrows():
                conn.execute("""
                    INSERT INTO tb_financial_income (
                        stock_code, report_date, report_type,
                        basic_eps, diluted_eps, total_revenue, revenue,
                        operating_cost, gross_profit, selling_expense,
                        admin_expense, rd_expense, financial_expense,
                        operating_profit, total_profit, net_profit,
                        np_parent_company, np_excl_nonrecurring,
                        updated_at
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW()
                    ) ON CONFLICT (stock_code, report_date, report_type)
                    DO UPDATE SET
                        basic_eps = EXCLUDED.basic_eps,
                        updated_at = NOW()
                """, (
                    stock_code, row['end_date'], row['report_type'],
                    row['basic_eps'], row['diluted_eps'], row['total_revenue'], row['revenue'],
                    row['operating_cost'], row['gross_profit'], row['selling_expense'],
                    row['admin_expense'], row['rd_expense'], row['financial_expense'],
                    row['operating_profit'], row['total_profit'], row['net_profit'],
                    row['np_parent_company'], row['np_excl_nonrecurring']
                ))
                saved += 1
        return saved
```

---

## 七、实施路径

### Phase 1：基础数据层（MVP）

| 任务 | 说明 | 工期 | 依赖 |
|------|------|------|------|
| T1.1 | 设计并创建财务三表数据库结构 | 1天 | - |
| T1.2 | 开发 Tushare Pro 财务数据采集脚本 | 3天 | - |
| T1.3 | 开发 AKShare 降级采集脚本 | 2天 | T1.2 |
| T1.4 | 后端 API：财务报表查询接口 | 2天 | T1.1 |
| T1.5 | 前端：财务报表展示页面 | 3天 | T1.4 |
| T1.6 | 财务指标计算服务开发 | 3天 | T1.1 |
| T1.7 | 后端 API：财务指标查询接口 | 1天 | T1.6 |
| T1.8 | 前端：财务指标卡片展示 | 2天 | T1.7 |

**Phase 1 交付物**：
- 财务三表数据可查询
- 核心财务指标（ROE/毛利率/净利率/营收增速）可展示

### Phase 2：分析增强

| 任务 | 说明 | 工期 | 依赖 |
|------|------|------|------|
| T2.1 | 趋势图表组件开发 | 3天 | Phase 1 |
| T2.2 | 同比/环比对比功能 | 2天 | T2.1 |
| T2.3 | 杜邦分析计算与展示 | 3天 | T1.6 |
| T2.4 | 同业对比数据聚合与展示 | 4天 | Phase 1 |
| T2.5 | 财报解读 Agent 开发 | 5天 | T1.1 |
| T2.6 | 财报解读结果展示与推送 | 3天 | T2.5 |

**Phase 2 交付物**：
- 财务趋势可视化
- 同业对比分析
- AI 财报自动解读与推送

### Phase 3：高级功能

| 任务 | 说明 | 工期 | 依赖 |
|------|------|------|------|
| T3.1 | 估值历史分位计算与展示 | 3天 | Phase 1 |
| T3.2 | 现金流质量分析 | 3天 | T1.6 |
| T3.3 | 自定义指标筛选与排序 | 4天 | T1.6 |
| T3.4 | 财务报告 PDF 导出 | 3天 | Phase 2 |

---

## 八、风险与注意事项

| 风险类型 | 说明 | 应对措施 |
|---------|------|---------|
| **Tushare 限流** | 财务数据接口调用频繁可能触发限流 | 设置请求延迟 + AKShare 降级方案 |
| **财务数据延迟** | A 股财报披露有时间差，年报通常次年4月才全部披露 | 财报披露日历监控 + 增量采集 |
| **季报累计数据** | Q2/Q3 季报为累计值，非单季数据，需特殊处理 | 数据入库时标注 report_type，查询时区分展示 |
| **非经常性损益** | 一次性损益可能干扰利润分析 | 优先参考扣非净利润 |
| **会计准则差异** | 不同公司可能采用不同会计准则 | 数据标准化时做标注说明 |

---

## 九、相关文档

| 文档 | 路径 |
|------|------|
| 产品规划文档 | `docs/prd/security-analyze-full-prd.md` |
| 功能规划文档 | `docs/prd/feature-planning.md` |
| 股票公司采集设计 | `docs/plans/2026-05-10-stock-company-collection-design.md` |
| 数据模型设计 | `docs/wiki/data-model-stock-company-collection.md` |

---

*本文档为财务报告分析模块详细设计，具体实施需根据实际开发进度调整。*
