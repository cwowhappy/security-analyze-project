# 数据采集模块重构设计：通用采集管道

> 日期：2026-05-13  
> 范围：`collector/` 模块  
> 目标：将脚本集合升级为配置驱动的通用采集管道框架

---

## 1. 背景与目标

当前 `collector` 模块采用 `TaskExecutor` + 注册表模式，脚本层面已具备基础能力（字段别名映射、批量失败熔断、部分 resume 逻辑）。但随着数据类型和数据源增多，硬编码的字段提取、固定间隔的 API 调用、单一数据源依赖等问题日益突出。

本次重构旨在实现以下目标：

- **单股票独立更新**：`single` 模式支持针对单只股票的精准更新
- **批量断点恢复**：`full` 模式支持进程中断后精确恢复，无需重复处理已成功且在有效期内的记录
- **任务类型语义化**：以数据类型划分任务类型，`full`/`single` 作为执行子模式
- **自适应调速**：根据接口限流/错误信号自动调节调用间隔，动态收敛到最优频率
- **多数据源兜底**：同一数据类型支持多数据源串行 fallback，非空字段不覆盖
- **字段映射外置化**：采集接口字段名 ↔ 数据表字段的映射、空值处理、格式转换全部外置到 YAML 配置

---

## 2. 架构总览

重构后架构分为四层：

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
DataSourceAdapter（AKShare / Tushare / ...）
    ↓
Domain Models + DB Repositories
```

### 2.1 新增核心组件

| 组件 | 职责 |
|------|------|
| `AdaptiveRequestEngine` | 为每个数据源独立维护动态 delay 状态，实现智能调速 |
| `SourceFallbackPipeline` | 串行调度适配器，主源 fallback + 空字段补充 |
| `FieldMappingConfigLoader` | 从 YAML 加载字段映射、转换器、空值策略配置 |
| `StockCollectionStateTracker` | 为 `full` 模式提供 stock 级状态持久化，支持 TTL 过期 |
| `FieldMapper` | 将适配器原始数据按配置转换为标准化记录 |

---

## 3. 领域模型与数据库变更

### 3.1 CollectionTask 扩展

`CollectionTask` 新增字段：

- `mode: str` — 执行模式：`full`（全量批量）或 `single`（单只股票）
- `source_priority: list[str]` — 有序数据源列表，如 `["akshare", "tushare"]`，持久化为 JSON

`task_type` 收敛为纯数据类型：

- `stock_basic` — 股票基础信息
- `company_info` — 公司信息
- `financial_income` — 利润表
- `financial_balance` — 资产负债表
- `financial_cashflow` — 现金流量表
- `financial_indicator` — 财务指标（计算衍生）

> 注：`financial_full` 作为组合任务（orchestrator）保留，顺序执行 income → balance → cashflow → indicator，内部复用上述通用管道。

### 3.2 新增 tb_collection_stock_state 表

```sql
CREATE TABLE tb_collection_stock_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID REFERENCES tb_collection_task(id),
    stock_code VARCHAR(20) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'success', 'failed', 'skipped')),
    error_message TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(task_id, stock_code, task_type)
);
```

`full` 模式启动时，查询此表过滤出：
- 状态非 `success` 的股票
- 状态为 `success` 但 `updated_at < NOW() - TTL` 的股票

### 3.3 tb_collection_task 表变更

```sql
ALTER TABLE tb_collection_task
    ADD COLUMN mode VARCHAR(20),
    ADD COLUMN source_priority JSONB;
```

---

## 4. 外部配置体系（YAML）

配置文件位于 `collector/config/mappings/`，按 `task_type` 命名，如 `stock_basic.yaml`。

### 4.1 配置结构示例

```yaml
task_type: stock_basic
ttl_hours: 24  # 该类型数据有效期，覆盖全局默认值

sources:
  - name: akshare
    adapter: stock_basic_akshare_adapter
    priority: 1
    field_mapping:
      - api_field: "代码"
        db_field: "stock_code"
        converter: "str"
        null_policy: "skip"
      - api_field: "名称"
        db_field: "name"
        converter: "str"
        null_policy: "fail"
    params:
      api_name: "stock_info_a_code_name"

  - name: tushare
    adapter: stock_basic_tushare_adapter
    priority: 2
    field_mapping:
      - api_field: "ts_code"
        db_field: "ts_code"
        converter: "str"
      - api_field: "industry"
        db_field: "industry"
        converter: "str"
        null_policy: "default"
        default_value: ""
```

### 4.2 字段说明

| 属性 | 说明 |
|------|------|
| `api_field` | 接口返回数据中的原始字段名，支持多别名列表（如 `["资产总计", "总资产", "资产合计"]`） |
| `db_field` | 标准化后的数据表字段名 |
| `converter` | 数据类型转换器：`str`/`int`/`float`/`date`/`datetime`/`shares_10k`/`percent` 等 |
| `null_policy` | 空值处理策略：`skip`（保持 None，可被后续源补充）/`default`（填充默认值）/`fail`（触发 source 失败，fallback） |
| `default_value` | `null_policy=default` 时生效 |

### 4.3 TTL 优先级

按任务类型设置的 TTL 优先级最高：
1. YAML 配置中的 `ttl_hours`
2. `Settings.collection_ttl_hours`（全局配置，默认 24）
3. 硬编码默认值 24

---

## 5. 采集引擎运行时

### 5.1 AdaptiveRequestEngine

为每个数据源独立维护 `DelayState`：

```python
@dataclass
class DelayState:
    current_delay: float
    consecutive_success: int = 0
```

**算法规则**：

- **初始值**：`current_delay = random.uniform(min_delay, max_delay)`
- **请求成功**：`consecutive_success += 1`；若达到 `adaptive_success_threshold`（默认 10），则 `current_delay = max(current_delay * 0.9, min_delay)`，重置计数
- **可恢复错误（429/Timeout/503）**：`consecutive_success = 0`；`current_delay = min(current_delay * 2 + random_jitter, max_delay)`；重试该请求（最多 `retry_max_attempts` 次）
- **不可恢复错误（404/业务错误）**：不重试，立即向上抛

### 5.2 SourceFallbackPipeline

执行流程：

1. 按 `source_priority` 排序后的 sources 依次尝试
2. 对每个 source，调用适配器（`adapter.fetch(stock_code, source_config)`），适配器内部通过 `AdaptiveRequestEngine.sleep_and_execute(source_name, fn)` 包装
3. 适配器返回原始 `Dict[str, Any]`，交由 `FieldMapper` 处理为 `mapped_record`
4. 若当前 source 处理失败（适配器异常 / `null_policy=fail` 触发），记录失败原因，继续下一个 source
5. 第一个成功返回的 `mapped_record` 作为 **base_record**
6. 后续 source 只补充 base_record 中为 `None` 或空字符串的字段（**非空不覆盖**）
7. 所有 source 均失败，该 stock 标记为 `failed`

---

## 6. 断点恢复与任务执行数据流

### 6.1 full 模式

1. `TaskExecutor` 创建 `CollectionTask`（`mode=full`, `task_type=xxx`, `source_priority=[...]`），状态 `RUNNING`
2. 任务处理器获取目标股票全量列表
3. **过滤**：查询 `tb_collection_stock_state`，排除 `success` 且未过期的记录
4. 按 `collection_batch_size`（默认 20）分批次处理
5. 每只股票处理完成后，状态写入内存缓冲区
6. 每满一个批次，批量 `flush` 到 `tb_collection_stock_state`，同时计算该批次失败率
7. 若某批次失败率超过 `batch_fail_threshold`（默认 10%），触发熔断：Task 标记 `FAILED`，已处理数据保留，未处理股票保持 `pending`
8. 全部完成后，Task 状态 `SUCCESS`

### 6.2 single 模式

1. `task_params` 传入 `stock_code`
2. 跳过状态过滤，直接进入 `SourceFallbackPipeline`
3. 处理完成后更新/插入 `tb_collection_stock_state`
4. Task 标记 `SUCCESS`

---

## 7. 错误处理与熔断

| 错误类型 | 处理方式 | 是否计入熔断 |
|----------|----------|--------------|
| 429 / Timeout / 503 | 适配器内重试 3 次，仍失败则 source fallback | 是（stock 最终失败） |
| 404 / 数据不存在 | 不可恢复，直接失败 | 是 |
| `null_policy=fail` | 该 source 失败，fallback 下一源；若全部失败则 stock 失败 | 是 |
| 字段转换异常 | 该 source 失败，fallback 下一源 | 是 |

**熔断后行为**：
- Task 状态设为 `FAILED`
- `error_message` 记录熔断批次及失败明细
- 已 flush 到库的 `tb_collection_stock_state` 状态永久保留
- 未处理股票保持 `pending`，下次任务重启时自动恢复

---

## 8. 适配器标准化与迁移路径

### 8.1 适配器接口

```python
class DataSourceAdapter(Protocol):
    def fetch(self, stock_code: str, source_config: SourceConfig) -> Dict[str, Any]:
        ...
```

### 8.2 迁移策略

- **全量替换**：旧脚本（`stock_full.py`, `company_full.py` 等）直接改造为适配器实现类，不复用 Legacy 模式
- **配置抽取**：将现有脚本中的字段别名、类型转换、空值判断逻辑迁移到 YAML 配置
- **适配器瘦身**：适配器仅负责"调 API + 返回原始 Dict/DataFrame"，其余逻辑下沉到 `FieldMapper`
- **CLI 兼容**：`cli.py` 保留现有子命令作为快捷入口，内部转换为新的 `CollectionTask` 语义调用 `TaskExecutor`

### 8.3 迁移清单

| 旧脚本 | 新适配器 | 数据类型 |
|--------|----------|----------|
| `stock_full.py` | `stock_basic_akshare_adapter` + `stock_basic_tushare_adapter` | `stock_basic` |
| `company_full.py` | `company_info_akshare_adapter` | `company_info` |
| `field_supplement.py` | 拆分为对应类型的 tushare 补充 source | 多类型 |
| `financial_income.py` | `financial_income_akshare_adapter` | `financial_income` |
| `financial_balance.py` | `financial_balance_akshare_adapter` | `financial_balance` |
| `financial_cashflow.py` | `financial_cashflow_akshare_adapter` | `financial_cashflow` |
| `financial_indicator.py` | `financial_indicator_calculated_adapter` | `financial_indicator` |
| `financial_full.py` | 保留为 orchestrator，复用上述适配器 | `financial_full` |

### 8.4 Composite Task 设计（financial_full）

`financial_full` 是一种特殊的组合任务（orchestrator），本身不直接映射单一数据表，而是按固定顺序编排 4 个原子子阶段：`financial_income` → `financial_balance` → `financial_cashflow` → `financial_indicator`。

#### 8.4.1 与通用管道的关系

`financial_full` 不直接实例化 `SourceFallbackPipeline`，而是通过内部构造子任务（或直接调用子 handler）来复用相同的采集引擎。每个子阶段拥有独立的 YAML 配置（`financial_income.yaml`、`financial_balance.yaml` 等），支持独立的数据源、字段映射和 TTL 设置。

#### 8.4.2 执行流程

**对单只股票的顺序执行逻辑**：

1. 调用 `financial_income` handler 采集利润表
2. 调用 `financial_balance` handler 采集资产负债表
3. 调用 `financial_cashflow` handler 采集现金流量表
4. 调用 `financial_indicator` handler，读取上述三表数据计算衍生指标

每个子阶段完成后立即持久化到对应数据表（`tb_financial_income`、`tb_financial_balance` 等），**不等待后续阶段**。这保证了即使组合任务中断，已完成的子阶段数据不会丢失。

#### 8.4.3 子阶段状态管理

`financial_full` 任务拥有自身的 `CollectionTask` 记录（`task_type=financial_full`）。同时，每只股票在每个子阶段完成后，以该子阶段的 `task_type` 写入 `tb_collection_stock_state`：

| stock_code | task_type | status | updated_at |
|------------|-----------|--------|------------|
| 000001 | financial_income | success | 2026-05-13 10:00 |
| 000001 | financial_balance | success | 2026-05-13 10:01 |
| 000001 | financial_cashflow | failed | 2026-05-13 10:02 |
| 000001 | financial_indicator | pending | — |

这种设计的优势是：
- 子阶段可被独立的 `single` 模式直接调用（如只更新某只股票的利润表）
- `financial_full` 重启时，可精确跳过各子阶段中已成功且未过期的记录

#### 8.4.4 失败与跳过策略

**单股票失败传播**：
- 若 `financial_income` 失败：该股票跳过 `balance` / `cashflow` / `indicator`（因为后三者依赖或可与利润表交叉校验），该股票整体标记为 `failed`
- 若 `financial_balance` 失败：跳过 `cashflow` / `indicator`，但 `income` 已持久化的数据保留
- 若 `financial_cashflow` 失败：跳过 `indicator`，`income` 和 `balance` 保留
- 若 `financial_indicator` 失败：前三表数据不受影响

**批次熔断**：`financial_full` 的批次失败率按子阶段独立计算。例如 income 阶段某批次失败率超过阈值，则 income 阶段熔断， Task 整体标记 `FAILED`，但已完成的 balance/cashflow/indicator 数据保留。重启时从 income 阶段继续。

#### 8.4.5 断点恢复

`full` 模式重启时，对每只待处理股票：
1. 查询 `tb_collection_stock_state`，获取该股票 4 个子阶段的状态和更新时间
2. 跳过所有子阶段均为 `success` 且未过期的股票
3. 对存在 `pending` / `failed` / 过期的子阶段，从最早的缺失阶段开始顺序补全

例如：某股票 income 和 balance 成功且未过期，但 cashflow 已过期，则只重新采集 `cashflow` 和 `indicator`。

---

## 9. 配置扩展

### 9.1 新增 Settings 项

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `collection_ttl_hours` | `COLLECTION_TTL_HOURS` | `24` | 全局默认 TTL |
| `collection_batch_size` | `COLLECTION_BATCH_SIZE` | `20` | 批处理大小，兼作状态提交批次 |
| `adaptive_min_delay` | `ADAPTIVE_MIN_DELAY` | `1.0` | 最小调用间隔（秒） |
| `adaptive_max_delay` | `ADAPTIVE_MAX_DELAY` | `60.0` | 最大调用间隔（秒） |
| `adaptive_backoff_jitter` | `ADAPTIVE_BACKOFF_JITTER` | `0.5` | 退避抖动范围（秒） |
| `adaptive_success_threshold` | `ADAPTIVE_SUCCESS_THRESHOLD` | `10` | 连续成功多少次后尝试降速 |
| `retry_max_attempts` | `RETRY_MAX_ATTEMPTS` | `3` | 单个请求最大重试次数 |

### 9.2 数据源级覆盖

每个 source 的 YAML 配置可局部覆盖 `min_delay` 和 `max_delay`：

```yaml
sources:
  - name: tushare
    min_delay: 2.0
    max_delay: 120.0
```

---

## 10. 测试策略

### 10.1 单元测试

| 测试文件 | 覆盖内容 |
|----------|----------|
| `test_adaptive_request_engine.py` | delay 状态机：错误退避曲线、成功收敛曲线、抖动边界 |
| `test_source_fallback_pipeline.py` | 串行 fallback、非空补充、全源失败、部分字段补充 |
| `test_field_mapper.py` | converter 注册表、null_policy 策略、多别名提取、转换异常 |
| `test_config_loader.py` | YAML 加载、schema 校验、无效配置报错 |
| `test_stock_state_tracker.py` | 批次缓冲、flush 逻辑、TTL 过期判断 |

### 10.2 集成测试

- mock adapter 替代真实 AKShare/Tushare，构造模拟 DataFrame/Dict
- 验证 `full` 模式中断恢复：模拟处理 50 只股票后异常退出，重启任务后已处理且未过期的 50 只被跳过，过期和未处理的继续采集
- 验证 `single` 模式强制更新：状态为 `success` 的股票仍被重新采集

### 10.3 数据迁移验证

- Flyway SQL 在本地 PostgreSQL 执行，验证新旧字段共存期间 CLI 行为一致性

**目标**：新增代码行覆盖率 ≥ 85%。

---

## 11. 实施阶段建议

### 阶段一：基础设施（1 天）
- 数据库迁移：新增字段 + 新表
- 配置加载器：`FieldMappingConfigLoader` + YAML schema 定义
- 转换器注册表：基础 converter 实现

### 阶段二：核心引擎（2 天）
- `AdaptiveRequestEngine` 实现
- `FieldMapper` 实现
- `SourceFallbackPipeline` 实现
- `StockCollectionStateTracker` 实现

### 阶段三：适配器迁移（2 天）
- 编写 `stock_basic` 和 `company_info` 的 YAML 配置 + 新适配器
- 改造 `TaskExecutor` 和 `CollectionTaskHandler` 支持 `mode` + `source_priority`
- CLI 兼容层改造

### 阶段四：财务数据迁移（2 天）
- `financial_income` / `financial_balance` / `financial_cashflow` / `financial_indicator` 配置 + 适配器
- `financial_full` orchestrator 适配新架构

### 阶段五：测试与验收（2 天）
- 单元测试补全
- 集成测试（mock 场景）
- 本地 PostgreSQL 全链路验证
