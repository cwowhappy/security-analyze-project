# 阶段B代码审查报告

> 审查范围：阶段B新增/修改的全部代码（后端Java、前端Vue/TS、采集Python、数据库SQL）
> 审查日期：2026-05-09
> 审查人：Kimi Code CLI

---

## 一、严重问题（Critical）

### C1. 杜邦分析组件存在除零崩溃风险

| 维度 | 详情 |
|------|------|
| **位置** | `frontend/src/views/research/components/DupontAnalysisChart.vue` |
| **问题** | `effectiveAssetTurnover` 和 `effectiveEquityMultiplier` 的计算未校验分母是否为0。当 `totalAssets === 0` 或 `totalEquity === 0` 时，产生 `Infinity` 或 `NaN`，`.toFixed(2)` 会输出 `"Infinity"` 或 `"NaN"` 到树图节点上。 |
| **影响** | 页面展示异常字符串；极端情况下 ECharts 渲染失败。 |
| **修复建议** | 引入安全除法工具函数：```ts function safeDiv(a?: number, b?: number): number | undefined { if (a == null || b == null || b === 0) return undefined; return a / b; }```，并对所有展示值做 `undefined` 兜底显示 `"-"`。 |

### C2. FinanceTask 重复触发衍生指标计算

| 维度 | 详情 |
|------|------|
| **位置** | `collector/collector/tasks/finance_task.py` + `collector/collector/cli.py` |
| **问题** | `run_incremental()` 先调用 `run_full(incremental=True)`（内部已触发一次 FM），返回后又显式调用 `_trigger_fundamental_metrics("incremental")`，导致**同一批最新年度指标被计算两次**。调度器里又显式再跑一次，形成**三重触发**。 |
| **影响** | 浪费大量 CPU 和数据库 I/O；对全量采集场景尤其严重。 |
| **修复建议** | 两个方案二选一：<br>① **推荐**：移除 `run_incremental` 末尾的显式触发，仅依赖 `run_full` 内部的自动触发；调度器也去掉显式 FM 调用。<br>② 在 `FinanceTask` 中关闭自动触发（`auto_trigger_fundamental_metrics=false`），由调用方（CLI/调度器）全权控制 FM 执行时机。 |

---

## 二、高优先级问题（High）

### H1. 行业排名接口对不存在的股票码返回500

| 维度 | 详情 |
|------|------|
| **位置** | `backend/src/main/java/.../research/application/FundamentalAnalysisService.java#getIndustryRank` |
| **问题** | 首行使用 `jdbcTemplate.queryForObject` 查询行业，若股票代码不存在，抛出 `EmptyResultDataAccessException`，直接返回 HTTP 500。 |
| **影响** | 用户体验差；错误日志噪音。 |
| **修复建议** | 捕获异常或改用 `query` + `stream.findFirst()`，不存在时返回空的 `IndustryRankResponse`（Controller 层可映射为 404）。 |

### H2. Service 层直接使用 JdbcTemplate，违反 DDD 分层

| 维度 | 详情 |
|------|------|
| **位置** | `FundamentalAnalysisService.java` |
| **问题** | `getIndustryRank` 方法内直接编写 SQL 并通过注入的 `NamedParameterJdbcTemplate` 执行，同时内联定义 `RowMapper` 构造 `IndustryRankItemDto`。这打破了 `application → domain ← infrastructure` 的依赖方向。 |
| **影响** | 应用层与基础设施层耦合；SQL 变更需修改 Service；难以单元测试（必须连数据库）。 |
| **修复建议** | 将行业排名查询封装到 `FundamentalMetricsRepository`（或新建 `IndustryRankRepository`）中，Service 仅调用 `repository.findIndustryRankItems(industry)` 并做排序/组装。 |

### H3. 行业排名表格排序事件未绑定

| 维度 | 详情 |
|------|------|
| **位置** | `frontend/src/views/research/FundamentalAnalysisView.vue` + `IndustryRankTable.vue` |
| **问题** | `IndustryRankTable` 定义了 `@sort` 事件并在表头渲染排序箭头，但父组件 `FundamentalAnalysisView` 未监听 `@sort`，导致用户点击表头后 UI 箭头变化但数据从不重新加载或排序。 |
| **影响** | 排序功能不可用，是功能缺陷。 |
| **修复建议** | 父组件绑定 `@sort="onRankSort"`，实现：```ts async function onRankSort(field: string) { const res = await getIndustryRank(selectedStock.value, field, 'desc'); rankData.value = res.items; }``` |

### H4. 筛选器行业下拉菜单从未填充数据

| 维度 | 详情 |
|------|------|
| **位置** | `frontend/src/views/research/components/FundamentalScreener.vue` |
| **问题** | `industryOptions` 初始化后仅有 `"全部行业"`，没有任何代码从 API 加载行业列表。用户无法使用行业筛选。 |
| **影响** | 行业筛选功能不可用。 |
| **修复建议** | 组件挂载时调用接口（如 `/api/company/industries` 或从 `screenCompanies` 结果中聚合唯一行业）填充下拉选项。 |

### H5. 杜邦分析缺少空值保护，显示 NaN

| 维度 | 详情 |
|------|------|
| **位置** | `DupontAnalysisChart.vue` |
| **问题** | `m.parentNetProfit / 1e8` 等计算未做空值检查。若 API 返回字段缺失，模板直接渲染 `"NaN亿"`。 |
| **影响** | 数据缺失时展示不友好的 `"NaN"` 字符串。 |
| **修复建议** | 所有金额展示统一使用安全格式化：```ts const profitText = m.parentNetProfit != null ? (m.parentNetProfit / 1e8).toFixed(2) + '亿' : '-'; ``` |

---

## 三、中优先级问题（Medium）

### M1. 后端：Overview 接口存在 N+1 查询

| 维度 | 详情 |
|------|------|
| **位置** | `FundamentalAnalysisService.java#toAnnualMetricDto` → `mergePrecomputedMetrics` |
| **问题** | 对每只股票的近5年指标，`mergePrecomputedMetrics` 逐年被调用一次 `findByStockCodeAndYear`，形成 N+1。 |
| **影响** | 延迟增加；网络往返 × 5。 |
| **修复建议** | 在 `toOverviewResponse` 中一次性调用 `stockFundamentalMetricsRepository.findByStockCode(stockCode, 5)`，返回 `Map<Integer, StockFundamentalMetrics>` 按 `reportYear` 索引，然后传入 `toAnnualMetricDto` 做合并。 |

### M2. 后端：行业排名在内存全量排序

| 维度 | 详情 |
|------|------|
| **位置** | `FundamentalAnalysisService.java#getIndustryRank` |
| **问题** | SQL 先按 `stock_code ASC` 取回同行业所有公司，再在 Java 中按用户指定字段排序。对于大行业（如银行，40+家）尚可，但随着数据增长会成为瓶颈。 |
| **影响** | 内存占用；DB 排序优势未利用。 |
| **修复建议** | 在 Repository 层实现带白名单校验的动态 `ORDER BY`（如 `ORDER BY sfm.roe DESC NULLS LAST`），并限制返回条数（如 Top 100）。 |

### M3. 采集：ROE/ROA 使用期末值而非期间平均值

| 维度 | 详情 |
|------|------|
| **位置** | `collector/collector/tasks/fundamental_metrics_task.py` |
| **问题** | `roe = parent_net_profit / total_equity`、`roa = parent_net_profit / total_assets`、`asset_turnover = operate_income / total_assets` 均使用期末资产负债表数值。标准财务分析应使用平均值 `(期初 + 期末) / 2`，否则对快速扩张/收缩的公司会产生失真。 |
| **影响** | 指标值与专业投研工具存在系统性偏差。 |
| **修复建议** | 计算时读取上一年同期的 `total_equity` 和 `total_assets`，取平均值作为分母。注意首年无数据时回退到期末值。 |

### M4. 采集：增量模式仍加载全部历史年份

| 维度 | 详情 |
|------|------|
| **位置** | `fundamental_metrics_task.py#run_incremental` → `_compute_stock_metrics` |
| **问题** | 增量模式只需计算最近一个完整年报年度，但 `_compute_stock_metrics` 仍查询该股票所有年份数据，计算全部后再过滤。 |
| **影响** | 不必要的数据库加载和计算。 |
| **修复建议** | 为 `_compute_stock_metrics` 增加可选参数 `loadYears?: List[int]`，增量模式下只查询 `target_year` 和 `target_year - 1`。 |

### M5. 采集：批次写入失败时重复计数

| 维度 | 详情 |
|------|------|
| **位置** | `fundamental_metrics_task.py#_process_codes` |
| **问题** | 单只股票计算失败时 `total_failed += 1`；若整批写入失败，又 `total_failed += len(batch)`，导致失败数可能超过实际股票数。 |
| **影响** | 监控数据失真。 |
| **修复建议** | 区分「计算失败」和「写入失败」，或仅累加写入失败时真正出错的股票数（可用 set 记录失败股票代码）。 |

### M6. 数据库：冗余索引 + 缺少部分索引

| 维度 | 详情 |
|------|------|
| **位置** | `V3__add_stock_fundamental_metrics.sql` |
| **问题** | ① `idx_sfm_stock_code` 与 `idx_sfm_stock_year_deleted` 在左前缀规则下重复；② `idx_sfm_is_deleted` 是低基数的布尔索引，PostgreSQL 几乎不会使用；③ 缺少 `WHERE is_deleted = FALSE` 的部分索引。 |
| **影响** | 索引维护开销；写入性能下降。 |
| **修复建议** | 删除 `idx_sfm_stock_code` 和 `idx_sfm_is_deleted`；新增：```sql CREATE INDEX idx_sfm_active ON stock_fundamental_metrics(stock_code, report_year) WHERE is_deleted = FALSE;``` |

### M7. 前端：格式化函数在多处重复定义

| 维度 | 详情 |
|------|------|
| **位置** | `FundamentalAnalysisView.vue`、`IndustryRankTable.vue`、`IndustryPeersTable.vue` 等 |
| **问题** | `formatMoney`、`formatPercent` 在每个文件中独立实现，逻辑不完全一致（如单位阈值、小数位）。 |
| **影响** | 维护困难；同一数值在不同组件展示格式可能不同。 |
| **修复建议** | 提取到 `frontend/src/utils/format.ts`：```ts export function formatMoney(val?: number): string { ... } export function formatPercent(val?: number): string { ... }``` |

### M8. 前端：四个图表组件存在大量重复代码

| 维度 | 详情 |
|------|------|
| **位置** | `ProfitabilityChart.vue`、`CostExpenseChart.vue`、`BalanceSheetChart.vue`、`CashFlowChart.vue` |
| **问题** | 约 90% 代码相同：echarts `use([...])`、tooltip formatter 模式、双Y轴配置、scoped CSS。任何样式调整需改4处。 |
| **影响** | 技术债务；容易漏改。 |
| **修复建议** | 提取为 `MetricChart.vue` 通用组件或 `useChartOption` composable，通过 props 接收 `title`、`series`、`yAxisFormat`。 |

---

## 四、低优先级问题（Low）

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| L1 | `StockFundamentalMetrics.java` | 缺少 `@Table` / `@Id` 注解（Spring Data JDBC 约定） | 补充 `@Table("stock_fundamental_metrics")` 和 `@Id` |
| L2 | `StockFundamentalMetricsRepositoryImpl.java` | `batchUpsert` 日志使用 `metrics.get(0).getStockCode()`，混批时误导 | 改为只记录数量，或按 stockCode 分组后分批写入 |
| L3 | `FundamentalAnalysisService.java` | `mergePrecomputedMetrics` 吞掉所有异常（`catch Exception e`） | 只捕获 `DataAccessException`，并把异常对象传入日志 |
| L4 | `FundamentalAnalysisService.java` | `getIndustryRank` 单方法过长（~100行） | 拆分为「查行业」「查数据」「排序算排名」三个私有方法 |
| L5 | `FundamentalAnalysisService.java` | `safePercentage` 精度为 4 位小数，百分比通常只需 2 位 | 根据前端展示需求统一调整 scale |
| L6 | `ResearchController.java` | `screenCompanies` 与服务层重复调用 `PageUtils.normalize` | 只在 Controller 或 Service 一处做规范化 |
| L7 | `ResearchController.java` | 每次请求打 `info` 级日志 | 改为 `debug`，减少生产环境噪音 |
| L8 | `FundamentalScreener.vue` | `debounceTimer` 未在组件卸载时清理 | `onBeforeUnmount(() => clearTimeout(debounceTimer))` |
| L9 | `FundamentalAnalysisView.vue` | 加载失败后同一只股票无法重新点击 | 错误时清空 `selectedStock` 或添加重试机制 |
| L10 | `FundamentalAnalysisView.vue` | `height: calc(100vh - 64px)` 硬编码头部高度 | 使用 CSS flex 或 CSS 变量动态计算 |
| L11 | `AnnualMetricDto.java` | 缺少 `assetGrowthRate` 字段 | 如前端需要，补充字段并在 `mergePrecomputedMetrics` 中映射 |
| L12 | `cli.py` | 模块 docstring 未包含 `fundamental-metrics` 子命令 | 更新 docstring |
| L13 | `V3__add_stock_fundamental_metrics.sql` | `updated_at` 无自动更新触发器 | 如需要，添加 `BEFORE UPDATE` 触发器或依赖应用层维护 |

---

## 五、Top 5 优先修复项（推荐执行顺序）

1. **修复 C2：FinanceTask 重复触发** — 性能影响最直接，改动最小（删一行 + 调度器调整）。
2. **修复 C1/H5：杜邦分析除零与 NaN** — 前端崩溃/展示异常，影响用户体验。
3. **修复 H1：行业排名 500 错误** — 接口鲁棒性问题，线上容易暴露。
4. **修复 H3：行业排名排序未绑定** — 功能缺陷，用户可见。
5. **修复 M3：ROE/ROA 使用平均值** — 数据正确性问题，越早修复历史数据越干净。

---

## 六、代码质量总体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 功能完整性 | ⭐⭐⭐⭐☆ | 阶段B核心功能（同比、杜邦、对比池、行业排名）均已实现 |
| 架构合规性 | ⭐⭐⭐☆☆ | Service 层直接写 SQL 违反 DDD 分层；Repository 职责边界需厘清 |
| 代码复用性 | ⭐⭐☆☆☆ | 前端图表组件大量重复；格式化函数多处拷贝 |
| 边界处理 | ⭐⭐⭐☆☆ | 除零、空值、异常捕获有遗漏；部分问题被静默吞掉 |
| 测试覆盖 | ⭐⭐⭐⭐☆ | 新增 Repository 集成测试较完整；但缺少 Service 层单元测试 |
| 性能意识 | ⭐⭐⭐☆☆ | N+1、内存排序、冗余索引、重复触发等问题存在 |

**总体建议**：优先修复 Critical 和 High 级别问题，随后进行一次前端图表组件的抽象重构（提取通用组件/composable），最后补充 Service 层单元测试和杜邦分析的边界用例测试。
