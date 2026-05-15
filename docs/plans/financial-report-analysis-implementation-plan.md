# 财务报告与基本面分析模块 · 技术设计与实现计划

> 版本：v1.0 | 日期：2026-05-11 | 基于现有 DDD 分层架构扩展

---

## 一、总体架构决策

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| **沿用现有分层** | 严格遵循后端 `domain → application → interfaces` + `infrastructure` 的 DDD 分层，前端沿用 `views/stores/api/types` 结构 |
| **最小侵入** | 不修改现有 stock/company/user 模块，通过 `stockCode` 外键关联 |
| **渐进交付** | 按 P0 → P1 → P2 优先级分阶段实现，每阶段可独立测试 |
| **数据先行** | 先建表 → 采集器入库 → 后端 API → 前端页面 |

### 1.2 模块边界

```
┌─────────────────────────────────────────────────────────────────┐
│                     财务报告分析模块（新增）                       │
│                                                                 │
│  后端: org.cwowhappy.securityanalyze.financial.*                │
│  前端: src/views/financial/  +  src/types/financial.ts          │
│  采集: src/data_collector/scripts/financial_*.py                │
│                                                                 │
│  依赖现有: tb_stock_basic.stock_code（外键关联）                 │
│  被依赖: 暂无                                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、数据库设计（Flyway 迁移）

### 2.1 新增表清单

| 表名 | 说明 | 优先级 |
|------|------|--------|
| `tb_financial_income` | 利润表 | P0 |
| `tb_financial_balance` | 资产负债表 | P0 |
| `tb_financial_cashflow` | 现金流量表 | P0 |
| `tb_financial_indicator` | 财务指标（计算结果）| P0 |
| `tb_financial_report_analysis` | AI 财报解读报告 | P1 |

### 2.2 迁移脚本规划

```
backend/src/main/resources/db/migration/
├── V9__create_financial_statement_tables.sql      -- 三表：income/balance/cashflow
├── V10__create_financial_indicator_table.sql      -- 指标计算结果表
└── V11__create_financial_report_analysis_table.sql -- AI 解读报告表
```

> 具体 DDL 直接引用 PRD 中的定义，略作调整（统一命名前缀、补充索引）。

---

## 三、后端实现计划（Java）

### 3.1 包结构

```
org.cwowhappy.securityanalyze.financial/
├── domain/
│   ├── model/
│   │   ├── FinancialIncome.java           -- 利润表领域实体
│   │   ├── FinancialBalance.java          -- 资产负债表领域实体
│   │   ├── FinancialCashflow.java         -- 现金流量表领域实体
│   │   ├── FinancialIndicator.java        -- 财务指标领域实体
│   │   └── FinancialReportAnalysis.java   -- AI 解读报告领域实体
│   └── repository/
│       ├── FinancialIncomeRepository.java
│       ├── FinancialBalanceRepository.java
│       ├── FinancialCashflowRepository.java
│       ├── FinancialIndicatorRepository.java
│       └── FinancialReportAnalysisRepository.java
├── application/
│   ├── dto/
│   │   ├── FinancialIncomeDTO.java
│   │   ├── FinancialBalanceDTO.java
│   │   ├── FinancialCashflowDTO.java
│   │   ├── FinancialIndicatorDTO.java
│   │   ├── TrendDataDTO.java
│   │   ├── DupontAnalysisDTO.java
│   │   └── PeerComparisonDTO.java
│   └── service/
│       ├── FinancialReportAppService.java         -- 财报查询服务
│       ├── FinancialIndicatorAppService.java      -- 指标计算与查询
│       └── FinancialAnalysisAppService.java       -- 趋势/杜邦/对比
├── infrastructure/
│   └── persistence/
│       ├── entity/
│       │   ├── FinancialIncomeEntity.java
│       │   ├── FinancialBalanceEntity.java
│       │   ├── FinancialCashflowEntity.java
│       │   ├── FinancialIndicatorEntity.java
│       │   └── FinancialReportAnalysisEntity.java
│       ├── mapper/
│       │   ├── FinancialIncomeRowMapper.java
│       │   ├── FinancialBalanceRowMapper.java
│       │   ├── FinancialCashflowRowMapper.java
│       │   ├── FinancialIndicatorRowMapper.java
│       │   └── FinancialReportAnalysisRowMapper.java
│       └── repository/
│           ├── JdbcFinancialIncomeRepository.java
│           ├── JdbcFinancialBalanceRepository.java
│           ├── JdbcFinancialCashflowRepository.java
│           ├── JdbcFinancialIndicatorRepository.java
│           └── JdbcFinancialReportAnalysisRepository.java
└── interfaces/rest/
    └── controller/
        └── FinancialAnalysisController.java
```

### 3.2 RESTful API 设计（与 PRD 对齐）

```
GET  /api/v1/stocks/{stockCode}/financial/income
GET  /api/v1/stocks/{stockCode}/financial/balance
GET  /api/v1/stocks/{stockCode}/financial/cashflow
GET  /api/v1/stocks/{stockCode}/financial/indicator
GET  /api/v1/stocks/{stockCode}/financial/trend?metrics=revenue,np_parent,roe&periods=8
GET  /api/v1/stocks/{stockCode}/financial/dupont?reportDate=2024-12-31
GET  /api/v1/stocks/{stockCode}/financial/peer-comparison?metric=roe
GET  /api/v1/stocks/{stockCode}/financial/report-analysis?reportDate=2024-12-31
POST /api/v1/stocks/{stockCode}/financial/report-analysis  -- 触发 AI 解读
```

### 3.3 实现顺序

| 阶段 | 内容 | 预计工作量 |
|------|------|-----------|
| 1 | Flyway 迁移脚本（V9 三表） | 0.5 天 |
| 2 | 领域模型 + Entity + RowMapper + Repository | 1.5 天 |
| 3 | Application DTO + AppService（财报查询、指标计算） | 2 天 |
| 4 | Controller + 全局异常处理扩展 | 1 天 |
| 5 | 集成测试（Testcontainers） | 1 天 |

---

## 四、前端实现计划（Vue 3 + TypeScript）

### 4.1 目录结构

```
frontend/src/
├── types/
│   └── financial.ts              -- 财务数据类型定义
├── api/modules/
│   └── financial.ts              -- 财务 API 封装
├── stores/modules/
│   └── financial.ts              -- Pinia Store
├── views/
│   └── financial/
│       ├── FinancialAnalysisView.vue      -- 财务分析主容器
│       ├── components/
│       │   ├── FinancialTabs.vue          -- 顶层标签页切换（基本面分析/财务报表/AI解读）
│       │   ├── StatementTabs.vue          -- 财务报表子标签（利润表/资产负债表/现金流量表）
│       │   ├── IndicatorCard.vue          -- 指标卡片
│       │   ├── IndicatorGrid.vue          -- 指标网格布局
│       │   ├── IncomeStatement.vue        -- 利润表
│       │   ├── BalanceSheet.vue           -- 资产负债表
│       │   ├── CashflowStatement.vue      -- 现金流量表
│       │   ├── IndicatorTrendChart.vue    -- 趋势图表
│       │   ├── YoYComparisonTable.vue     -- 同比对比表（待实现）
│       │   ├── DupontAnalysis.vue         -- 杜邦分析
│       │   ├── PeerComparisonChart.vue    -- 同业对比
│       │   └── AIReportAnalysis.vue       -- AI 解读面板（待实现）
│       └── composables/
│           └── useFinancialFormatter.ts   -- 金额/百分比格式化
```

### 4.2 路由设计

在现有 `/stocks/:stockCode` 路由下增加子路由，或作为独立页面：

```typescript
// 方案 A：股票详情页内嵌标签页（推荐，与原型一致）
{
  path: '/stocks/:stockCode',
  name: 'StockDetail',
  component: () => import('@/views/stock/StockDetailView.vue'),
  children: [
    {
      path: 'financial',
      name: 'StockFinancial',
      component: () => import('@/views/financial/FinancialAnalysisView.vue'),
      meta: { title: '财务分析' }
    }
  ]
}

// 方案 B：独立路由（备用）
{
  path: '/financial/:stockCode',
  name: 'FinancialAnalysis',
  component: () => import('@/views/financial/FinancialAnalysisView.vue'),
}
```

### 4.3 关键组件规格

| 组件 | 职责 | 依赖 |
|------|------|------|
| `IndicatorCard` | 单指标展示（值 + 同比变化 + 报告期） | 纯展示 |
| `IndicatorGrid` | 4x2/3x2/2x4 响应式网格 | `IndicatorCard` |
| `FinancialTable` | 三表数据表格（支持单位切换）| 纯展示 |
| `IndicatorTrendChart` | 趋势图（营收/净利润/ROE 等）| ECharts / Chart.js |
| `DupontAnalysis` | ROE 三因素分解展示 | 纯展示 |
| `AIReportAnalysis` | AI 解读报告渲染（Markdown）| 无 |

### 4.4 状态管理（Pinia）

```typescript
// stores/modules/financial.ts
interface FinancialState {
  stockCode: string | null
  activeTab: 'analysis' | 'statements' | 'ai-report'
  activeStatement: 'income' | 'balance' | 'cashflow'  // 财务报表子标签
  indicators: FinancialIndicator[]
  incomeStatements: FinancialIncome[]
  balanceSheets: FinancialBalance[]
  cashflows: FinancialCashflow[]
  trendData: TrendData[]
  dupontData: DupontAnalysis | null
  peerComparison: PeerComparison | null
  aiReport: AIReport | null
  loading: boolean
}
```

---

## 五、数据采集器实现计划（Python）

### 5.1 新增采集脚本

```
collector/src/data_collector/scripts/
├── financial_income.py           -- 利润表采集
├── financial_balance.py          -- 资产负债表采集
├── financial_cashflow.py         -- 现金流量表采集
├── financial_indicator.py        -- 财务指标采集/计算
└── financial_full.py             -- 三表批量采集编排
```

### 5.2 数据源选择

| 数据类型 | 主源 | 备用源 | 说明 |
|---------|------|--------|------|
| 利润表 | AKShare `stock_financial_report_sina` | Tushare `income` | AKShare 免费优先 |
| 资产负债表 | AKShare `stock_financial_report_sina` | Tushare `balancesheet` | — |
| 现金流量表 | AKShare `stock_financial_report_sina` | Tushare `cashflow` | — |
| 财务指标 | 后端计算 | Tushare `fina_indicator` | 核心指标自行计算，Tushare 作校验 |

### 5.3 指标计算服务

在采集器中新增 `services/indicator_calculator.py`：

```python
class IndicatorCalculator:
    def calculate(self, income: Income, balance: Balance, cashflow: Cashflow) -> Indicator:
        # 盈利能力
        roe = np_parent / avg_equity
        roa = net_profit / avg_assets
        gross_margin = gross_profit / revenue
        # ... 30+ 指标
```

> 采集器计算后入库 `tb_financial_indicator`，后端只做查询和轻量组装。

---

## 六、实施路线图

### Phase 1：数据底座（Week 1）

| 任务 | 负责人 | 产出 |
|------|--------|------|
| Flyway V9/V10 迁移脚本 | 后端 | 数据库表就绪 |
| 采集器：三表采集脚本 | 采集器 | AKShare 财务数据采集 |
| 后端：领域模型 + Repository | 后端 | 数据访问层就绪 |

### Phase 2：后端 API（Week 2）

| 任务 | 产出 |
|------|------|
| AppService（财报查询 + 指标计算）| `/financial/{income,balance,cashflow,indicator}` |
| 趋势/杜邦/对比 API | `/financial/trend`, `/dupont`, `/peer-comparison` |
| Controller + 测试 | 完整 API 契约 + 集成测试通过 |

### Phase 3：前端页面（Week 3）

| 任务 | 产出 |
|------|------|
| 类型定义 + API 模块 + Store | 数据层就绪 |
| 财务报表标签页（三表展示）| `/stocks/:code/financial` |
| 基本面概览（指标卡片 + 趋势图）| 核心指标看板 |
| 同比对比 + 杜邦分析 | 分析工具就绪 |

### Phase 4：AI 解读 + 同业对比（Week 4）

| 任务 | 产出 |
|------|------|
| AI 财报解读接口 + 前端面板 | DeepSeek API 接入 |
| 同业对比 API + 前端图表 | 行业均值对比 |
| 设计规范对齐（Stripe 风格）| CSS Variables + 暗色主题 |

---

## 七、风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| AKShare 财务字段不完整 | 数据采集失败 | 降级到 Tushare Pro；字段缺失时留空 |
| 财务指标计算精度 | 指标值与外部工具不一致 | 使用 `Decimal` / `BigDecimal`，单元测试覆盖 |
| 前端图表库选择 | ECharts vs Chart.js | 优先 ECharts（中文文档好），体积大可按需引入 |
| AI 解读延迟高 | 用户体验差 | 异步任务 + 轮询状态；首次展示骨架屏 |
| 数据量大（5000+ 股票 x 12 期）| 查询慢 | 按 `stock_code + report_date` 建立复合索引；分页返回 |

---

## 八、验收标准

| 验收项 | 标准 |
|--------|------|
| 数据库 | Flyway 迁移成功，表结构符合 PRD |
| 后端 API | 所有 `/financial/*` 接口返回正确 JSON，集成测试通过 |
| 前端页面 | 股票详情页可切换至财务分析，三表展示正确，指标卡片响应式正常 |
| 数据采集 | 单只股票三表可在 10 秒内完成采集入库 |
| 代码质量 | Jacoco 覆盖率 ≥ 60%，Sonar 无阻断级问题 |

---

*本文档为财务报告与基本面分析模块的技术设计总纲，各子任务需进一步细化到具体文件和函数级别。*
