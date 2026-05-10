# 投研分析框架之基本面分析模块设计文档

> 本文档定义投研分析框架第一阶段——股票基本面分析模块的完整设计方案，涵盖阶段A（可视化看板）、阶段B（衍生指标与横向对比）、阶段C（估值分析）的渐进式演进路径。  
> 版本：v1.0  
> 日期：2026-05-09

---

## 一、设计目标与范围

### 1.1 目标
- 基于现有财务三表数据（资产负债表、利润表、现金流量表），构建股票基本面分析能力
- 支持单公司历史趋势可视化与同行业的横向对比
- 为后续估值分析（PE/PB/PS 分位、DCF 等）预留扩展接口

### 1.2 范围（阶段A）
- **覆盖市场**：A 股（沪深京）
- **数据周期**：近5年年报数据默认展示
- **数据源**：现有 `financial_report` 表（已采集数据）
- **不包含**：实时行情数据、K 线数据、人工录入/修改、多公司对比图表（阶段B）

### 1.3 设计原则
- **复用优先**：阶段A尽量复用现有 `finance` 模块的 API 和前端组件
- **分层清晰**：新增模块遵循后端 DDD 分层约定（`api/application/domain/infrastructure`）
- **渐进演进**：阶段A只展示和查询已有数据，阶段B引入计算逻辑和新表，阶段C引入估值模型

---

## 二、总体架构与模块划分

### 2.1 新增后端模块：`research/fundamental`
在 `backend/src/main/java/com/example/securityanalyze/` 下新增 `research` package，与 `finance/` 同级：

```
research/
├── api/
│   ├── FundamentalAnalysisController.java
│   ├── FundamentalOverviewResponse.java
│   ├── FundamentalScreenRequest.java
│   ├── FundamentalScreenResponse.java
│   └── IndustryPeersResponse.java
├── application/
│   └── FundamentalAnalysisService.java
├── domain/
│   ├── FundamentalMetrics.java
│   ├── MetricTrend.java
│   └── FundamentalMetricsRepository.java
└── infrastructure/
    └── FundamentalMetricsRepositoryImpl.java
```

### 2.2 新增前端目录：`src/views/research/`
```
views/research/
├── FundamentalAnalysisView.vue          # 独立投研分析完整版页面
├── components/
│   ├── FundamentalScreener.vue          # 股票筛选器（左侧边栏）
│   ├── CompanyInfoHeader.vue            # 公司信息栏
│   ├── MetricDashboard.vue              # 核心指标仪表盘（8张卡片）
│   ├── MetricComparisonChart.vue        # 多公司对比图表（阶段B启用）
│   └── IndustryPeersTable.vue           # 同行业公司速览表
```

### 2.3 公司详情页改造
在公司详情页（`CompanyDetailView.vue`）**新增**独立的"基本面分析" Tab，与现有 Tab 并存：
- **基本信息**
- **关联证券**
- **财务报告**（保持现有名称和功能不变）
- **基本面分析**（新增，阶段A精简版）
- **历史变更**

### 2.4 简版 vs 完整版差异对照

基本面分析功能在两个入口中以不同深度呈现：

| 维度 | 公司详情页 Tab（简版） | 独立投研页面（完整版） |
|------|----------------------|----------------------|
| **入口路径** | `/companies/:stockCode`（Tab 切换） | `/research/fundamental`（顶部导航进入） |
| **页面布局** | 垂直单栏，适配 Tab 内容区宽度 | 左右分栏（左侧筛选 320px + 右侧看板） |
| **指标卡片** | 8张卡片，1行4列 × 2行 | 8张卡片，2行4列，带颜色阈值提示 |
| **图表数量** | 4个（盈利能力/成本费用/资产负债/现金流） | 5个（上述4个 + 杜邦分析占位） |
| **图表高度** | 320px | 360px |
| **公司搜索** | ❌ 无（当前公司已确定） | ✅ 左侧全局搜索 + 行业/市场筛选 |
| **同行业对比** | ❌ 无 | ✅ 底部折叠区展示同行速览表 |
| **对比池** | ❌ 无 | 阶段B启用，阶段A预留 UI |
| **历史周期** | 近5年年报 | 近5年年报（阶段C可扩展至10年） |
| **数据获取** | 调用 `/research/fundamental/overview/{stockCode}` | 同一接口，组装方式相同 |
| **目标用户** | 快速浏览单公司基本面的用户 | 深度研究、横向对比的投研人员 |

> **设计原则**：简版与完整版共享同一套后端接口（`/overview`）和图表组件，区别仅在于页面布局、周边功能（搜索/筛选/对比）和图表数量。简版可视为完整版去掉左侧边栏和同行业对比后的纵向排列。

### 2.5 路由规划
| 页面 | 路径 | 说明 |
|------|------|------|
| 公司详情 | `/companies/:stockCode` | 新增"基本面分析" Tab |
| 独立投研 | `/research/fundamental` | 新增顶级导航"投研分析" → "基本面分析" |

---

## 三、公司详情页"基本面分析" Tab（阶段A精简版）

### 3.1 布局结构
采用**上卡下图**的垂直布局，总高度控制在 2-3 屏以内。

**第一层：核心指标卡片区（1行5列）**
复用并扩展 `ReportSummaryCards`，在现有5张卡片基础上增加：
- 毛利率（=`(operate_income - operate_cost) / operate_income`）
- 净利率（=`net_profit / operate_income`）
- ROE（=`parent_net_profit / total_equity`，阶段B预计算，阶段A先由前端简单除法展示）

卡片采用渐变底色区分类别：盈利类（蓝绿）、资产类（橙红）、现金流类（灰紫）。

**第二层：趋势图表区（4个图表，上下排列）**
每个图表占一行，高度 320px：

1. **盈利能力趋势** — 双 Y 轴混合图：柱状图展示营业总收入 + 归母净利润 + **营业成本**（左轴，金额），折线图展示毛利率 + 净利率（右轴，%）
2. **成本费用结构趋势** — 堆叠柱状图：销售费用 + 管理费用 + 研发费用 + 财务费用；叠加折线展示期间费用率
3. **资产负债结构趋势** — 堆叠柱状图：总资产 = 流动资产 + 非流动资产；叠加折线展示资产负债率
4. **现金流健康度趋势** — 分组柱状图：经营/投资/筹资现金流净额并列展示；叠加折线展示期末现金余额

**第三层：关键数据表格（可选，折叠态默认收起）**
简要展示近5年年报的核心数据一览表（8列：报告期、营收、净利润、毛利率、ROE、总资产、负债率、经营现金流）。

### 3.2 数据源
直接调用现有 `/finance/{stockCode}/indicators` 接口，请求参数：
- `metrics`：阶段A涉及的约20个字段（含成本和费用指标）
- `reportType=年报`
- `startDate` 自动计算为5年前

### 3.3 空态与加载
- 无数据时显示"暂无年报数据，请检查采集任务是否已完成"
- 加载状态使用 `ElSkeleton` 卡片占位 + 图表区域 spin

---

## 四、独立投研分析页面（阶段A完整版）

### 4.1 页面定位
独立页面 `/research/fundamental` 是投研分析的**主战场**，不依赖从公司列表进入，支持全局搜索和筛选任意公司。与详情页精简版的区别：展示维度更丰富、支持更长历史周期、预留多公司对比和行业排名的扩展接口。

### 4.2 布局：左右分栏

**左侧边栏（固定宽 320px）**
- **搜索区**：股票代码/名称模糊搜索框，支持下拉联想
- **筛选区**：行业下拉（复用 `industry_category` 数据）、市场板块（SH/SZ/BJ）多选
- **公司列表**：搜索结果以卡片列表呈现（代码、名称、行业、最新营收），点击后在右侧加载
- **对比池（阶段B启用，阶段A预留 UI 占位）**：用户可点击"加入对比"将公司暂存，最多5家

**右侧主区域：单公司深度看板**
- **公司信息栏**：公司名称、股票代码、所属行业、市场、上市日期，带快捷链接跳转至公司详情页
- **核心指标仪表盘（8张卡片，2行4列）**：营业总收入、归母净利润、毛利率、净利率、ROE、总资产、资产负债率、经营现金流/净利润比。卡片带颜色阈值提示（如资产负债率 > 70% 标红，阶段B配置规则）
- **图表区（5个图表，每个高度 360px）**：
  1. **盈利能力全景**：柱状图（营收、净利润）+ 折线图（毛利率、净利率、ROE），双 Y 轴
  2. **成本费用结构**：堆叠柱状图展示营业成本 + 销售费用 + 管理费用 + 研发费用 + 财务费用；叠加折线展示期间费用率（=(销售+管理+研发+财务费用)/营业收入 × 100%）
  3. **资产负债结构**：堆叠柱状图（流动资产、非流动资产、流动负债、非流动负债）+ 折线（资产负债率）
  4. **现金流全景**：分组柱状图（经营/投资/筹资净额）+ 折线（期末现金余额、经营现金流/净利润比）
  5. **杜邦分析拆解（阶段B启用，阶段A显示"即将上线"占位）**：树形图展示 ROE = 净利率 × 资产周转率 × 权益乘数
- **同行业公司速览（底部折叠区，默认收起）**：表格展示同行业其他公司的核心指标（代码、名称、营收、净利润、ROE、资产负债率），点击行可切换当前分析对象

### 4.3 默认行为
页面首次加载右侧显示引导态："请输入股票代码或选择左侧筛选条件开始分析"。选择公司后自动加载近5年年报数据，图表支持鼠标悬停查看具体数值。

---

## 五、后端数据层扩展

### 5.1 阶段A的数据策略：零新表
阶段A仅做数据展示与组装，所有指标均可从现有 `financial_report` 表直接查询计算，**不新增业务数据表**。

### 5.2 核心领域模型
```java
// FundamentalMetrics.java — 单公司基本面指标集合
public class FundamentalMetrics {
    private String stockCode;
    private String stockName;
    private String industry;
    private List<AnnualMetric> annualMetrics;  // 近5年年报指标列表
}

// AnnualMetric.java — 单一年度的指标快照
public class AnnualMetric {
    private LocalDate reportDate;
    private Integer reportYear;
    // 盈利能力
    private BigDecimal totalRevenue;
    private BigDecimal parentNetProfit;
    private BigDecimal grossMargin;
    private BigDecimal netMargin;
    private BigDecimal roe;
    // 资产负债
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal debtRatio;
    // 现金流
    private BigDecimal operatingCashFlow;
    private BigDecimal investingCashFlow;
    private BigDecimal financingCashFlow;
    // 成本费用（阶段A新增）
    private BigDecimal operateCost;
    private BigDecimal saleExpense;
    private BigDecimal manageExpense;
    private BigDecimal researchExpense;
    private BigDecimal financeExpense;
}
```

### 5.3 新增 API 接口

#### 5.3.1 基本面概览
```
GET /api/research/fundamental/overview/{stockCode}
```
**响应**：`FundamentalOverviewResponse`
- 公司基本信息（名称、行业、市场）
- `metrics`：近5年年报指标数组（按报告期升序）

#### 5.3.2 股票筛选
```
GET /api/research/fundamental/screen?industry=...&market=...&keyword=...&page=...&size=...
```
**响应**：`FundamentalScreenResponse`
- 分页公司列表（代码、名称、行业、最新营收、最新净利润）

#### 5.3.3 同行业对比
```
GET /api/research/fundamental/industry-peers/{stockCode}
```
**响应**：`IndustryPeersResponse`
- 目标公司所属行业的其他公司列表
- 每家公司展示最新年报核心指标（营收、净利润、ROE、资产负债率）

### 5.4 Repository 查询策略
`FundamentalMetricsRepositoryImpl` 基于 `NamedParameterJdbcTemplate` 编写联合查询：
```sql
SELECT fr.*, cs.stock_name, c.industry
FROM financial_report fr
JOIN company_security cs ON fr.stock_code = cs.stock_code
JOIN company c ON cs.company_id = c.id
WHERE fr.stock_code = :stockCode
  AND fr.report_type = '年报'
  AND fr.report_date >= :startDate
  AND fr.is_deleted = FALSE
ORDER BY fr.report_date ASC
```

### 5.5 阶段B预留：衍生指标物化表
阶段B引入同比增长率、ROE、期间费用率等需要跨期/跨表计算的指标时，再新增 `stock_fundamental_metrics` 表：

```sql
CREATE TABLE IF NOT EXISTS stock_fundamental_metrics (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    report_year INTEGER NOT NULL,
    -- 同比增长率
    revenue_yoy DECIMAL(10,4),
    profit_yoy DECIMAL(10,4),
    asset_growth_rate DECIMAL(10,4),
    -- 效率指标
    roe DECIMAL(10,4),
    roa DECIMAL(10,4),
    asset_turnover DECIMAL(10,4),
    equity_multiplier DECIMAL(10,4),
    -- 偿债指标
    current_ratio DECIMAL(10,4),
    quick_ratio DECIMAL(10,4),
    -- 盈利质量
    cashflow_profit_ratio DECIMAL(10,4),
    period_expense_rate DECIMAL(10,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_fundamental_metrics UNIQUE (stock_code, report_year)
);
```

预计算由新增采集任务 `FundamentalMetricsTask` 触发，在 `FinanceTask` 完成财报采集后自动执行。

---

## 六、数据流与交互流程

### 6.1 公司详情页数据流
```
用户点击"基本面分析" Tab
  → 前端调用 GET /finance/{stockCode}/indicators
  → 后端查询 financial_report（年报，近5年）
  → 返回指标数组
  → 前端渲染卡片（简单除法计算毛利率/净利率/ROE）
  → 前端渲染4个 ECharts 图表
```

### 6.2 独立投研页面数据流
```
用户进入 /research/fundamental
  → 右侧显示引导态
用户搜索/筛选公司
  → 前端调用 GET /research/fundamental/screen
  → 左侧展示公司列表
用户点击某公司
  → 前端调用 GET /research/fundamental/overview/{stockCode}
  → 右侧渲染深度看板
用户展开"同行业速览"
  → 前端调用 GET /research/fundamental/industry-peers/{stockCode}
  → 底部展示同行对比表
```

---

## 七、错误处理与边界情况

| 场景 | 处理策略 |
|------|----------|
| 公司无年报数据 | 显示空态引导"暂无年报数据，请检查采集任务是否已完成" |
| 某年度部分指标缺失 | 图表中该数据点不绘制，Tooltip 显示"数据缺失" |
| 除数为零（如 ROE 计算时分母为零） | 返回 `null`，前端显示"-" |
| 筛选结果为空 | 左侧列表显示"未找到匹配公司" |
| 同行业公司只有自己一家 | 底部折叠区显示"暂无同行业对比数据" |
| 后端接口超时 | 前端 `ElMessage.error("数据加载失败，请稍后重试")`，支持手动刷新 |

---

## 八、测试策略

### 8.1 后端测试
- **Controller 层单元测试**：使用 `@WebMvcTest` 验证三个 API 接口的响应结构和状态码
- **Repository 层集成测试**：继承 `RepositoryTestBase`，基于 Testcontainers + PostgreSQL 验证联合查询 SQL 的正确性
- **Service 层单元测试**：Mock Repository，验证空数据兜底、单位转换、指标计算逻辑

### 8.2 前端测试
- 当前项目尚未配置前端测试框架，阶段A以手工验证为主
- 建议后续引入 Vitest + Vue Test Utils，对 `MetricDashboard` 和图表组件编写单元测试

### 8.3 端到端验证
- 使用项目内置 `security-analyze-tester` skill 验证：
  - 公司详情页"基本面分析" Tab 正常加载且图表渲染
  - 独立投研页面搜索/筛选/图表交互完整可用

---

## 九、阶段B与阶段C的演进路径

### 9.1 阶段B：衍生指标计算与横向对比
**核心变化：从"展示原始数据"升级为"计算洞察指标"**

**数据层**：
- 新增 `stock_fundamental_metrics` 表（详见 5.5 节）
- 新增采集任务 `FundamentalMetricsTask`，在 `FinanceTask` 完成后自动触发预计算

**前端增强**：
- 指标卡片增加**同比变动箭头**（红涨绿跌或按业务语义）
- 图表支持**多公司对比模式**（对比池最多5家公司，同图表不同颜色线）
- 新增**行业排名表格**：按 ROE、毛利率、净利润增速等指标对同行业公司排序
- 杜邦分析拆解图从占位变为可用：树形图展示 ROE = 净利率 × 资产周转率 × 权益乘数

### 9.2 阶段C：综合估值分析工具
**核心变化：从"历史财务分析"升级为"未来估值判断"**

- 接入日行情数据（复用 `quote_task` 已采集的收盘价），计算 PE/PB/PS 及历史分位数
- 新增简易 DCF 估值输入面板：用户可调整增长率、折现率假设，实时生成估值区间
- 输出**综合评分卡片**：财务健康分（阶段B指标加权）+ 估值吸引力分（阶段C分位数评分）
- 增加**估值预警**：当 PE 处于历史90%分位时标红提示高估

---

## 十、阶段A实施计划

### 任务拆分（预计 3-4 天）

**后端（1.5 天）**
1. **P0** — 新建 `research/fundamental` package，定义 Domain 值对象与 Repository 接口（0.5 天）
2. **P0** — 实现 `FundamentalMetricsRepositoryImpl`，编写三表联合查询 SQL（0.5 天）
3. **P0** — 实现 `FundamentalAnalysisService` 与 `FundamentalAnalysisController`，暴露 `/overview`、`/screen`、`/industry-peers` 三个接口（0.5 天）

**前端（2 天）**
4. **P0** — 新增 `src/views/research/` 目录，创建 `FundamentalAnalysisView.vue` 框架与左侧边栏组件（0.5 天）
5. **P0** — 实现公司详情页"基本面分析" Tab（复用并扩展 `ReportSummaryCards` + `IndicatorChart`，新增成本费用趋势图）（0.5 天）
6. **P0** — 实现独立页面右侧深度看板（5个图表区域 + 公司信息栏 + 同行业速览）（0.5 天）
7. **P1** — 顶部导航新增"投研分析"菜单，注册 `/research/fundamental` 路由（0.25 天）
8. **P1** — 空态、加载态、错误处理与响应式适配（0.25 天）

### 前后端并行策略
后端任务1-3与前端任务4可并行启动；前端任务5依赖后端接口 mock，任务6依赖真实接口。建议后端先提供 `/overview` 接口，前端以此为核心串联其余视图。

---

## 十一、相关文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| 财务模块设计 | `docs/wiki/finance-module-design.md` | 财务三表数据模型、现有 API 契约 |
| 模块设计 | `docs/wiki/module-design.md` | 7大功能模块的职责边界与分层归属 |
| API 契约（公司） | `docs/wiki/api-company.md` | 公司列表、公司详情接口字段定义 |
| 数据库基线 | `backend/src/main/resources/db/release/v1.0.0__full_schema.sql` | 完整表结构快照 |
