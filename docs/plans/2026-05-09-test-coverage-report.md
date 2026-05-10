# 公司信息与投研分析模块 — 测试覆盖率报告

> 日期：2026-05-09  
> 范围：公司信息模块（company）+ 投研分析模块（research/fundamental）  
> 后端工具：JaCoCo + JUnit 5 + Testcontainers + Mockito  
> 前端工具：Vitest + @vitest/coverage-v8 + @vue/test-utils

---

## 一、测试用例清单

### 1.1 后端 — 公司信息模块（company）

#### Controller 层（`CompanyControllerTest`）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `shouldListCompanies` | 集成测试 | 验证公司列表接口返回结构与字段 |
| 2 | `shouldGetCompanyDetail` | 集成测试 | 验证公司详情接口返回公司基本信息与证券列表 |
| 3 | `shouldReturn404WhenCompanyNotFound` | 集成测试 | 验证不存在的 stockCode 返回 404 |
| 4 | `shouldNormalizePaginationParams` | 集成测试 | 验证负数页码/零 size 被规范化 |
| 5 | `shouldBatchQueryCompanies` | 集成测试 | 验证 `/companies/batch` 批量查询接口 |

#### Service 层（`CompanyServiceTest`）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `shouldListCompanies` | 单元测试 | 分页查询 + 批量关联公司信息避免 N+1 |
| 2 | `shouldListCompaniesWithEmptyResult` | 单元测试 | 空关键词返回空列表 |
| 3 | `shouldGetCompanyDetail` | 单元测试 | 正常详情查询，验证 securities 列表组装 |
| 4 | `shouldReturnEmptyWhenStockCodeNotFound` | 单元测试 | 证券不存在返回 Optional.empty() |
| 5 | `shouldReturnEmptyWhenCompanyNotFound` | 单元测试 | 公司不存在返回 Optional.empty() |
| 6 | `shouldBatchQueryCompanies` | 单元测试 | 批量查询多个 stockCode，正常组装 |
| 7 | `shouldReturnEmptyForBatchQueryWithInvalidCodes` | 单元测试 | 无效 stockCode 返回空列表 |
| 8 | `shouldLimitBatchQueryTo50` | 单元测试 | 超过 50 个代码时只返回前 50 条 |

#### Repository 层（`CompanyRepositoryImplTest`）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `shouldFindById` | 集成测试 | 按 ID 查询公司 |
| 2 | `shouldReturnEmptyWhenIdNotFound` | 集成测试 | 不存在的 ID 返回空 |
| 3 | `shouldFindAllById` | 集成测试 | 批量按 ID 查询 |
| 4 | `shouldReturnEmptyListWhenIdsEmpty` | 集成测试 | 空 ID 列表返回空 |
| 5 | `shouldFindByKeyword` | 集成测试 | 按公司名称关键词模糊匹配 |
| 6 | `shouldFindByKeywordMatchingShortName` | 集成测试 | 按公司简称匹配 |
| 7 | `shouldReturnAllWhenKeywordNullOrBlank` | 集成测试 | 无关键词返回全部 |
| 8 | `shouldCountByKeyword` | 集成测试 | 关键词计数 |
| 9 | `shouldFindByStockCode` | 集成测试 | 通过 stockCode 关联查询公司 |
| 10 | `shouldReturnEmptyWhenStockCodeNotFound` | 集成测试 | 不存在的 stockCode |
| 11 | `shouldReturnEmptyWhenOffsetExceedsTotal` | 集成测试 | 偏移量超过总数 |
| 12 | `shouldHandleKeywordCaseInsensitive` | 集成测试 | ILIKE 大小写不敏感 |
| 13 | `shouldTrimKeyword` | 集成测试 | 关键字前后空格 trim |

> 注：`CompanySecurityRepositoryImpl` 暂无独立测试类，其逻辑通过 `CompanyRepositoryImplTest` 和 `CompanyServiceTest` 间接覆盖。

---

### 1.2 后端 — 投研分析模块（research/fundamental）

#### Controller 层（`ResearchControllerTest`）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `shouldReturn200WhenOverviewExists` | 集成测试 | `/overview/{stockCode}` 正常返回 |
| 2 | `shouldReturn404WhenOverviewNotFound` | 集成测试 | 无数据返回 404 |
| 3 | `shouldScreenCompanies` | 集成测试 | `/screen` 筛选返回分页结果 |
| 4 | `shouldGetIndustryPeers` | 集成测试 | `/industry-peers/{stockCode}` 返回同行列表 |
| 5 | `shouldNormalizePaginationParams` | 集成测试 | 分页参数规范化 |

#### Service 层（`FundamentalAnalysisServiceTest`）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `shouldGetOverview` | 单元测试 | 正常获取基本面概览 |
| 2 | `shouldReturnEmptyWhenOverviewNotFound` | 单元测试 | 无数据返回 Optional.empty() |
| 3 | `shouldCalculateGrossMarginCorrectly` | 单元测试 | 毛利率 = (收入 - 成本) / 收入 × 100 |
| 4 | `shouldCalculateNetMarginCorrectly` | 单元测试 | 净利率 = 净利润 / 收入 × 100 |
| 5 | `shouldCalculateRoeCorrectly` | 单元测试 | ROE = 净利润 / 净资产 × 100 |
| 6 | `shouldHandleNullFieldsGracefully` | 单元测试 | 空值字段不抛异常，返回 null |
| 7 | `shouldHandleDivisionByZero` | 单元测试 | 除数为零返回 null |
| 8 | `shouldScreenCompanies` | 单元测试 | 筛选公司列表并分页 |
| 9 | `shouldNormalizePagination` | 单元测试 | 负数页码/零 size 被规范化 |
| 10 | `shouldGetIndustryPeers` | 单元测试 | 获取同行业对比数据 |

#### Repository 层（`FundamentalMetricsRepositoryImplTest`）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `shouldFindByStockCode` | 集成测试 | 正常查询基本面指标 |
| 2 | `shouldReturnEmptyWhenStockCodeNotFound` | 集成测试 | 不存在的 stockCode |
| 3 | `shouldReturnEmptyWhenNoAnnualReports` | 集成测试 | 公司存在但无年报数据 |
| 4 | `shouldFilterByReportYearRange` | 集成测试 | 只返回近 N 年的年报 |
| 5 | `shouldScreenCompanies` | 集成测试 | 无条件筛选返回公司列表 |
| 6 | `shouldScreenCompaniesByKeyword` | 集成测试 | 按代码精确/名称前缀筛选 |
| 7 | `shouldScreenCompaniesByMarket` | 集成测试 | 按市场板块筛选 |
| 8 | `shouldCountScreenCompanies` | 集成测试 | 筛选结果计数 |
| 9 | `shouldFindIndustryPeers` | 集成测试 | 同行业公司对比（排除自身） |
| 10 | `shouldReturnEmptyPeersWhenNoSameIndustry` | 集成测试 | 无同行业公司时返回空 |
| 11 | `shouldCalculatePeerMetrics` | 集成测试 | 验证同行 ROE/负债率计算正确 |

---

### 1.3 前端 — 投研分析 API（research.spec.ts）

| # | 用例名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `should fetch overview by stock code` | 单元测试 | `getFundamentalOverview` 调用正确 URL |
| 2 | `should screen companies with keyword` | 单元测试 | `screenCompanies` 带关键词参数 |
| 3 | `should screen companies with industry and market filters` | 单元测试 | `screenCompanies` 带筛选条件 |
| 4 | `should fetch industry peers by stock code` | 单元测试 | `getIndustryPeers` 调用正确 URL |

> 注：公司信息前端（`CompanyListView.vue`、`CompanyDetailView.vue`）暂无单元测试；投研分析前端组件（`FundamentalAnalysisView.vue`、`FundamentalAnalysisTab.vue`、`FundamentalScreener.vue`、4 个图表组件）暂无单元测试。

---

## 二、覆盖率汇总

### 2.1 后端覆盖率（JaCoCo）

| Package / 类 | 指令覆盖率 | 分支覆盖率 | 行覆盖率 | 复杂度覆盖率 | 状态 |
|-------------|----------|----------|---------|------------|------|
| **company/api** | **100.0%** | N/A | **100.0%** | **100.0%** | ✅ |
| `CompanyController` | 100.0% | N/A | 100.0% | 100.0% | ✅ |
| **company/application** | **80.2%** | **43.5%** | **80.8%** | **60.9%** | ⚠️ |
| `CompanyService` | 80.2% | 43.5% | 80.8% | 60.9% | ⚠️ |
| **company/infrastructure** | **94.5%** | **63.0%** | **94.0%** | **59.0%** | ✅ |
| `CompanyRepositoryImpl` | 99.5% | 77.8% | 100.0% | 73.3% | ✅ |
| `CompanySecurityRepositoryImpl` | 85.4% | 56.2% | 82.5% | 57.1% | ⚠️ |
| **company/domain** | N/A | N/A | N/A | N/A | ⚪ |
| `Company`, `CompanySecurity`, `CompanyRepository`, `CompanySecurityRepository` | — | — | — | — | ⚪（Lombok 实体/接口） |
| **research/api** | **100.0%** | N/A | **100.0%** | **100.0%** | ✅ |
| `ResearchController` | 100.0% | N/A | 100.0% | 100.0% | ✅ |
| **research/application** | **99.8%** | **80.0%** | **100.0%** | **81.8%** | ✅ |
| `FundamentalAnalysisService` | 99.8% | 80.0% | 100.0% | 81.8% | ✅ |
| **research/infrastructure** | **89.3%** | **46.7%** | **91.1%** | **50.0%** | ⚠️ |
| `FundamentalMetricsRepositoryImpl` | 86.5% | 46.4% | 88.5% | 47.6% | ⚠️ |
| `FundamentalMetricsRepositoryImpl$1` | 100.0% | 50.0% | 100.0% | 66.7% | ✅ |
| **research/domain** | N/A | N/A | N/A | N/A | ⚪ |
| `AnnualMetric`, `FundamentalMetrics`, `PeerMetric`, `ScreenCompanyItem`, `FundamentalMetricsRepository` | — | — | — | — | ⚪（Lombok 实体/接口） |

**图例**：
- ✅ 覆盖率达标（行 ≥ 80%，分支 ≥ 60%）
- ⚠️ 需提升（行或分支未达标）
- ⚪ 不计入（Lombok 生成类 / 纯接口，Jacoco 源码级统计为 N/A）

---

### 2.2 前端覆盖率（Vitest Coverage v8）

| 文件/目录 | 语句覆盖率 | 分支覆盖率 | 函数覆盖率 | 行覆盖率 | 状态 |
|----------|----------|----------|----------|---------|------|
| **api/research.ts** | **100%** | **100%** | **100%** | **100%** | ✅ |
| **views/company/fundamental/** | **0%** | **0%** | **0%** | **0%** | ❌ |
| `FundamentalAnalysisTab.vue` | 0% | 0% | 0% | 0% | ❌ |
| **views/research/** | **0%** | **0%** | **0%** | **0%** | ❌ |
| `FundamentalAnalysisView.vue` | 0% | 0% | 0% | 0% | ❌ |
| `FundamentalScreener.vue` | 0% | 0% | 0% | 0% | ❌ |
| `ProfitabilityChart.vue` | 0% | 0% | 0% | 0% | ❌ |
| `CostExpenseChart.vue` | 0% | 0% | 0% | 0% | ❌ |
| `BalanceSheetChart.vue` | 0% | 0% | 0% | 0% | ❌ |
| `CashFlowChart.vue` | 0% | 0% | 0% | 0% | ❌ |

> 注：前端 Vitest 配置 `include: ['src/**/*.spec.ts']`，新创建的 `.vue` 组件未编写 `.spec.ts` 测试文件，因此覆盖率为 0%。

---

## 三、覆盖率缺口分析

### 3.1 后端缺口

#### CompanyService（行 80.8%，分支 43.5%）

**未覆盖分支**：
- `listCompanies` 中 `company == null` 的分支（当 `companyMap.get(sec.getCompanyId())` 返回 null 时）
- `toDetailResponse` 中行业映射循环的多个分支（`level2Code != null`、`primary` 判断等）
- `loadIndustries` 中 `default -> mapping.getStandardCode()` 分支

**未覆盖行**：
- `getCompanyDetail` 中 `companyOpt.isEmpty()` 后的日志和返回
- `toListItem` 中 `company == null` 时的默认值处理

#### CompanySecurityRepositoryImpl（行 82.5%，分支 56.2%）

**未覆盖分支**：
- `findByCompanyIds` 中 `companyIds == null || companyIds.isEmpty()` 的提前返回分支
- `findByKeyword` 中 `keyword == null || keyword.isBlank()` 的分支（目前测试通过 Service 间接覆盖，但缺少直接测试）
- `countByKeyword` 同上

#### FundamentalMetricsRepositoryImpl（行 88.5%，分支 46.4%）

**未覆盖分支**：
- `findByStockCode` 中 `metrics.isEmpty()` 后的 `Optional.empty()` 分支（已覆盖）
- `screenCompanies` 中 `keyword`、`industry`、`market` 三个条件均为 null 的分支（已覆盖）
- `screenCompanies` 中 `industry != null` 和 `market != null` 的分支（部分覆盖）
- `countScreenCompanies` 中条件组合分支（部分覆盖）
- `findIndustryPeers` 中 `peers.isEmpty()` 场景（部分覆盖）

### 3.2 前端缺口

前端新创建的 8 个文件（1 个 API + 7 个 Vue 组件）中，仅 API 层有测试覆盖，**全部 Vue 组件覆盖率为 0%**。

主要原因：
1. 未编写 `.spec.ts` 测试文件
2. 图表组件依赖 ECharts，在 jsdom 环境下渲染需要额外 mock
3. 组件涉及异步数据获取（`onMounted` + `watch`），需要掌握 `flushPromises` 等测试技巧

---

## 四、提升测试覆盖率的建议

### 4.1 后端（优先级：高）

#### 目标1：CompanyService 分支覆盖率 → 70%+

**新增测试用例**：

```java
// CompanyServiceTest 中补充
@Test
void shouldListCompaniesWhenCompanyInfoMissing() {
    // 某条 securities 的 company_id 在 companyMap 中找不到
    CompanySecurity sec = createSecurity(999L, "600999", "孤儿证券", "SH");
    when(companySecurityRepository.findByKeyword(null, 0, 20)).thenReturn(List.of(sec));
    when(companySecurityRepository.countByKeyword(null)).thenReturn(1L);
    when(companyRepository.findAllById(List.of(999L))).thenReturn(List.of());

    CompanyListResponse response = companyService.listCompanies(null, 0, 20);

    assertEquals(1, response.getItems().size());
    assertNull(response.getItems().get(0).getIndustry()); // company 为 null 时 industry 为 null
}

@Test
void shouldLoadIndustriesWithMultipleStandards() {
    // 验证 level2Code 为 null 时的分支
    // 验证 primary = false 时的分支
}
```

#### 目标2：CompanySecurityRepositoryImpl 独立测试类

新建 `CompanySecurityRepositoryImplTest`，继承 `RepositoryTestBase`，覆盖：
- `findByCompanyId` — 正常查询、空 companyId
- `findByCompanyIds` — 正常查询、null/empty 列表
- `findByStockCode` — 精确匹配、不存在
- `findByKeyword` — 代码精确匹配、名称前缀匹配、keyword 为 null
- `countByKeyword` — 同上

#### 目标3：FundamentalMetricsRepositoryImpl 分支覆盖率 → 70%+

**新增测试用例**：

```java
@Test
void shouldScreenCompaniesByIndustry() {
    // 按行业筛选，验证 industry 条件拼接正确
}

@Test
void shouldScreenCompaniesWithAllFilters() {
    // keyword + industry + market 同时存在
}

@Test
void shouldCountScreenCompaniesWithFilters() {
    // count 时同时带 keyword + industry + market
}

@Test
void shouldReturnEmptyPeersWhenTargetCompanyHasNoIndustry() {
    // target_company CTE 返回 industry = null
}
```

#### 目标4：Jacoco 排除 Lombok 生成代码（可选）

在 `build.gradle` 中配置 Jacoco 排除规则，避免 `*Dto`、`*Response`、Domain 实体等 Lombok 类拉低整体覆盖率印象分：

```groovy
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/api/**Response*',
                '**/api/**Item*',
                '**/domain/**',
            ])
        }))
    }
}
```

> **注意**：排除配置仅用于报告展示，不应作为降低测试质量要求的借口。核心逻辑类仍需保持高覆盖率。

---

### 4.2 前端（优先级：高）

#### 目标1：FundamentalAnalysisTab.vue 组件测试

新建 `src/views/company/fundamental/FundamentalAnalysisTab.spec.ts`：

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import FundamentalAnalysisTab from './FundamentalAnalysisTab.vue'

vi.mock('@/api/research', () => ({
  getFundamentalOverview: vi.fn(),
}))

import { getFundamentalOverview } from '@/api/research'

describe('FundamentalAnalysisTab', () => {
  it('should render metric cards after loading', async () => {
    vi.mocked(getFundamentalOverview).mockResolvedValue({
      stockCode: '600519',
      stockName: '贵州茅台',
      metrics: [{
        reportDate: '2023-12-31',
        totalRevenue: 150545774400,
        parentNetProfit: 74734034300,
        grossMargin: 87.92,
        netMargin: 52.49,
        roe: 28.64,
        totalAssets: 272699712000,
        totalEquity: 223601626300,
        operatingCashFlow: 66593123800,
        operateCost: 18182166700,
        saleExpense: 4646490800,
        manageExpense: 11936681200,
        researchExpense: 157208100,
        financeExpense: -1791682400,
        periodExpenseRate: 10.04,
        debtRatio: 18.00,
        endCce: 69070051800,
        cashflowProfitRatio: 89.11,
        operatingCashFlow: 66593123800,
        investingCashFlow: -2871441700,
        financingCashFlow: -38863260000,
      }],
    })

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('营业总收入')
    expect(wrapper.text()).toContain('毛利率')
    expect(wrapper.text()).toContain('ROE')
  })

  it('should show empty state when no data', async () => {
    vi.mocked(getFundamentalOverview).mockResolvedValue({
      stockCode: '600519',
      stockName: '测试',
      metrics: [],
    })

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无年报数据')
  })
})
```

#### 目标2：图表组件测试策略

ECharts 在 jsdom 环境下无法正常渲染 Canvas。推荐策略：

1. **浅渲染（shallow mount）**：只验证组件接收 props 并传递给 `VChart`
2. **Mock `vue-echarts`**：在测试配置中全局 mock

```typescript
// vitest.config.ts 或 setupFiles
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    props: ['option'],
    template: '<div class="mock-chart" />',
  },
}))
```

然后测试验证：

```typescript
it('should pass metrics to chart option', async () => {
  const wrapper = mount(ProfitabilityChart, {
    props: { metrics: mockMetrics },
  })
  // 验证 option 计算属性中包含了正确的 series 数据
})
```

#### 目标3：FundamentalScreener.vue 测试

- 验证搜索输入框变化后触发 `screenCompanies`
- 验证点击公司项后触发 `select` 事件并高亮
- 验证空结果展示

#### 目标4：FundamentalAnalysisView.vue 测试

- 验证未选择公司时展示引导态
- 验证选择公司后加载数据和图表
- 验证同行业折叠面板展开/收起

---

### 4.3 端到端测试补充（优先级：中）

使用项目内置 `security-analyze-tester` skill 补充以下场景：

| # | 场景 | 验证点 |
|---|------|--------|
| 1 | 公司详情页 Tab 切换 | 基本信息 → 关联证券 → 财务报告 → **基本面分析** → 历史变更，各 Tab 正常加载 |
| 2 | 基本面分析简版 | 8张卡片数值正确、4个图表渲染、折叠表格数据完整 |
| 3 | 投研分析导航入口 | 顶部导航"投研分析"可点击进入独立页面 |
| 4 | 独立页面完整版 | 左侧筛选 → 选择公司 → 右侧看板加载 → 展开同行业对比 |
| 5 | 空态场景 | 输入不存在股票代码 / 无年报数据的公司，空态提示正确 |
| 6 | 公司批量查询 | 登录后"重点关注"功能从 localStorage 切换到后端批量接口 |

---

## 五、总结

| 维度 | 当前状态 | 目标 |
|------|---------|------|
| 后端 Controller | 100% / N/A | 保持 ✅ |
| 后端 Service | 80.8% / 43.5% | 行 85%+ / 分支 70%+ |
| 后端 Repository | 82.5%–100% / 46.4%–77.8% | 行 90%+ / 分支 70%+ |
| 前端 API | 100% / 100% | 保持 ✅ |
| 前端组件 | 0% / 0% | 行 60%+ / 分支 50%+ |

**下一步行动**：
1. 本周内完成后端 Service/Repository 的补充测试（预计 0.5 天）
2. 下周开始前端组件测试，优先 `FundamentalAnalysisTab.vue` 和 `FundamentalScreener.vue`（预计 1 天）
3. 配置 ECharts mock 后补充图表组件测试（预计 0.5 天）
