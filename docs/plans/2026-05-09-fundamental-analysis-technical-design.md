# 投研分析之基本面分析模块 — 详细技术设计方案

> 本文档基于 `2026-05-09-fundamental-analysis-design.md` 高层设计，细化到具体的类定义、接口签名、SQL 查询、前端组件结构与 API 契约。  
> 版本：v1.0  
> 日期：2026-05-09

---

## 〇、简版 vs 完整版边界说明

本文档为 **投研分析独立页面（完整版）** 的详细技术方案。公司详情页 Tab（简版）的技术方案见 `2026-05-09-company-info-update-technical-design.md`。

| 维度 | 简版（公司详情 Tab） | 完整版（本文档） |
|------|---------------------|-----------------|
| 后端接口 | 共用 `/research/fundamental/overview/{stockCode}` | 本文档定义 `/overview`、`/screen`、`/industry-peers` |
| 前端组件 | `FundamentalAnalysisTab.vue`（公司详情内嵌） | `FundamentalAnalysisView.vue` + 左侧边栏 + 同行业对比 |
| 图表组件 | 复用本文档定义的图表组件 | 本文档定义全部图表组件 |
| 独有功能 | 无 | 股票筛选、同行业速览、对比池预留 |

---

## 一、后端详细设计

### 1.1 模块结构

```
backend/src/main/java/com/example/securityanalyze/research/
├── api/
│   ├── ResearchController.java
│   ├── FundamentalOverviewResponse.java
│   ├── FundamentalScreenRequest.java
│   ├── FundamentalScreenResponse.java
│   ├── ScreenCompanyItem.java
│   └── IndustryPeersResponse.java
├── application/
│   └── FundamentalAnalysisService.java
├── domain/
│   ├── FundamentalMetrics.java
│   ├── AnnualMetric.java
│   └── FundamentalMetricsRepository.java
└── infrastructure/
    └── FundamentalMetricsRepositoryImpl.java
```

### 1.2 Domain 实体

#### AnnualMetric.java

```java
package com.example.securityanalyze.research.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AnnualMetric {
    private LocalDate reportDate;
    private Integer reportYear;

    // 盈利能力
    private BigDecimal totalRevenue;
    private BigDecimal operateIncome;
    private BigDecimal operateCost;
    private BigDecimal parentNetProfit;
    private BigDecimal grossMargin;
    private BigDecimal netMargin;
    private BigDecimal roe;

    // 资产负债
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal totalCurrentAssets;
    private BigDecimal totalNoncurrentAssets;
    private BigDecimal debtRatio;

    // 现金流
    private BigDecimal operatingCashFlow;
    private BigDecimal investingCashFlow;
    private BigDecimal financingCashFlow;
    private BigDecimal endCce;
    private BigDecimal cashflowProfitRatio;

    // 成本费用（阶段A新增）
    private BigDecimal saleExpense;
    private BigDecimal manageExpense;
    private BigDecimal researchExpense;
    private BigDecimal financeExpense;
    private BigDecimal periodExpenseRate;
}
```

#### FundamentalMetrics.java

```java
package com.example.securityanalyze.research.domain;

import lombok.Data;
import java.util.List;

@Data
public class FundamentalMetrics {
    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private List<AnnualMetric> annualMetrics;
}
```

#### FundamentalMetricsRepository.java

```java
package com.example.securityanalyze.research.domain;

import java.util.List;
import java.util.Optional;

public interface FundamentalMetricsRepository {
    Optional<FundamentalMetrics> findByStockCode(String stockCode, int years);
    List<ScreenCompanyItem> screenCompanies(String keyword, String industry, String market, int offset, int limit);
    long countScreenCompanies(String keyword, String industry, String market);
    List<PeerMetric> findIndustryPeers(String stockCode);
}
```

### 1.3 Repository 实现

#### FundamentalMetricsRepositoryImpl.java

核心查询 SQL — 单公司基本面概览：

```java
private static final String OVERVIEW_SQL = """
    SELECT
        fr.report_date,
        fr.report_year,
        fr.total_revenue,
        fr.operate_income,
        fr.operate_cost,
        fr.parent_net_profit,
        fr.total_assets,
        fr.total_liabilities,
        fr.total_equity,
        fr.total_current_assets,
        fr.total_noncurrent_assets,
        fr.operating_cash_flow,
        fr.investing_cash_flow,
        fr.financing_cash_flow,
        fr.end_cce,
        fr.sale_expense,
        fr.manage_expense,
        fr.research_expense,
        fr.finance_expense,
        cs.stock_name,
        c.industry,
        cs.market
    FROM financial_report fr
    JOIN company_security cs ON fr.stock_code = cs.stock_code
    JOIN company c ON cs.company_id = c.id
    WHERE fr.stock_code = :stockCode
      AND fr.report_type = '年报'
      AND fr.report_date >= :startDate
      AND fr.is_deleted = FALSE
      AND cs.is_deleted = FALSE
      AND c.is_deleted = FALSE
    ORDER BY fr.report_date ASC
    """;
```

核心查询 SQL — 股票筛选：

```java
private static final String SCREEN_SQL = """
    SELECT DISTINCT ON (cs.stock_code)
        cs.stock_code,
        cs.stock_name,
        c.industry,
        cs.market,
        fr.total_revenue,
        fr.parent_net_profit
    FROM company_security cs
    JOIN company c ON cs.company_id = c.id
    LEFT JOIN financial_report fr ON cs.stock_code = fr.stock_code
        AND fr.report_type = '年报'
        AND fr.is_deleted = FALSE
    WHERE cs.is_deleted = FALSE
      AND c.is_deleted = FALSE
    """;
```

筛选条件动态拼接（keyword / industry / market）：

```java
// keyword: 匹配 stock_code 精确 或 stock_name 前缀
if (keyword != null && !keyword.isBlank()) {
    sql += " AND (cs.stock_code = :keyword OR cs.stock_name ILIKE :prefix)";
}
// industry: 精确匹配
if (industry != null && !industry.isBlank()) {
    sql += " AND c.industry = :industry";
}
// market: 精确匹配
if (market != null && !market.isBlank()) {
    sql += " AND cs.market = :market";
}

sql += " ORDER BY cs.stock_code ASC LIMIT :limit OFFSET :offset";
```

核心查询 SQL — 同行业对比：

```java
private static final String PEERS_SQL = """
    WITH target_company AS (
        SELECT c.industry
        FROM company_security cs
        JOIN company c ON cs.company_id = c.id
        WHERE cs.stock_code = :stockCode
          AND cs.is_deleted = FALSE
          AND c.is_deleted = FALSE
    )
    SELECT DISTINCT ON (cs.stock_code)
        cs.stock_code,
        cs.stock_name,
        c.industry,
        fr.total_revenue,
        fr.parent_net_profit,
        CASE WHEN fr.total_equity IS NOT NULL AND fr.total_equity > 0
             THEN fr.parent_net_profit / fr.total_equity * 100
             ELSE NULL
        END as roe,
        CASE WHEN fr.total_assets IS NOT NULL AND fr.total_assets > 0
             THEN fr.total_liabilities / fr.total_assets * 100
             ELSE NULL
        END as debt_ratio
    FROM company_security cs
    JOIN company c ON cs.company_id = c.id
    JOIN target_company tc ON c.industry = tc.industry
    LEFT JOIN financial_report fr ON cs.stock_code = fr.stock_code
        AND fr.report_type = '年报'
        AND fr.is_deleted = FALSE
    WHERE cs.stock_code != :stockCode
      AND cs.is_deleted = FALSE
      AND c.is_deleted = FALSE
    ORDER BY cs.stock_code, fr.report_date DESC
    """;
```

### 1.4 Service 层

#### FundamentalAnalysisService.java

```java
@Service
@RequiredArgsConstructor
public class FundamentalAnalysisService {

    private final FundamentalMetricsRepository fundamentalMetricsRepository;

    public Optional<FundamentalOverviewResponse> getOverview(String stockCode) {
        Optional<FundamentalMetrics> metricsOpt =
            fundamentalMetricsRepository.findByStockCode(stockCode, 5);
        return metricsOpt.map(this::toOverviewResponse);
    }

    public FundamentalScreenResponse screenCompanies(
            String keyword, String industry, String market, int page, int size) {
        int offset = page * size;
        List<ScreenCompanyItem> items = fundamentalMetricsRepository
            .screenCompanies(keyword, industry, market, offset, size);
        long total = fundamentalMetricsRepository
            .countScreenCompanies(keyword, industry, market);
        // 组装分页响应...
    }

    public IndustryPeersResponse getIndustryPeers(String stockCode) {
        List<PeerMetric> peers = fundamentalMetricsRepository.findIndustryPeers(stockCode);
        return new IndustryPeersResponse(peers);
    }

    private FundamentalOverviewResponse toOverviewResponse(FundamentalMetrics metrics) {
        // 遍历 annualMetrics，计算毛利率/净利率/ROE/资产负债率/期间费用率/现金流比率
        // 注意空值和除零保护
    }
}
```

**关键计算逻辑（Service 层组装时）**：

```java
// 毛利率 = (operate_income - operate_cost) / operate_income * 100
BigDecimal grossMargin = safeDivide(
    income.subtract(cost), income, 4
).multiply(BigDecimal.valueOf(100));

// 净利率 = parent_net_profit / operate_income * 100
BigDecimal netMargin = safeDivide(
    profit, income, 4
).multiply(BigDecimal.valueOf(100));

// ROE = parent_net_profit / total_equity * 100
BigDecimal roe = safeDivide(
    profit, equity, 4
).multiply(BigDecimal.valueOf(100));

// 资产负债率 = total_liabilities / total_assets * 100
BigDecimal debtRatio = safeDivide(
    liabilities, assets, 4
).multiply(BigDecimal.valueOf(100));

// 期间费用率 = (sale + manage + research + finance) / operate_income * 100
BigDecimal periodExpenseRate = safeDivide(
    saleExpense.add(manageExpense).add(researchExpense).add(financeExpense),
    income, 4
).multiply(BigDecimal.valueOf(100));

// 经营现金流/净利润比
BigDecimal cashflowProfitRatio = safeDivide(
    operatingCashFlow, profit, 4
).multiply(BigDecimal.valueOf(100));

private static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator, int scale) {
    if (numerator == null || denominator == null ||
        denominator.compareTo(BigDecimal.ZERO) == 0) {
        return null;
    }
    return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
}
```

### 1.5 Controller 与 DTO

#### ResearchController.java

```java
@RestController
@RequestMapping("/api/research")
@RequiredArgsConstructor
public class ResearchController {

    private final FundamentalAnalysisService fundamentalAnalysisService;

    @GetMapping("/fundamental/overview/{stockCode}")
    public ResponseEntity<FundamentalOverviewResponse> getOverview(
            @PathVariable String stockCode) {
        return fundamentalAnalysisService.getOverview(stockCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/fundamental/screen")
    public ResponseEntity<FundamentalScreenResponse> screenCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // normalize page/size...
        FundamentalScreenResponse response = fundamentalAnalysisService
            .screenCompanies(keyword, industry, market, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fundamental/industry-peers/{stockCode}")
    public ResponseEntity<IndustryPeersResponse> getIndustryPeers(
            @PathVariable String stockCode) {
        return ResponseEntity.ok(
            fundamentalAnalysisService.getIndustryPeers(stockCode)
        );
    }
}
```

#### FundamentalOverviewResponse.java

```java
@Data
public class FundamentalOverviewResponse {
    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private List<AnnualMetricDto> metrics;
}

@Data
public class AnnualMetricDto {
    private String reportDate;
    private Integer reportYear;
    // 盈利能力
    private BigDecimal totalRevenue;
    private BigDecimal operateIncome;
    private BigDecimal operateCost;
    private BigDecimal parentNetProfit;
    private BigDecimal grossMargin;
    private BigDecimal netMargin;
    private BigDecimal roe;
    // 资产负债
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal debtRatio;
    // 现金流
    private BigDecimal operatingCashFlow;
    private BigDecimal investingCashFlow;
    private BigDecimal financingCashFlow;
    private BigDecimal endCce;
    private BigDecimal cashflowProfitRatio;
    // 成本费用
    private BigDecimal saleExpense;
    private BigDecimal manageExpense;
    private BigDecimal researchExpense;
    private BigDecimal financeExpense;
    private BigDecimal periodExpenseRate;
}
```

#### ScreenCompanyItem.java

```java
@Data
public class ScreenCompanyItem {
    private String stockCode;
    private String stockName;
    private String industry;
    private String market;
    private BigDecimal latestRevenue;
    private BigDecimal latestProfit;
}
```

---

## 二、前端详细设计

### 2.1 新增目录结构

```
frontend/src/
├── api/research.ts                    # 新增：投研分析 API 封装
├── types/research.ts                  # 新增：投研分析类型定义
├── views/research/
│   ├── FundamentalAnalysisView.vue    # 独立投研分析完整版页面
│   └── components/
│       ├── FundamentalScreener.vue    # 左侧筛选/搜索/公司列表
│       ├── CompanyInfoHeader.vue      # 公司信息栏
│       ├── MetricDashboard.vue        # 8张核心指标卡片
│       ├── ProfitabilityChart.vue     # 盈利能力全景图
│       ├── CostExpenseChart.vue       # 成本费用结构图（新增）
│       ├── BalanceSheetChart.vue      # 资产负债结构图
│       ├── CashFlowChart.vue          # 现金流全景图
│       ├── DupontPlaceholder.vue      # 杜邦分析占位（阶段B）
│       └── IndustryPeersTable.vue     # 同行业公司速览表
└── views/company/
    └── fundamental/
        └── FundamentalAnalysisTab.vue  # 公司详情页精简版 Tab
```

### 2.2 API 封装

#### `src/api/research.ts`

```typescript
import { client } from './axios'
import type {
  FundamentalOverview,
  ScreenParams,
  ScreenResponse,
  IndustryPeersResponse,
} from '@/types/research'

export async function getFundamentalOverview(
  stockCode: string
): Promise<FundamentalOverview> {
  const response = await client.get(`/research/fundamental/overview/${stockCode}`)
  return response.data
}

export async function screenCompanies(
  params: ScreenParams
): Promise<ScreenResponse> {
  const response = await client.get('/research/fundamental/screen', { params })
  return response.data
}

export async function getIndustryPeers(
  stockCode: string
): Promise<IndustryPeersResponse> {
  const response = await client.get(`/research/fundamental/industry-peers/${stockCode}`)
  return response.data
}
```

### 2.3 类型定义

#### `src/types/research.ts`

```typescript
export interface AnnualMetric {
  reportDate: string
  reportYear: number
  // 盈利能力
  totalRevenue: number
  operateIncome: number
  operateCost: number
  parentNetProfit: number
  grossMargin: number
  netMargin: number
  roe: number
  // 资产负债
  totalAssets: number
  totalLiabilities: number
  totalEquity: number
  debtRatio: number
  // 现金流
  operatingCashFlow: number
  investingCashFlow: number
  financingCashFlow: number
  endCce: number
  cashflowProfitRatio: number
  // 成本费用
  saleExpense: number
  manageExpense: number
  researchExpense: number
  financeExpense: number
  periodExpenseRate: number
}

export interface FundamentalOverview {
  stockCode: string
  stockName: string
  industry: string
  market: string
  metrics: AnnualMetric[]
}

export interface ScreenParams {
  keyword?: string
  industry?: string
  market?: string
  page?: number
  size?: number
}

export interface ScreenCompanyItem {
  stockCode: string
  stockName: string
  industry: string
  market: string
  latestRevenue?: number
  latestProfit?: number
}

export interface ScreenResponse {
  items: ScreenCompanyItem[]
  total: number
  page: number
  size: number
}

export interface PeerMetric {
  stockCode: string
  stockName: string
  industry: string
  totalRevenue?: number
  parentNetProfit?: number
  roe?: number
  debtRatio?: number
}

export interface IndustryPeersResponse {
  peers: PeerMetric[]
}
```

### 2.4 公司详情页精简版 Tab

#### `src/views/company/fundamental/FundamentalAnalysisTab.vue`

**组件职责**：只负责单公司近5年年报数据的可视化展示，不处理搜索/筛选。

**Props**：
```typescript
defineProps<{
  stockCode: string
}>()
```

**内部状态**：
```typescript
const loading = ref(false)
const overview = ref<FundamentalOverview | null>(null)
```

**布局**（与高层设计一致）：
1. `MetricDashboard.vue` — 8张核心指标卡片（取最新年报数据）
2. `ProfitabilityChart.vue` — 盈利能力趋势（柱状：营收/净利润/营业成本 + 折线：毛利率/净利率）
3. `CostExpenseChart.vue` — 成本费用结构（堆叠柱状：四费 + 折线：期间费用率）
4. `BalanceSheetChart.vue` — 资产负债结构
5. `CashFlowChart.vue` — 现金流健康度
6. `ElCollapse` 折叠面板 — 近5年数据一览表

**数据获取**：
```typescript
async function fetchData() {
  loading.value = true
  try {
    overview.value = await getFundamentalOverview(props.stockCode)
  } catch (err) {
    ElMessage.error('加载基本面数据失败')
  } finally {
    loading.value = false
  }
}
```

### 2.5 独立投研分析完整版页面

#### `src/views/research/FundamentalAnalysisView.vue`

**布局：CSS Grid 左右分栏**

```vue
<template>
  <div class="research-layout">
    <!-- 左侧边栏 -->
    <aside class="research-sidebar">
      <FundamentalScreener
        v-model:selected-stock="selectedStock"
        @select="onSelectCompany"
      />
    </aside>

    <!-- 右侧主区域 -->
    <main class="research-main">
      <template v-if="selectedStock">
        <CompanyInfoHeader :overview="overview" />
        <MetricDashboard :latest-metric="latestMetric" />
        <div class="charts-container">
          <ProfitabilityChart :metrics="overview.metrics" />
          <CostExpenseChart :metrics="overview.metrics" />
          <BalanceSheetChart :metrics="overview.metrics" />
          <CashFlowChart :metrics="overview.metrics" />
          <DupontPlaceholder />
        </div>
        <ElCollapse v-model="activeCollapse">
          <ElCollapseItem title="同行业公司速览" name="peers">
            <IndustryPeersTable :stock-code="selectedStock" />
          </ElCollapseItem>
        </ElCollapse>
      </template>
      <ResearchEmptyState v-else />
    </main>
  </div>
</template>
```

**样式**：
```css
.research-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  height: calc(100vh - 64px); /* 减去顶部导航高度 */
  overflow: hidden;
}
.research-sidebar {
  overflow-y: auto;
  border-right: 1px solid var(--border-color);
  padding: 16px;
}
.research-main {
  overflow-y: auto;
  padding: 16px;
}
.charts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-top: 24px;
}
```

### 2.6 图表组件设计

所有图表组件遵循统一的 Props 接口：

```typescript
// 通用图表数据接口
interface ChartProps {
  metrics: AnnualMetric[]  // 按 reportDate 升序排列的年报指标数组
}
```

#### ProfitabilityChart.vue

- **类型**：双 Y 轴混合图（`bar` + `line`）
- **系列**：
  - 左轴（金额，柱状）：`totalRevenue`、`parentNetProfit`、`operateCost`
  - 右轴（百分比，折线）：`grossMargin`、`netMargin`、`roe`
- **颜色**：营收蓝、净利润绿、成本红；毛利率橙、净利率紫、ROE 青
- **Tooltip**：金额格式化为"亿/万"，百分比保留2位小数

#### CostExpenseChart.vue

- **类型**：堆叠柱状图（`stack`）+ 单折线
- **系列**：
  - 堆叠柱状（左轴，金额）：`saleExpense`、`manageExpense`、`researchExpense`、`financeExpense`
  - 折线（右轴，百分比）：`periodExpenseRate`
- **颜色**：四费使用同色系不同深浅（蓝系）

#### BalanceSheetChart.vue

- **类型**：堆叠柱状图 + 单折线
- **系列**：
  - 堆叠柱状（左轴，金额）：`totalCurrentAssets`、`totalNoncurrentAssets`（资产侧）；`totalCurrentLiabilities`、`totalNoncurrentLiabilities`（负债侧，用负值或不同颜色）
  - 折线（右轴，百分比）：`debtRatio`

#### CashFlowChart.vue

- **类型**：分组柱状图 + 双折线
- **系列**：
  - 分组柱状（左轴，金额）：`operatingCashFlow`、`investingCashFlow`、`financingCashFlow`
  - 折线（右轴）：`endCce`（期末现金余额，金额）、`cashflowProfitRatio`（现金流/净利润比，百分比）

---

## 三、数据库查询索引优化

阶段A新增的查询模式对现有索引的依赖：

| 查询场景 | 现有索引 | 是否够用 |
|----------|----------|----------|
| `financial_report` 按 `stock_code + report_type + report_date` | `idx_fin_report_stock_code` | ✅ 够用，但建议追加复合索引 |
| `financial_report` 按 `stock_code + report_type + report_date` 取最新一条 | 无直接覆盖 | ⚠️ 建议新增 `idx_fin_report_stock_type_date` |

**建议新增索引**（`V2__fundamental_analysis_indexes.sql`）：

```sql
-- 基本面分析常用查询：按股票代码+报告类型+报告日期范围查询
CREATE INDEX IF NOT EXISTS idx_fin_report_stock_type_date
    ON financial_report(stock_code, report_type, report_date);

-- 筛选查询：按行业+市场快速定位公司
CREATE INDEX IF NOT EXISTS idx_company_industry
    ON company(industry) WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_cs_market
    ON company_security(market) WHERE is_deleted = FALSE;
```

---

## 四、错误处理与边界情况

| 场景 | 前端表现 | 后端处理 |
|------|----------|----------|
| 公司无年报数据 | `ResearchEmptyState` 显示"暂无年报数据" | `findByStockCode` 返回空列表，Service 组装为空 `Optional` |
| 某年度部分指标为 NULL | ECharts 该数据点不绘制 | Repository 直接返回 NULL，Service 计算时 `safeDivide` 返回 NULL |
| 除数为零 | Tooltip 显示"-" | `safeDivide` 返回 NULL，DTO 字段为 NULL |
| 筛选结果为空 | 左侧列表显示"未找到匹配公司" | 正常返回空列表分页结构 |
| 同行业只有自己 | `IndustryPeersTable` 显示"暂无同行业对比数据" | SQL 中 `cs.stock_code != :stockCode` 自然过滤 |
| 后端接口 500 | `ElMessage.error("数据加载失败，请稍后重试")` + 重试按钮 | `GlobalExceptionHandler` 统一捕获，记录 ERROR 日志 |
| 后端接口 404 | 右侧显示"公司不存在或尚未入库" | Controller 返回 `404 Not Found` |

---

## 五、测试策略

### 5.1 后端单元测试

**Repository 集成测试**（继承 `RepositoryTestBase`）：

```java
@Import(FundamentalMetricsRepositoryImpl.class)
class FundamentalMetricsRepositoryTest extends RepositoryTestBase {

    @Autowired
    private FundamentalMetricsRepository repository;

    @Test
    void shouldReturnAnnualMetricsOrderedByDate() {
        // 插入测试数据：某公司3年年报
        // 调用 findByStockCode("TEST001", 5)
        // 断言：返回3条，按日期升序，字段计算正确
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        // 插入测试数据：某年度 total_equity = NULL
        // 断言：roe 字段为 NULL，不抛异常
    }

    @Test
    void shouldScreenCompaniesByIndustry() {
        // 插入多家公司，不同行业
        // 调用 screenCompanies(null, "白酒", null, 0, 20)
        // 断言：只返回白酒行业公司
    }
}
```

**Service 单元测试**：

```java
@ExtendWith(MockitoExtension.class)
class FundamentalAnalysisServiceTest {

    @Mock
    private FundamentalMetricsRepository repository;

    @InjectMocks
    private FundamentalAnalysisService service;

    @Test
    void shouldCalculateGrossMarginCorrectly() {
        // Mock 返回 operate_income=1000, operate_cost=600
        // 断言 grossMargin = 40.00
    }

    @Test
    void shouldReturnEmptyWhenCompanyNotFound() {
        // Mock 返回 Optional.empty()
        // 断言 service.getOverview 返回 Optional.empty()
    }
}
```

### 5.2 Controller 单元测试

```java
@WebMvcTest(ResearchController.class)
class ResearchControllerTest {

    @MockBean
    private FundamentalAnalysisService service;

    @Test
    void shouldReturn200WhenOverviewExists() throws Exception {
        mockMvc.perform(get("/api/research/fundamental/overview/600519"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.stockCode").value("600519"));
    }

    @Test
    void shouldReturn404WhenCompanyNotFound() throws Exception {
        mockMvc.perform(get("/api/research/fundamental/overview/999999"))
               .andExpect(status().isNotFound());
    }
}
```

---

## 六、与现有模块的集成点

| 集成点 | 说明 |
|--------|------|
| `finance` 模块 | 复用 `financial_report` 表数据，不修改 `finance` 的 API 和前端组件；基本面分析模块独立读取同一数据源 |
| `company` 模块 | 通过 `company_security` 和 `company` 表获取公司基本信息（名称、行业、市场），不依赖 `company` 模块的 Service |
| `industry` 模块 | 筛选条件下拉框复用 `industry_category` 数据，通过独立 SQL 查询获取 |
| 顶部导航 | 在 `App.vue` 或导航组件中新增"投研分析"菜单项，路由指向 `/research/fundamental` |

---

## 七、实施检查清单

### 后端
- [ ] 新建 `research/` package 及全部 Java 类
- [ ] `FundamentalMetricsRepositoryImpl` 实现3条核心 SQL
- [ ] `FundamentalAnalysisService` 实现空值保护和除零保护
- [ ] `ResearchController` 暴露3个 REST 接口
- [ ] 新增数据库索引脚本 `V2__fundamental_analysis_indexes.sql`
- [ ] 编写 Repository / Service / Controller 三层单元测试

### 前端
- [ ] 新增 `src/api/research.ts` 和 `src/types/research.ts`
- [ ] 新建 `src/views/research/` 目录及全部 Vue 组件
- [ ] 实现 `FundamentalAnalysisTab.vue` 并注册到公司详情页
- [ ] 实现 `FundamentalAnalysisView.vue` 并注册路由
- [ ] 顶部导航新增"投研分析"入口
- [ ] 5个图表组件统一适配暗色主题（与现有 `IndicatorChart.vue` 风格一致）

### 联调
- [ ] 验证 `/api/research/fundamental/overview/{stockCode}` 返回数据结构
- [ ] 验证筛选接口分页和条件过滤正确
- [ ] 验证同行业接口排除当前公司且数据正确
- [ ] 验证无数据场景的空态和错误态
