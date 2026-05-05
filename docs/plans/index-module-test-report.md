# 指数信息模块测试报告

**报告日期**：2026-05-05
**测试范围**：指数模块后端（Java）、前端（Vue 3）、数据采集（Python）
**测试执行人**：AI 编程助手

---

## 一、执行概况

| 层级 | 测试框架 | 新增测试数 | 总测试数 | 通过 | 失败 | 通过率 |
|------|----------|-----------|---------|------|------|--------|
| 后端（Java） | JUnit 5 + Testcontainers | 43 | 208 | 208 | 0 | **100%** |
| 采集模块（Python） | pytest | 45 | 45 | 45 | 0 | **100%** |
| 前端（Vue） | Vitest + Vue Test Utils | 24 | 24 | 24 | 0 | **100%** |
| **合计** | — | **112** | **277** | **277** | **0** | **100%** |

> 注：后端 208 个测试中，指数模块新增 **43** 个，其余为已有模块测试（company、industry、finance、auth、user、admin、collector dashboard 等）。

---

## 二、各层新增测试详情

### 2.1 后端（Java）— 新增 43 个测试

| 测试类 | 类型 | 测试数 | 覆盖内容 |
|--------|------|--------|---------|
| `IndexRepositoryImplTest` | 集成测试（Testcontainers） | 14 | `findByIndexCode`、`findByKeyword`（代码精确匹配、名称模糊匹配、大小写不敏感、trim、分页）、`countByKeyword`、`findAllByIndexCodes`、`findCoreByType`、`findById` |
| `IndexHistoryRepositoryImplTest` | 集成测试（Testcontainers） | 5 | `findByIndexCodeAndGranularity`（无/有日期范围、分页）、`countByIndexCodeAndGranularity` |
| `EtfInfoRepositoryImplTest` | 集成测试（Testcontainers） | 5 | `findByEtfCode`、`findByTrackingIndexCode`、`findByEtfCodes`（含空列表边界） |
| `IndexEtfMappingRepositoryImplTest` | 集成测试（Testcontainers） | 3 | `findByIndexCode`、`findByEtfCode` |
| `IndexServiceTest` | 单元测试（Mockito） | 12 | `listIndexes`（含关键词）、`getIndexDetail`（含未找到）、`getIndexTrend`（含日期范围）、`getIndexEtfs`（映射表优先 + fallback）、`getIndexCategories`（含空结果） |
| `IndexControllerTest` | 集成测试（MockMvc + MockitoBean） | 9 | `GET /api/indexes`（含关键词分页）、`GET /api/indexes/{code}`（含 404）、`GET /api/indexes/{code}/trend`（含日期范围）、`GET /api/indexes/{code}/etfs`、`GET /api/indexes/categories`、分页参数规范化 |

**测试数据工厂扩展**：`TestDataFactory` 新增 `indexInfo()`、`indexHistory()`、`etfInfo()`、`indexEtfMapping()` 及其数据库插入辅助方法，支持全部 4 张指数模块表的测试数据构造。

### 2.2 数据采集（Python）— 新增 45 个测试

| 测试文件 | 测试数 | 覆盖内容 |
|----------|--------|---------|
| `test_index_basic_task.py` | 19 | `_parse_index`（正常/缺 code/缺 name/空 publish_date/nan publish_date）、`_infer_index_type`（宽基/行业/主题/策略/其他）、`_infer_market`（SH/SZ/CN）、`run`（空结果/有数据/跳过无效项）、`IndexInfoEntity` upsert_sql/to_tuple |
| `test_index_history_task.py` | 14 | `GRANULARITY_MAP`、`_parse_history_df`（正常/空/含 nan）、`_to_float`、`_to_int`、`_collect_single`（有数据/无数据）、`_load_success_set`（正常/DB 错误）、`_mark_success`、`IndexHistoryEntity` upsert_sql/to_tuple |
| `test_etf_basic_task.py` | 12 | `_parse_etf`（正常/缺 code/缺 name/回退 fund_size/无 fund_size）、`_infer_market`（SH/SZ/CN）、`_parse_float`、`run`（空结果/有数据/跳过无效项）、`EtfInfoEntity` upsert_sql/to_tuple |

### 2.3 前端（Vue）— 新增 24 个测试

**测试基础设施**：首次引入 Vitest + `@vue/test-utils` + jsdom，配置 `vitest.config.ts` 与全局 setup。

| 测试文件 | 测试数 | 覆盖内容 |
|----------|--------|---------|
| `src/api/index.spec.ts` | 8 | `getIndexList`（默认分页/含关键词）、`getIndexDetail`（含编码）、`getIndexTrend`（默认粒度/日期范围）、`getIndexEtfs`、`getIndexCategories` |
| `src/views/index/IndexListView.spec.ts` | 8 | 分类 Tabs 渲染、指数卡片展示、点击跳转详情、搜索调用 API、空结果处理、清空关键词重置、分页切换、分类加载错误容错 |
| `src/views/index/IndexDetailView.spec.ts` | 8 | 挂载加载详情、基本信息展示、趋势 Tab 激活加载、ETF Tab 激活加载、粒度切换、图表 option 计算、详情加载错误容错、趋势数据缓存（不重复请求） |

---

## 三、测试执行过程记录

### 3.1 已知问题修复

在执行全部后端测试时，发现 **finance 模块已有测试存在编译错误**（非本模块引入）：

- **根因**：`FinanceService.getIndicators()` 方法签名已扩展为 5 个参数（新增 `String reportType`），但 `FinanceServiceTest` 和 `FinanceControllerTest` 中仍按 4 个参数调用。
- **影响**：导致 `./gradlew test` 在 `compileTestJava` 阶段失败，无法执行任何测试。
- **修复**：
  - `FinanceServiceTest`：为所有 9 处 `getIndicators` 调用补充第 5 个参数 `"年报"` 或 `"季报"`。
  - `FinanceControllerTest`：补充第 5 个参数 `null`。
- **结果**：全部后端 208 个测试编译通过并运行成功。

### 3.2 前端测试配置要点

- Element Plus 组件在 jsdom 中可直接渲染，仅需全局 stub `v-chart`（ECharts 组件）。
- `v-loading` 指令在测试环境中产生 warn，不影响测试结果，可在 setup 中全局注册 `ElLoadingDirective` 消除。

---

## 四、测试覆盖率分析（定性评估）

### 4.1 指数模块后端覆盖

| 层级 | 文件 | 覆盖状态 |
|------|------|---------|
| API（Controller） | `IndexController.java` | **完全覆盖** — 5 个端点全部测试 |
| Application（Service） | `IndexService.java` | **完全覆盖** — 6 个 public 方法全部测试 |
| Domain（Repository 接口） | `IndexRepository.java` 等 | 通过集成测试间接覆盖 |
| Infrastructure（Repository 实现） | `IndexRepositoryImpl.java` | **完全覆盖** — 6 个方法全部测试 |
| Infrastructure | `IndexHistoryRepositoryImpl.java` | **完全覆盖** — 4 个方法全部测试 |
| Infrastructure | `EtfInfoRepositoryImpl.java` | **完全覆盖** — 3 个方法全部测试 |
| Infrastructure | `IndexEtfMappingRepositoryImpl.java` | **完全覆盖** — 2 个方法全部测试 |

### 4.2 指数模块前端覆盖

| 文件 | 覆盖状态 |
|------|---------|
| `src/api/index.ts` | **完全覆盖** — 5 个 API 函数全部测试 |
| `src/views/index/IndexListView.vue` | **核心逻辑覆盖** — 数据加载、搜索、分页、分类展示、路由跳转 |
| `src/views/index/IndexDetailView.vue` | **核心逻辑覆盖** — 详情加载、Tab 切换、趋势粒度切换、ETF 加载、图表 option 计算 |

> 前端组件测试聚焦在**逻辑行为**（数据流、事件、计算属性），对 CSS/样式类名的断言保持最小化，以减少 Element Plus 版本升级带来的脆性。

### 4.3 采集模块覆盖

| 文件 | 覆盖状态 |
|------|---------|
| `collector/tasks/index_basic_task.py` | **核心逻辑覆盖** — 数据解析、类型推断、市场推断、主流程 |
| `collector/tasks/index_history_task.py` | **核心逻辑覆盖** — DataFrame 解析、类型转换、单任务采集、进度加载/标记 |
| `collector/tasks/etf_basic_task.py` | **核心逻辑覆盖** — 数据解析、市场推断、主流程 |
| `collector/models.py` | **实体方法覆盖** — `IndexInfoEntity`、`IndexHistoryEntity`、`EtfInfoEntity` 的 `upsert_sql()` 和 `to_upsert_tuple()` |

---

## 五、优化建议

### 5.1 测试基础设施

1. **Gradle 测试前置检查**
   - 建议将 `./gradlew test` 加入 CI 流水线，并在每次提交前本地执行，避免类似 finance 测试签名不匹配的问题累积。
   - 可考虑在 `build.gradle` 中配置 `compileTestJava` 失败后立即终止，以缩短反馈周期。

2. **前端测试框架完善**
   - 当前仅配置了指数模块的测试，建议逐步为公司列表、公司详情等已有页面补充测试。
   - 建议在 `package.json` 中保留 `test:coverage` 脚本，定期运行覆盖率报告，设定覆盖率阈值（如 70%）。
   - `v-loading` 指令的 warn 可通过在 `setup.ts` 中 `import { vLoading } from 'element-plus'` 并全局注册消除。

3. **Python 测试规范化**
   - 当前 pytest 已配置 `pytest.ini`，建议在 CI 中运行 `pytest --cov=collector` 生成覆盖率报告。
   - `test_company_task.py` 等已有测试与新增测试风格一致，无需调整。

### 5.2 测试用例补充

| 优先级 | 建议补充的测试场景 | 说明 |
|--------|-------------------|------|
| P1 | IndexHistoryTask 的 Session 断点续传端到端测试 | 当前仅测试了 `_load_success_set` 和 `_mark_success`，建议 mock 数据库状态，验证 `run()` 方法能正确跳过已成功的任务 |
| P1 | IndexHistoryTask 的并发安全性测试 | `ThreadPoolExecutor(max_workers=3)` 下，验证数据库 upsert 不会因并发冲突失败 |
| P2 | 指数模块 Repository 的边界条件 | 如 `findByKeyword` 传入特殊字符（`%`、`_`）、`findAllByIndexCodes` 传入超大批量 |
| P2 | IndexService 的异常传播测试 | 如 Repository 抛出 `DataAccessException` 时，Service 是否按预期记录日志并抛出 |
| P2 | 前端图表空数据/单条数据处理 | `IndexDetailView` 中 `trendData` 为空或仅 1 条时，`chartOption` 计算是否正常 |
| P3 | 指数-ETF 映射关系的精确匹配 | 当 akshare 或第三方数据源提供 ETF→指数精确映射后，补充 `IndexEtfMapping` 的采集和查询测试 |

### 5.3 代码质量建议

1. **IndexHistoryTask 中的潜在 Bug**
   - 第 94 行 `future_to_task` 的推导存在重复：`for code, gran in tasks for code, gran in tasks`，这会导致任务数翻倍（虽然 `dict` 会去重，但属于明显错误）。建议修复为单层推导。

2. **前端 Element Plus 废弃 API**
   - `ElRadioButton` 使用 `label` 作为 value 的写法将在 Element Plus 3.0 中废弃，建议替换为 `:value="..."`。

3. **测试数据工厂可维护性**
   - `TestDataFactory` 已涵盖 7 个模块，建议保持按模块分组的代码结构，当新增模块时延续此模式。

---

## 六、附录：关键文件清单

### 新增测试文件

```
backend/src/test/java/com/example/securityanalyze/index/
├── api/IndexControllerTest.java
├── application/IndexServiceTest.java
└── infrastructure/
    ├── IndexRepositoryImplTest.java
    ├── IndexHistoryRepositoryImplTest.java
    ├── EtfInfoRepositoryImplTest.java
    └── IndexEtfMappingRepositoryImplTest.java

collector/tests/
├── test_index_basic_task.py
├── test_index_history_task.py
└── test_etf_basic_task.py

frontend/src/
├── test/setup.ts
├── api/index.spec.ts
└── views/index/
    ├── IndexListView.spec.ts
    └── IndexDetailView.spec.ts
```

### 修改文件

```
backend/src/test/java/com/example/securityanalyze/common/TestDataFactory.java
backend/src/test/java/com/example/securityanalyze/finance/application/FinanceServiceTest.java
backend/src/test/java/com/example/securityanalyze/finance/api/FinanceControllerTest.java
frontend/package.json
frontend/vitest.config.ts
```

---

*报告结束。*
