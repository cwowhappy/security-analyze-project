# 数据采集模块

基于 Poetry + Python 3.11 构建的股票数据采集模块（v3.0 通用采集管道）。

## 技术栈

- Python 3.11
- Poetry（依赖管理）
- Pydantic + pydantic-settings（数据验证与配置）
- PyYAML（字段映射配置）
- structlog（结构化日志）
- pytest（单元测试）
- Ruff（代码检查 + 格式化）

## 项目结构

```
src/data_collector/
├── config.py                    # pydantic-settings 全局配置
├── cli.py                       # CLI 入口
├── task_executor.py             # 任务执行器（按 task_type + mode 路由）
├── core/
│   ├── domain/                  # 领域模型（Stock、Company、CollectionTask 等）
│   ├── config/                  # 配置加载器
│   │   └── field_mapping_config.py    # YAML 字段映射配置解析
│   └── pipeline/                # 通用采集管道核心组件
│       ├── adaptive_request_engine.py # 自适应请求调速
│       ├── field_mapper.py      # 字段映射与转换
│       ├── source_fallback_pipeline.py # 多数据源串行 fallback
│       ├── stock_state_tracker.py      # stock 级状态追踪（断点恢复）
│       └── converters.py        # 字段转换器注册表
├── adapters/                    # 仓库实现 + 数据源适配器
│   ├── db_stock_repository.py
│   ├── db_company_repository.py
│   ├── db_collection_task_repository.py
│   ├── db_stock_state_repository.py
│   ├── data_source_adapter.py   # 适配器协议（Protocol）
│   ├── stock_basic_akshare_adapter.py
│   ├── stock_basic_tushare_adapter.py
│   ├── company_info_akshare_adapter.py
│   ├── company_info_tushare_adapter.py
│   ├── financial_sina_adapter.py
│   └── financial_indicator_calculated_adapter.py
├── scripts/                     # 采集脚本（Legacy，逐步迁移中）
│   ├── stock_full.py
│   ├── company_full.py
│   ├── field_supplement.py
│   └── financial_*.py
├── config/mappings/             # YAML 字段映射配置
│   ├── stock_basic.yaml
│   ├── company_info.yaml
│   ├── financial_income.yaml
│   ├── financial_balance.yaml
│   ├── financial_cashflow.yaml
│   └── financial_indicator.yaml
└── infrastructure/              # 基础设施层
    ├── db.py                    # PostgreSQL 连接池
    └── logging/                 # 日志配置
```

## 核心架构（v3.0 通用采集管道）

### 总体架构

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

### 核心组件

| 组件 | 职责 |
|------|------|
| `AdaptiveRequestEngine` | 为每个数据源独立维护动态 delay 状态，实现"错误指数退避 + 成功逐步收敛"的智能调速 |
| `SourceFallbackPipeline` | 按 `source_priority` 串行调度适配器，主源 fallback + 空字段补充（非空不覆盖） |
| `FieldMapper` | 按 YAML 配置将原始 API 数据转换为标准化记录，支持类型转换和空值策略 |
| `StockCollectionStateTracker` | 为 `full` 模式提供 stock 级状态持久化，支持 TTL 过期判断和批次缓冲 flush |
| `FieldMappingConfigLoader` | 从 `config/mappings/*.yaml` 加载字段映射、转换器、空值策略配置 |
| `Converter 注册表` | 可扩展的字段值转换器（`str`/`int`/`float`/`date`/`decimal`/`shares_10k`/`capital` 等） |

### 任务类型语义化

`task_type` 收敛为纯数据类型，`mode`（full/single）和 `source_priority` 作为执行参数：

| task_type | mode=full | mode=single |
|-----------|-----------|-------------|
| `stock_basic` | 全量股票列表采集 | 单只股票更新 |
| `company_info` | 全量公司信息采集 | 单条公司更新 |
| `financial_income` | 全量利润表采集 | 单只股票利润表 |
| `financial_balance` | 全量资产负债表采集 | 单只股票资产负债表 |
| `financial_cashflow` | 全量现金流量表采集 | 单只股票现金流量表 |
| `financial_indicator` | 全量财务指标计算 | 单只股票财务指标 |
| `financial_full` | 全量三表+指标组合采集 | 单只股票组合采集 |

### 数据源 Fallback 与补充

同一数据类型支持多数据源配置，按 `source_priority` 顺序串行 fallback：

```yaml
# stock_basic.yaml 示例
sources:
  - name: akshare
    adapter: stock_basic_akshare_adapter
    priority: 1
    # ...
  - name: tushare
    adapter: stock_basic_tushare_adapter
    priority: 2
    # ...
```

- 第一个成功返回的数据源建立 **base_record**
- 后续数据源只补充 base_record 中为 `None` 或空字符串的字段（**非空不覆盖**）
- 若所有数据源均失败，该 stock 标记为 `failed`

### 断点恢复机制

`full` 模式支持三层恢复粒度：

1. **Task 级**：任务整体状态（RUNNING/SUCCESS/FAILED）
2. **Stock 级**：每只股票的处理状态记录在 `tb_collection_stock_state`
   - 状态为 `success` 且未超过 TTL 的股票自动跳过
   - 状态为 `failed` / `pending` / 已过期的股票重新采集
3. **请求级**：单只股票的接口调用失败时，由 `AdaptiveRequestEngine` 自动重试 2-3 次

### 自适应调速

替代固定 `time.sleep(random.uniform(1, 3))`：

- **初始**：`delay = random.uniform(min_delay, max_delay)`
- **成功**：连续 N 次成功后，`delay *= 0.9`，逐步降低间隔
- **失败（可恢复）**：`delay = min(delay * 2 + jitter, max_delay)`，触发重试
- **失败（不可恢复）**：`NonRecoverableError`，不重试立即 fallback

每个数据源独立维护 delay 状态。

## 开发规范

### 1. 代码规范（Ruff）

使用 **Ruff** 统一替代 Black + isort + flake8：

```bash
poetry run ruff check .          # 代码检查
poetry run ruff check . --fix    # 自动修复
poetry run ruff format .         # 代码格式化
```

**Ruff 配置规则：**
- `E/W`：pycodestyle 风格检查
- `F`：Pyflakes 逻辑检查
- `I`：isort 导入排序
- `N`：PEP8 命名规范
- `UP`：pyupgrade Python 升级建议
- `B`：flake8-bugbear 潜在 Bug
- `C4`：推导式优化
- `SIM`：代码简化建议
- 行长度：100
- 目标 Python 版本：3.11

### 2. 日志规范（structlog）

使用 **structlog** 进行结构化日志输出：

| 级别 | 使用场景 |
|------|----------|
| `DEBUG` | 请求参数、响应内容、批次 flush 细节 |
| `INFO` | 业务流程节点、任务开始/结束 |
| `WARNING` | 单条采集失败、数据源 fallback、限流触发 |
| `ERROR` | 批次整体失败（必须打印堆栈）|

**最佳实践：**
- 禁止 `print()`，统一使用 `structlog.get_logger(__name__)`
- 异常使用 `logger.exception("描述")` 自动包含堆栈
- 关键上下文使用 `logger.bind(key=value)` 绑定
- 开发环境：彩色文本输出
- 生产环境：`LOG_FORMAT=json` 启用 JSON 结构化输出

### 3. 数据采集通用规范

- **限流**：由 `AdaptiveRequestEngine` 自动调节，无需手动 sleep
- **并发**：单线程顺序执行，避免触发封禁
- **数据校验**：使用 dataclass 校验采集数据
- **关联**：股票通过 `tb_stock_basic.company_id` 直接关联公司
- **字段映射**：新增数据类型时，优先修改 YAML 配置而非硬编码

### 4. 适配器开发规范

新增数据源适配器需遵循 `DataSourceAdapter` 协议：

```python
class DataSourceAdapter(Protocol):
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        ...
```

- 返回原始 API 字段的字典（不做字段名转换，由 `FieldMapper` 处理）
- 空数据返回 `{}`，异常直接抛出（由 `SourceFallbackPipeline` 捕获并 fallback）
- 批量获取的适配器（如 Tushare `stock_basic`）应在内部缓存，避免重复 API 调用

### 5. 测试规范

- 测试文件命名：`test_{被测模块}.py`
- 测试类命名：`Test{被测类}`
- 测试方法命名：`test_should_{预期}_when_{条件}`
- 目录结构：
  - `tests/unit/`：单元测试（不依赖外部服务，mock 数据源）
  - `tests/integration/`：集成测试（依赖数据库/网络）
- 新增核心组件必须配套单元测试，目标覆盖率 ≥ 85%

## 配置项

采集器通过环境变量或 `.env` 文件配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST/PORT/NAME/USER/PASSWORD` | PostgreSQL 连接 | — |
| `TUSHARE_TOKEN` | Tushare API Token | — |
| `COLLECTION_TTL_HOURS` | stock 级状态有效期（小时）| 24 |
| `COLLECTION_BATCH_SIZE` | 批处理大小（兼作状态提交批次）| 20 |
| `ADAPTIVE_MIN_DELAY` | 最小调用间隔（秒）| 1.0 |
| `ADAPTIVE_MAX_DELAY` | 最大调用间隔（秒）| 60.0 |
| `ADAPTIVE_BACKOFF_JITTER` | 退避抖动范围（秒）| 0.5 |
| `ADAPTIVE_SUCCESS_THRESHOLD` | 连续成功多少次后尝试降速 | 10 |
| `RETRY_MAX_ATTEMPTS` | 单个请求最大重试次数 | 3 |
| `BATCH_FAIL_THRESHOLD` | 批次失败率阈值 | 0.1（10%）|
| `LOG_LEVEL` | 日志级别 | INFO |
| `LOG_FORMAT` | 日志格式（text/json）| text |

## 常用命令

```bash
# 安装依赖
poetry install

# CLI 模式：股票数据采集
poetry run python -m data_collector stock --full
poetry run python -m data_collector stock --code 000001

# CLI 模式：公司数据采集
poetry run python -m data_collector company --full
poetry run python -m data_collector company --code 000001

# CLI 模式：字段补充
poetry run python -m data_collector supplement --full

# 运行测试
poetry run pytest

# 代码检查
poetry run ruff check .

# 自动修复代码
poetry run ruff check . --fix

# 格式化代码
poetry run ruff format .
```
