# 财务报告模块设计文档

> 本文档定义公司财务报告模块的完整设计方案，涵盖数据模型、后端 API、前端页面及数据采集策略，作为开发实施的共同依据。  
> 版本：v1.0  
> 日期：2026-04-30

---

## 一、设计目标与范围

### 1.1 目标
- 支持查询上市公司季度与年度财务报告（资产负债表、利润表、现金流量表）
- 提供核心财务指标的趋势图表与对比分析
- 支持按报告期切换查看历史数据
- 数据通过 akshare 自动采集并持久化到 PostgreSQL

### 1.2 范围
- **覆盖市场**：A 股（沪深京）
- **报告类型**：年报、中报、一季报、三季报
- **数据源**：akshare（东方财富接口）
- **不包含**：实时行情数据、K 线数据、人工录入/修改

---

## 二、数据模型设计

### 2.1 设计原则
1. **核心指标结构化**：高频查询的财务指标（总资产、净利润、营收等）使用结构化字段存储，便于索引、排序与计算。
2. **完整数据 JSONB 化**：三张报表的完整原始数据存入 `JSONB`，保留字段灵活性，避免 400+ 列的宽表维护噩梦。
3. **去重键**：以 `(stock_code, report_date)` 为唯一约束，避免重复采集。

### 2.2 表结构

```sql
-- ============================================================
-- V3: 财务报表数据模型
-- ============================================================

CREATE TABLE IF NOT EXISTS financial_report (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,              -- 股票代码
    report_date DATE NOT NULL,                    -- 报告期截止日期（如 2025-12-31）
    report_type VARCHAR(10) NOT NULL,             -- 报告类型：年报 / 中报 / 一季报 / 三季报
    report_year INTEGER NOT NULL,                 -- 报告所属年份
    notice_date DATE,                             -- 公告日期
    currency VARCHAR(10) DEFAULT 'CNY',           -- 币种

    -- ========== 资产负债表核心指标 ==========
    total_assets DECIMAL(20,4),                   -- 资产总计
    total_liabilities DECIMAL(20,4),              -- 负债合计
    total_equity DECIMAL(20,4),                   -- 所有者权益合计
    monetary_funds DECIMAL(20,4),                 -- 货币资金
    accounts_receivable DECIMAL(20,4),            -- 应收账款
    inventory DECIMAL(20,4),                      -- 存货
    total_current_assets DECIMAL(20,4),           -- 流动资产合计
    total_noncurrent_assets DECIMAL(20,4),        -- 非流动资产合计
    total_current_liabilities DECIMAL(20,4),      -- 流动负债合计
    total_noncurrent_liabilities DECIMAL(20,4),   -- 非流动负债合计

    -- ========== 利润表核心指标 ==========
    total_revenue DECIMAL(20,4),                  -- 营业总收入
    operate_income DECIMAL(20,4),                 -- 营业收入
    operate_cost DECIMAL(20,4),                   -- 营业成本
    sale_expense DECIMAL(20,4),                   -- 销售费用
    manage_expense DECIMAL(20,4),                 -- 管理费用
    research_expense DECIMAL(20,4),               -- 研发费用
    finance_expense DECIMAL(20,4),                -- 财务费用
    operate_profit DECIMAL(20,4),                 -- 营业利润
    total_profit DECIMAL(20,4),                   -- 利润总额
    net_profit DECIMAL(20,4),                     -- 净利润
    parent_net_profit DECIMAL(20,4),              -- 归母净利润

    -- ========== 现金流量表核心指标 ==========
    operating_cash_flow DECIMAL(20,4),            -- 经营活动现金流净额
    investing_cash_flow DECIMAL(20,4),            -- 投资活动现金流净额
    financing_cash_flow DECIMAL(20,4),            -- 筹资活动现金流净额
    cce_add DECIMAL(20,4),                        -- 现金及等价物净增加额
    end_cce DECIMAL(20,4),                        -- 期末现金及等价物余额

    -- ========== 完整原始数据（JSONB） ==========
    balance_sheet JSONB,                          -- 资产负债表完整数据
    profit_sheet JSONB,                           -- 利润表完整数据
    cash_flow_sheet JSONB,                        -- 现金流量表完整数据

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_fin_report UNIQUE (stock_code, report_date)
);

-- 索引
CREATE INDEX idx_fin_report_stock_code ON financial_report(stock_code);
CREATE INDEX idx_fin_report_date ON financial_report(report_date);
CREATE INDEX idx_fin_report_type ON financial_report(report_type);
CREATE INDEX idx_fin_report_year ON financial_report(report_year);
CREATE INDEX idx_fin_report_notice ON financial_report(notice_date);
```

### 2.3 字段说明

| 字段组 | 字段 | 业务含义 |
|--------|------|----------|
| 元数据 | `stock_code` | 关联 `company_security.stock_code` |
| 元数据 | `report_date` | 报告期截止日期，如 `2025-12-31` |
| 元数据 | `report_type` | 年报 / 中报 / 一季报 / 三季报 |
| 资产 | `total_assets` | 资产总计，衡量公司规模 |
| 资产 | `total_equity` | 所有者权益，即净资产 |
| 负债 | `total_liabilities` | 负债合计 |
| 收入 | `total_revenue` | 营业总收入 |
| 利润 | `net_profit` | 净利润（含少数股东损益）|
| 利润 | `parent_net_profit` | 归母净利润，EPS 计算基数 |
| 现金流 | `operating_cash_flow` | 经营活动现金流净额，衡量造血能力 |
| 完整数据 | `balance_sheet` | JSONB，含 140+ 原始字段 |
| 完整数据 | `profit_sheet` | JSONB，含 200+ 原始字段 |
| 完整数据 | `cash_flow_sheet` | JSONB，含 250+ 原始字段 |

---

## 三、后端设计

### 3.1 领域模型

```
backend/src/main/java/com/example/securityanalyze/finance/
├── api
│   ├── FinanceController.java
│   ├── FinanceReportResponse.java
│   ├── FinanceIndicatorResponse.java
│   └── FinanceReportListResponse.java
├── application
│   └── FinanceService.java
├── domain
│   ├── FinancialReport.java
│   ├── FinancialReportRepository.java
│   └── FinancialIndicator.java          -- 领域服务：指标计算
└── infrastructure
    └── FinancialReportRepositoryImpl.java
```

#### FinancialReport（领域实体）

```java
package com.example.securityanalyze.finance.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Table("financial_report")
public class FinancialReport {

    @Id
    private Long id;

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private Integer reportYear;
    private LocalDate noticeDate;
    private String currency;

    // 资产负债表
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal monetaryFunds;
    private BigDecimal accountsReceivable;
    private BigDecimal inventory;
    private BigDecimal totalCurrentAssets;
    private BigDecimal totalNoncurrentAssets;
    private BigDecimal totalCurrentLiabilities;
    private BigDecimal totalNoncurrentLiabilities;

    // 利润表
    private BigDecimal totalRevenue;
    private BigDecimal operateIncome;
    private BigDecimal operateCost;
    private BigDecimal saleExpense;
    private BigDecimal manageExpense;
    private BigDecimal researchExpense;
    private BigDecimal financeExpense;
    private BigDecimal operateProfit;
    private BigDecimal totalProfit;
    private BigDecimal netProfit;
    private BigDecimal parentNetProfit;

    // 现金流量表
    private BigDecimal operatingCashFlow;
    private BigDecimal investingCashFlow;
    private BigDecimal financingCashFlow;
    private BigDecimal cceAdd;
    private BigDecimal endCce;

    // JSONB 完整数据
    private Map<String, Object> balanceSheet;
    private Map<String, Object> profitSheet;
    private Map<String, Object> cashFlowSheet;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3.2 Repository 接口

```java
package com.example.securityanalyze.finance.domain;

import java.util.List;
import java.util.Optional;

public interface FinancialReportRepository {

    List<FinancialReport> findByStockCode(String stockCode);

    List<FinancialReport> findByStockCodeAndYear(String stockCode, int year);

    Optional<FinancialReport> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate);

    void save(FinancialReport report);

    void saveAll(List<FinancialReport> reports);

    boolean existsByStockCodeAndReportDate(String stockCode, LocalDate reportDate);
}
```

### 3.3 Service 层

```java
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinancialReportRepository reportRepository;

    /**
     * 获取某公司的全部财务报告列表（按报告期倒序）
     */
    public List<FinanceReportListItem> listReports(String stockCode) {
        // ...
    }

    /**
     * 获取单份财务报告详情（含完整 JSONB 数据）
     */
    public Optional<FinanceReportResponse> getReportDetail(Long reportId) {
        // ...
    }

    /**
     * 获取核心财务指标趋势数据（用于 ECharts 图表）
     */
    public FinanceIndicatorResponse getIndicators(String stockCode, List<String> reportTypes) {
        // ...
    }

    /**
     * 计算衍生财务指标
     * - 毛利率 = (营业收入 - 营业成本) / 营业收入
     * - 净利率 = 净利润 / 营业收入
     * - 资产负债率 = 总负债 / 总资产
     * - ROE = 归母净利润 / 平均净资产
     */
    public List<DerivedIndicator> calculateDerivedIndicators(String stockCode) {
        // ...
    }
}
```

### 3.4 API 接口契约

#### 3.4.1 获取财务报告列表

```
GET /api/finance/{stockCode}/reports
```

**响应**

```json
{
  "stockCode": "600519",
  "stockName": "贵州茅台",
  "items": [
    {
      "id": 1,
      "reportDate": "2025-12-31",
      "reportType": "年报",
      "reportYear": 2025,
      "noticeDate": "2026-04-17",
      "totalRevenue": 174150686600.00,
      "netProfit": 86276033100.00,
      "parentNetProfit": 86276033100.00,
      "totalAssets": 298900000000.00,
      "totalEquity": 261600000000.00
    }
  ]
}
```

#### 3.4.2 获取单份报告详情

```
GET /api/finance/reports/{reportId}
```

**响应**

```json
{
  "id": 1,
  "stockCode": "600519",
  "reportDate": "2025-12-31",
  "reportType": "年报",
  "reportYear": 2025,
  "noticeDate": "2026-04-17",
  "currency": "CNY",

  "summary": {
    "totalAssets": 298900000000.00,
    "totalLiabilities": 37300000000.00,
    "totalEquity": 261600000000.00,
    "totalRevenue": 174150686600.00,
    "operateCost": 14916000000.00,
    "operateProfit": 119660000000.00,
    "netProfit": 86276033100.00,
    "parentNetProfit": 86276033100.00,
    "operatingCashFlow": 93340000000.00
  },

  "balanceSheet": { /* JSONB 完整数据 */ },
  "profitSheet": { /* JSONB 完整数据 */ },
  "cashFlowSheet": { /* JSONB 完整数据 */ }
}
```

#### 3.4.3 获取核心指标趋势

```
GET /api/finance/{stockCode}/indicators?metrics=totalRevenue,netProfit,grossMargin,netMargin,roe
```

**响应**

```json
{
  "stockCode": "600519",
  "metrics": [
    {
      "metric": "totalRevenue",
      "label": "营业总收入",
      "unit": "元",
      "data": [
        { "reportDate": "2023-12-31", "value": 150560000000.00 },
        { "reportDate": "2024-12-31", "value": 162500000000.00 },
        { "reportDate": "2025-12-31", "value": 174150686600.00 }
      ]
    },
    {
      "metric": "grossMargin",
      "label": "毛利率",
      "unit": "%",
      "data": [
        { "reportDate": "2023-12-31", "value": 91.54 },
        { "reportDate": "2024-12-31", "value": 91.76 },
        { "reportDate": "2025-12-31", "value": 91.44 }
      ]
    }
  ]
}
```

---

## 四、前端设计

### 4.1 页面结构

```
公司详情页（/companies/:stockCode）
├── 基本信息 Tab
├── 关联证券 Tab
├── 财务报告 Tab（新增）
│   ├── 顶部：报告期选择器（年份 + 报告类型筛选）
│   ├── 中部：核心指标趋势图（ECharts 折线/柱状图）
│   └── 底部：财务数据表格（三张报表 Tab 切换）
│       ├── 数据概览（摘要卡片）
│       ├── 资产负债表
│       ├── 利润表
│       └── 现金流量表
└── 历史变更 Tab
```

### 4.2 新增路由

无需新增路由，财务报告作为公司详情页的一个 Tab 页签存在。

### 4.3 新增组件

```
frontend/src/views/company/
├── CompanyDetailView.vue              -- 已有，增加财务报告 Tab
└── finance/
    ├── FinanceReportTab.vue           -- 财务报告 Tab 容器
    ├── ReportPeriodSelector.vue       -- 报告期选择器
    ├── IndicatorChart.vue             -- ECharts 趋势图组件
    ├── ReportSummaryCards.vue         -- 核心指标摘要卡片
    ├── BalanceSheetTable.vue          -- 资产负债表表格
    ├── ProfitSheetTable.vue           -- 利润表表格
    └── CashFlowSheetTable.vue         -- 现金流量表表格
```

### 4.4 TypeScript 类型

```typescript
// types/finance.ts

export type ReportType = '年报' | '中报' | '一季报' | '三季报'

export interface FinanceReportItem {
  id: number
  reportDate: string
  reportType: ReportType
  reportYear: number
  noticeDate?: string
  totalRevenue?: number
  netProfit?: number
  parentNetProfit?: number
  totalAssets?: number
  totalEquity?: number
}

export interface FinanceReportList {
  stockCode: string
  stockName: string
  items: FinanceReportItem[]
}

export interface FinanceReportDetail {
  id: number
  stockCode: string
  reportDate: string
  reportType: ReportType
  reportYear: number
  noticeDate?: string
  currency: string
  summary: FinanceSummary
  balanceSheet?: Record<string, any>
  profitSheet?: Record<string, any>
  cashFlowSheet?: Record<string, any>
}

export interface FinanceSummary {
  totalAssets?: number
  totalLiabilities?: number
  totalEquity?: number
  totalRevenue?: number
  operateCost?: number
  operateProfit?: number
  netProfit?: number
  parentNetProfit?: number
  operatingCashFlow?: number
}

export interface IndicatorDataPoint {
  reportDate: string
  value: number
}

export interface IndicatorMetric {
  metric: string
  label: string
  unit: string
  data: IndicatorDataPoint[]
}

export interface IndicatorResponse {
  stockCode: string
  metrics: IndicatorMetric[]
}
```

### 4.5 API 封装

```typescript
// api/finance.ts
import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

export async function getFinanceReports(stockCode: string) {
  const res = await client.get(`/finance/${stockCode}/reports`)
  return res.data
}

export async function getFinanceReportDetail(reportId: number) {
  const res = await client.get(`/finance/reports/${reportId}`)
  return res.data
}

export async function getFinanceIndicators(
  stockCode: string,
  metrics: string[]
) {
  const res = await client.get(`/finance/${stockCode}/indicators`, {
    params: { metrics: metrics.join(',') },
  })
  return res.data
}
```

---

## 五、数据采集设计

### 5.1 数据源映射

| 报表 | akshare 接口 | 说明 |
|------|-------------|------|
| 资产负债表 | `stock_balance_sheet_by_report_em(symbol='SH600519')` | 东方财富，按报告期 |
| 利润表 | `stock_profit_sheet_by_report_em(symbol='SH600519')` | 东方财富，按报告期 |
| 现金流量表 | `stock_cash_flow_sheet_by_report_em(symbol='SH600519')` | 东方财富，按报告期 |

> 注：`symbol` 参数需带市场前缀，如 `SH600519`、`SZ000001`、`BJ430047`。

### 5.2 采集任务

```python
# collector/tasks/finance_task.py

class FinanceTask:
    """采集财务报告数据任务"""

    def __init__(self, db: PostgresDB, source: AkshareSource):
        self.db = db
        self.source = source

    def run(self):
        """全量采集所有 A 股公司的财务报告"""
        pass  # TODO: 实现

    def run_by_stock_code(self, stock_code: str, market: str = "SH"):
        """按股票代码采集指定公司的全部财务报告"""
        pass  # TODO: 实现

    def _parse_report(self, row: pd.Series, sheet_type: str) -> Dict[str, Any]:
        """解析单条报表记录"""
        pass  # TODO: 实现

    def _extract_core_metrics(self, balance_df, profit_df, cashflow_df) -> Dict[str, Any]:
        """从三张 DataFrame 中提取核心指标"""
        pass  # TODO: 实现
```

### 5.3 核心指标映射（akshare → DB）

| DB 字段 | 资产负债表字段 | 利润表字段 | 现金流量表字段 |
|---------|--------------|-----------|--------------|
| `total_assets` | `ASSET_BALANCE` | — | — |
| `total_liabilities` | `LIAB_BALANCE` | — | — |
| `total_equity` | `EQUITY_BALANCE` | — | — |
| `monetary_funds` | `MONETARYFUNDS` | — | — |
| `accounts_receivable` | `ACCOUNTS_RECE` | — | — |
| `inventory` | `INVENTORY` | — | — |
| `total_current_assets` | `CURRENT_ASSET_BALANCE` | — | — |
| `total_noncurrent_assets` | `NONCURRENT_ASSET_BALANCE` | — | — |
| `total_current_liabilities` | `CURRENT_LIAB_BALANCE` | — | — |
| `total_noncurrent_liabilities` | `NONCURRENT_LIAB_BALANCE` | — | — |
| `total_revenue` | — | `TOTAL_OPERATE_INCOME` | — |
| `operate_income` | — | `OPERATE_INCOME` | — |
| `operate_cost` | — | `OPERATE_COST` | — |
| `sale_expense` | — | `SALE_EXPENSE` | — |
| `manage_expense` | — | `MANAGE_EXPENSE` | — |
| `research_expense` | — | `RESEARCH_EXPENSE` | — |
| `finance_expense` | — | `FINANCE_EXPENSE` | — |
| `operate_profit` | — | `OPERATE_PROFIT` | — |
| `total_profit` | — | `TOTAL_PROFIT` | — |
| `net_profit` | — | `NETPROFIT` | — |
| `parent_net_profit` | — | `PARENT_NETPROFIT` | — |
| `operating_cash_flow` | — | — | `NETCASH_OPERATE` |
| `investing_cash_flow` | — | — | `NETCASH_INVEST` |
| `financing_cash_flow` | — | — | `NETCASH_FINANCE` |
| `cce_add` | — | — | `CCE_ADD` |
| `end_cce` | — | — | `END_CCE` |

### 5.4 去重策略

采集时以 `(stock_code, report_date)` 为唯一键：
- **存在**：更新 `notice_date`、核心指标、JSONB 原始数据、`updated_at`
- **不存在**：插入新记录

```python
# 伪代码
if exists(stock_code, report_date):
    update_report(stock_code, report_date, data)
else:
    insert_report(data)
```

---

## 六、前端交互流程

```
用户点击「财务报告」Tab
    │
    ▼
调用 GET /api/finance/{stockCode}/reports
    │
    ▼
渲染报告期列表（左侧或顶部）
    │
    ▼
默认选中最近一期报告
    │
    ├──► 调用 GET /api/finance/reports/{reportId}
    │       渲染摘要卡片 + 三张报表 Tab
    │
    └──► 调用 GET /api/finance/{stockCode}/indicators
            渲染 ECharts 趋势图
```

---

## 七、实施计划（分阶段）

### 阶段一：数据库与后端基础设施（1-2 天）

- [ ] 编写 `V3__create_financial_report_table.sql` 迁移脚本
- [ ] 后端新增 `FinancialReport` 实体、`FinancialReportRepository` 接口
- [ ] 实现 `FinancialReportRepositoryImpl`（JDBC）
- [ ] 实现 `FinanceService`：列表查询、详情查询、指标趋势查询
- [ ] 实现 `FinanceController`：三个 RESTful 接口
- [ ] 后端单元测试

### 阶段二：数据采集（1-2 天）

- [ ] `AkshareSource` 新增三张报表的采集方法
- [ ] `FinanceTask` 实现 `_parse_report`、`_extract_core_metrics`
- [ ] `FinanceTask.run_by_stock_code()` 完整实现
- [ ] 手动运行采集任务，验证数据正确性
- [ ] 采集日志与异常处理

### 阶段三：前端页面（2-3 天）

- [ ] 新增 `types/finance.ts` 类型定义
- [ ] 新增 `api/finance.ts` API 封装
- [ ] 实现 `FinanceReportTab.vue` 容器组件
- [ ] 实现 `ReportPeriodSelector.vue`
- [ ] 实现 `IndicatorChart.vue`（ECharts 折线图）
- [ ] 实现 `ReportSummaryCards.vue`
- [ ] 实现三张报表表格组件
- [ ] 更新 `CompanyDetailView.vue`，接入财务报告 Tab
- [ ] 联调测试

### 阶段四：集成与优化（1 天）

- [ ] 前后端联调
- [ ] 边界测试：无数据、数据缺失、NaN 处理
- [ ] 性能测试：大数据量列表查询
- [ ] 更新项目进度文档

---

## 八、风险与注意事项

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| akshare 接口变更 | 采集失败 | 采集代码封装在 `AkshareSource`，变更时只需修改一处 |
| 财务报表字段极多 | 存储冗余、查询慢 | 核心指标结构化 + 完整数据 JSONB，按需查询 |
| 报告期格式不统一 | 数据解析错误 | 统一使用 `YYYY-MM-DD` 格式，report_type 枚举校验 |
| 同一家公司多证券 | 数据重复采集 | 以 `stock_code` 为维度采集，不同证券独立存储 |
| NaN / None 值处理 | 前端展示异常 | 后端统一将空值转为 `null`，前端展示 `-` |

---

## 九、修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-04-30 | v1.0 | 初始版本，定义数据模型、后端 API、前端组件、采集策略 |
