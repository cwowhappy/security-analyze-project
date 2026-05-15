# 采集器架构设计 v3

> 本文档描述 Python 采集器的内部架构。v3.0 在 v2.0 的脚本集合基础上，升级为**配置驱动的通用采集管道框架**。  
> 核心变化：新增 `AdaptiveRequestEngine`（智能调速）、`FieldMapper`（配置化字段映射）、`SourceFallbackPipeline`（多源串行 fallback）、`StockCollectionStateTracker`（stock 级状态持久化）。

---

## 一、总体架构

采集器作为独立后台进程运行，内部采用四层管道架构：

```
CLI / API 入口
    ↓
TaskExecutor（任务调度 + 状态机）
    ↓
CollectionTaskHandler（按 task_type + mode 分派）
    ↓
SourceFallbackPipeline（多源串行 fallback + 字段合并）
    ← AdaptiveRequestEngine（智能调速 + 重试）
    ← FieldMapper（字段映射 + 转换 + 空值策略）
    ↓
DataSourceAdapter（AKShare / Tushare / Calculated）
    ↓
Domain Models + DB Repositories
```

---

## 二、组件职责

### 2.1 TaskExecutor + 注册表

任务执行器负责根据 `(task_type, mode)` 从注册表查找并调用对应的处理器。

```python
# 注册表示例
_TASK_REGISTRY: dict[tuple[str, str], tuple[Callable, str]] = {}

@register_task("stock_basic", mode="full", data_source="akshare")
def handle_stock_basic_full(task: CollectionTask, settings: Settings) -> dict:
    ...
```

| 字段 | 说明 |
|------|------|
| `task_type` | 纯数据类型：`stock_basic`、`company_info`、`financial_income` 等 |
| `mode` | `full`（全量批量）或 `single`（单只股票/公司） |
| `source_priority` | 有序数据源列表，如 `["akshare", "tushare"]` |

### 2.2 AdaptiveRequestEngine（自适应请求引擎）

为每个数据源独立维护动态 delay 状态：

- **初始值**：`delay = random.uniform(min_delay, max_delay)`
- **请求成功**：`consecutive_success += 1`；若达到阈值（默认 10），`delay = max(delay * 0.9, min_delay)`
- **可恢复错误（429/Timeout/503）**：`consecutive_success = 0`；`delay = min(delay * 2 + jitter, max_delay)`；重试该请求（最多 3 次）
- **不可恢复错误（404/业务错误）**：`NonRecoverableError`，不重试，立即 fallback

### 2.3 SourceFallbackPipeline（数据源降级管道）

按 `source_priority` 串行调度适配器：

1. 对每个 source，调用适配器 `fetch(stock_code, source_config)`
2. 适配器内部通过 `AdaptiveRequestEngine` 包装调速和重试
3. 第一个成功返回的 `mapped_record` 作为 **base_record**
4. 后续 source 只补充 base_record 中为 `None` 或空字符串的字段（**非空不覆盖**）
5. 所有 source 均失败，该 stock 标记为 `failed`

### 2.4 FieldMapper（字段映射器）

按 YAML 配置将原始数据转换为标准化记录：

```yaml
# 配置示例
field_mapping:
  - api_field: "代码"
    db_field: "stock_code"
    converter: "str"
    null_policy: "fail"
```

| 属性 | 说明 |
|------|------|
| `api_field` | 接口原始字段名，支持多别名列表 |
| `db_field` | 数据表字段名 |
| `converter` | 转换器：`str`/`int`/`float`/`date`/`decimal`/`shares_10k`/`capital` 等 |
| `null_policy` | `skip`（保持 None）/`default`（填充默认值）/`fail`（触发 source 失败） |

### 2.5 StockCollectionStateTracker（采集状态追踪器）

为 `full` 模式提供 stock 级状态持久化：

```sql
CREATE TABLE tb_collection_stock_state (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES tb_collection_task(id),
    stock_code VARCHAR(20) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- pending / success / failed / skipped
    error_message TEXT,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(task_id, stock_code, task_type)
);
```

- 按 `batch_size`（默认 20）批次缓冲，满批次后 `flush` 到数据库
- `full` 模式重启时，跳过 `success` 且未超过 TTL 的记录
- `single` 模式无视 TTL，强制更新

### 2.6 DataSourceAdapter（数据源适配器）

统一适配器协议：

```python
class DataSourceAdapter(Protocol):
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        """调用外部 API，返回原始字段字典。"""
```

| 适配器 | 数据源 | 数据类型 |
|--------|--------|----------|
| `StockBasicAkshareAdapter` | AKShare | `stock_basic` |
| `StockBasicTushareAdapter` | Tushare | `stock_basic` |
| `CompanyInfoAkshareAdapter` | AKShare | `company_info` |
| `CompanyInfoTushareAdapter` | Tushare | `company_info` |
| `FinancialSinaAdapter` | AKShare/Sina | `financial_income`/`balance`/`cashflow` |
| `FinancialIndicatorCalculatedAdapter` | 计算衍生 | `financial_indicator` |

---

## 三、执行流程

### 3.1 full 模式执行流程

```
TaskExecutor 创建 CollectionTask (mode=full, task_type=xxx)
    ↓
获取目标股票全量列表
    ↓
查询 tb_collection_stock_state，过滤已处理且未过期的记录
    ↓
按 batch_size 分批次处理
    ↓
对每只股票：
    SourceFallbackPipeline 按 source_priority 逐个尝试
        → 成功：写入内存缓冲区
        → 失败：记录错误，继续下一只
    每满一个批次：bulk_upsert 到 tb_collection_stock_state
    计算该批次失败率，超过阈值则熔断
    ↓
全部完成后，Task 状态更新为 SUCCESS
```

### 3.2 single 模式执行流程

```
task_params 传入 stock_code
    ↓
跳过状态过滤，直接进入 SourceFallbackPipeline
    ↓
处理完成后更新/插入 tb_collection_stock_state
    ↓
Task 标记为 SUCCESS
```

### 3.3 复合任务执行流程（financial_full）

`financial_full` 是 orchestrator，顺序执行 4 个子阶段：

```
对每只待处理股票：
    1. financial_income → 利润表
    2. financial_balance → 资产负债表
    3. financial_cashflow → 现金流量表
    4. financial_indicator → 计算衍生指标

规则：
- income/balance/cashflow 任一失败 → 跳过 indicator
- 每完成一个子阶段立即 flush 状态（task_type 为子阶段类型）
- 重启时精确跳过各子阶段中已成功且未过期的记录
```

---

## 四、配置项

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `collection_ttl_hours` | `COLLECTION_TTL_HOURS` | `24` | stock 级状态有效期 |
| `collection_batch_size` | `COLLECTION_BATCH_SIZE` | `20` | 批处理大小，兼作状态提交批次 |
| `adaptive_min_delay` | `ADAPTIVE_MIN_DELAY` | `1.0` | 最小调用间隔（秒） |
| `adaptive_max_delay` | `ADAPTIVE_MAX_DELAY` | `60.0` | 最大调用间隔（秒） |
| `adaptive_backoff_jitter` | `ADAPTIVE_BACKOFF_JITTER` | `0.5` | 退避抖动范围（秒） |
| `adaptive_success_threshold` | `ADAPTIVE_SUCCESS_THRESHOLD` | `10` | 连续成功多少次后尝试降速 |
| `retry_max_attempts` | `RETRY_MAX_ATTEMPTS` | `3` | 单个请求最大重试次数 |
| `batch_fail_threshold` | `BATCH_FAIL_THRESHOLD` | `0.1` | 批次失败率阈值 |

---

## 五、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| **v3.0** | **2026-05-13** | **通用采集管道：新增 AdaptiveRequestEngine、FieldMapper、SourceFallbackPipeline、StockCollectionStateTracker；任务类型语义化；字段映射外置到 YAML；多数据源串行 fallback；断点恢复支持 TTL** |
| v2.0 | 2026-05-11 | 简化架构：去除 FastAPI、适配器抽象、实时降级；改为顺序脚本 + pending 轮询 |
| v1.0 | 2026-05-10 | 初始版本（已废弃） |
