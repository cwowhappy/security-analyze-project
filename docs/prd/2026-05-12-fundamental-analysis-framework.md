# 基本面分析框架 · 指标体系完整版

> 版本：v1.1 | 日期：2026-05-12 | 更新：补充缺失指标

---

## 一、基本面分析策略总览

证券基本面分析主要从以下几个维度评估公司投资价值：

| 分析维度 | 核心问题 | 主要指标 |
|---------|---------|---------|
| **盈利能力** | 公司赚钱能力强不强？ | ROE、ROA、ROIC、毛利率、净利率 |
| **成长性** | 公司增长前景如何？ | 营收增速、利润增速、扣非增速 |
| **偿债能力** | 财务风险大不大？ | 资产负债率、流动比率、速动比率、净负债率 |
| **运营效率** | 资产周转快不快？ | 资产周转率、存货周转天数、应收账款周转天数 |
| **估值水平** | 当前价格贵不贵？ | PE、PB、PS、股息率 |
| **现金流质量** | 利润含金量高不高？ | 经营现金流/净利润、自由现金流、资本开支 |

---

## 二、指标体系对照表

### 2.1 盈利能力指标 ✅ 已实现 | ❌ 待补充

| 指标名 | 代码 | 优先级 | 原型状态 | 计算公式 |
|--------|------|--------|---------|---------|
| 净资产收益率 | ROE | P0 | ✅ 已实现 | 归母净利润 ÷ 平均归母股东权益 × 100% |
| 总资产收益率 | ROA | P1 | ❌ 待补充 | 净利润 ÷ 平均总资产 × 100% |
| 投入资本回报率 | ROIC | P2 | ❌ 待补充 | EBIT×(1-税率) ÷ (股东权益+有息负债) |
| 毛利率 | GrossMargin | P0 | ✅ 已实现 | (营收 - 营业成本) ÷ 营收 × 100% |
| 净利率 | NetMargin | P0 | ✅ 已实现 | 净利润 ÷ 营收 × 100% |
| 扣非净利率 | NetMarginExcl | P1 | ❌ 待补充 | 扣非净利润 ÷ 营收 × 100% |
| 基本每股收益 | EPS | P0 | ✅ 已实现 | 归母净利润 ÷ 总股本 |
| 稀释每股收益 | DilutedEPS | P1 | ✅ 已实现 | 考虑稀释效应后的 EPS |

### 2.2 偿债能力指标 ✅ 已实现 | ❌ 待补充

| 指标名 | 代码 | 优先级 | 原型状态 | 计算公式 |
|--------|------|--------|---------|---------|
| 资产负债率 | DebtRatio | P0 | ✅ 已实现 | 总负债 ÷ 总资产 × 100% |
| 流动比率 | CurrentRatio | P1 | ❌ 待补充 | 流动资产 ÷ 流动负债 |
| 速动比率 | QuickRatio | P1 | ❌ 待补充 | (流动资产 - 存货) ÷ 流动负债 |
| 净负债率 | NetDebtRatio | P2 | ❌ 待补充 | (有息负债 - 现金) ÷ 净资产 × 100% |
| 产权比率 | EquityRatio | P2 | ❌ 待补充 | 总负债 ÷ 股东权益 |

### 2.3 运营效率指标 ✅ 已实现 | ❌ 待补充

| 指标名 | 代码 | 优先级 | 原型状态 | 计算公式 |
|--------|------|--------|---------|---------|
| 总资产周转率 | AssetTurnover | P1 | ✅ 已实现（杜邦） | 营收 ÷ 平均总资产 |
| 应收账款周转天数 | DSO | P1 | ❌ 待补充 | 平均应收账款 ÷ 营收 × 360 天 |
| 存货周转天数 | DIO | P1 | ❌ 待补充 | 平均存货 ÷ 营业成本 × 360 天 |
| 应付账款周转天数 | DPO | P2 | ❌ 待补充 | 平均应付账款 ÷ 营业成本 × 360 天 |
| 现金转换周期 | CCC | P2 | ❌ 待补充 | DSO + DIO - DPO |
| 固定资产周转率 | FixedAssetTurnover | P2 | ❌ 待补充 | 营收 ÷ 平均固定资产 |

### 2.4 成长性指标 ✅ 已实现 | ❌ 待补充

| 指标名 | 代码 | 优先级 | 原型状态 | 计算公式 |
|--------|------|--------|---------|---------|
| 营收增速（YoY）| RevenueGrowth | P0 | ✅ 已实现 | (本期营收 - 上期营收) ÷ 上期营收 × 100% |
| 归母净利润增速 | NPParentGrowth | P1 | ❌ 待补充 | (本期归母净利润 - 上期) ÷ 上期 × 100% |
| 扣非净利润增速 | NPExclGrowth | P1 | ❌ 待补充 | (本期扣非净利润 - 上期) ÷ 上期 × 100% |
| 经营现金流增速 | CFOGrowth | P1 | ❌ 待补充 | (本期经营现金流 - 上期) ÷ 上期 × 100% |
| 净资产增速 | EquityGrowth | P2 | ❌ 待补充 | 净资产增长率 |
| 总资产增速 | AssetGrowth | P2 | ❌ 待补充 | 总资产增长率 |

### 2.5 估值指标 ✅ 已实现 | ❌ 待补充

| 指标名 | 代码 | 优先级 | 原型状态 | 计算公式 |
|--------|------|--------|---------|---------|
| 市盈率 | PE-TTM | P0 | ✅ 已实现 | 市值 ÷ 归母净利润（TTM）|
| 市净率 | PB | P0 | ✅ 已实现 | 市值 ÷ 净资产 |
| 市销率 | PS | P1 | ❌ 待补充 | 市值 ÷ 营收（TTM）|
| 股息率 | DividendYield | P1 | ✅ 已实现 | 股息 ÷ 股价 × 100% |
| PEG 比率 | PEG | P2 | ❌ 待补充 | PE ÷ 净利润增速（G）|
| EV/EBITDA | EVEBITDA | P2 | ❌ 待补充 | 企业价值 ÷ EBITDA |

### 2.6 现金流质量指标 ✅ 已实现 | ❌ 待补充

| 指标名 | 代码 | 优先级 | 原型状态 | 计算公式 |
|--------|------|--------|---------|---------|
| 经营现金流 | CFO | P0 | ✅ 已实现 | 经营活动产生的现金流量净额 |
| 经营现金流/净利润 | CFOtoNP | P0 | ✅ 已实现 | CFO ÷ 净利润 |
| 自由现金流 | FCF | P1 | ✅ 已实现 | CFO - 资本开支 |
| 资本开支 | CAPEX | P1 | ✅ 已实现 | 购建固定资产等支出 |
| 净现金流 | NetCF | P1 | ✅ 已实现 | 经营+投资+筹资现金流之和 |
| 投资现金流 | CFI | P2 | ✅ 已实现 | 投资活动产生的现金流量净额 |
| 筹资现金流 | CFF | P2 | ✅ 已实现 | 筹资活动产生的现金流量净额 |

### 2.7 杜邦分析 ✅ 已实现

| 指标名 | 代码 | 原型状态 | 说明 |
|--------|------|---------|------|
| ROE 分解 | Dupont | ✅ 已实现 | ROE = 净利率 × 资产周转率 × 权益乘数 |
| 净利率（盈利能力）| NetMargin | ✅ 已实现 | 净利润 ÷ 营收 |
| 资产周转率（运营效率）| AssetTurnover | ✅ 已实现 | 营收 ÷ 总资产 |
| 权益乘数（财务杠杆）| EquityMultiplier | ✅ 已实现 | 总资产 ÷ 净资产 |

---

## 三、原型现状总结

### 3.1 已实现指标（14项）

| 类别 | 指标 |
|------|------|
| 盈利能力 | ROE、毛利率、净利率、基本每股收益、稀释每股收益 |
| 偿债能力 | 资产负债率 |
| 运营效率 | 总资产周转率（杜邦中） |
| 成长性 | 营收增速（YoY）|
| 估值 | PE-TTM、PB、股息率 |
| 现金流 | 经营现金流、经营现金流/净利润、自由现金流、资本开支、净现金流 |
| 杜邦 | 净利率、资产周转率、权益乘数 |

### 3.2 待补充指标（20项）

| 类别 | 指标 | 优先级 |
|------|------|--------|
| 盈利能力 | ROA、ROIC、扣非净利率 | P1-P2 |
| 偿债能力 | 流动比率、速动比率、净负债率、产权比率 | P1-P2 |
| 运营效率 | DSO、DIO、DPO、CCC、固定资产周转率 | P1-P2 |
| 成长性 | 归母净利润增速、扣非净利润增速、经营现金流增速、净资产增速、总资产增速 | P1-P2 |
| 估值 | PS、PEG、EV/EBITDA | P1-P2 |

### 3.3 覆盖率统计

| 维度 | 应有指标 | 已实现 | 覆盖率 |
|------|---------|-------|-------|
| 盈利能力 | 8 | 5 | 62.5% |
| 偿债能力 | 5 | 1 | 20% |
| 运营效率 | 6 | 1 | 16.7% |
| 成长性 | 6 | 1 | 16.7% |
| 估值 | 6 | 3 | 50% |
| 现金流 | 7 | 7 | 100% |
| **总计** | **38** | **18** | **47.4%** |

---

## 四、补充优先级建议

### Phase 1（高优先级，P0-P1）

| 指标 | 理由 |
|------|------|
| ROA | 衡量资产盈利效率的核心指标，与 ROE 互补 |
| 流动比率 | 衡量短期偿债能力，银行/保险行业尤为重要 |
| 速动比率 | 剔除存货后的流动性指标，更严格 |
| 营收增速（已有）| - |
| 归母净利润增速 | 判断利润增长质量 |
| 扣非净利润增速 | 剔除非经常性损益后的真实增长 |
| PS（市销率）| 适用于亏损或低利润公司估值 |

### Phase 2（中优先级，P2）

| 指标 | 理由 |
|------|------|
| ROIC | 衡量投入资本的回报，更准确反映创造价值能力 |
| 净负债率 | 衡量真实杠杆水平 |
| DSO/DIO/DPO | 运营效率分析的核心指标 |
| CCC | 营运资本效率的综合指标 |
| PEG | 成长股估值参考 |
| 净资产增速 | 股东权益增长情况 |

---

## 五、数据库表扩展建议

```sql
-- 扩展财务指标表，补充缺失指标
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS roa DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS roic DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS net_margin_excl DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS current_ratio DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS quick_ratio DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS net_debt_ratio DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS dso DECIMAL(10, 2);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS dio DECIMAL(10, 2);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS dpo DECIMAL(10, 2);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS ccc DECIMAL(10, 2);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS np_parent_growth DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS np_excl_growth DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS cfo_growth DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS equity_growth DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS asset_growth DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS ps DECIMAL(18, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS peg DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS ev_ebitda DECIMAL(10, 4);
ALTER TABLE tb_financial_indicator ADD COLUMN IF NOT EXISTS cfo_to_np DECIMAL(10, 4);
```

---

*本文档为基本面分析框架完整版，为后续原型扩展和功能开发提供依据。*
