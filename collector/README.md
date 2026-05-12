# 数据采集模块

基于 Poetry + Python 3.11 构建的股票数据采集模块（v2.0 简化架构）。

## 技术栈

- Python 3.11
- Poetry（依赖管理）
- Pydantic（数据验证）
- structlog（结构化日志）
- pytest（单元测试）
- Ruff（代码检查 + 格式化）

## 项目结构

```
src/data_collector/
├── config.py               # pydantic-settings 全局配置
├── cli.py                  # CLI 入口
├── task_executor.py        # 任务执行器（按 task_type 路由到脚本）
├── core/
│   └── domain/             # 领域模型（Stock、Company、CollectionTask）
├── adapters/               # 仓库实现
│   ├── db_stock_repository.py
│   ├── db_company_repository.py
│   └── db_collection_task_repository.py
├── scripts/                # 采集脚本（直接调用数据源 API）
│   ├── stock_full.py       # AKShare 股票全量采集
│   ├── company_full.py     # AKShare 公司信息全量采集
│   └── field_supplement.py # Tushare 字段补充
└── infrastructure/         # 基础设施层
    ├── db.py               # PostgreSQL 连接池
    └── logging/            # 日志配置
```

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
| `DEBUG` | 请求参数、响应内容 |
| `INFO` | 业务流程节点 |
| `WARNING` | 单条采集失败、限流触发 |
| `ERROR` | 批次整体失败（必须打印堆栈）|

**最佳实践：**
- 禁止 `print()`，统一使用 `structlog.get_logger(__name__)`
- 异常使用 `logger.exception("描述")` 自动包含堆栈
- 关键上下文使用 `logger.bind(key=value)` 绑定
- 开发环境：彩色文本输出
- 生产环境：`LOG_FORMAT=json` 启用 JSON 结构化输出

### 3. 采集器架构（v2.0）

#### 3.1 总体架构

```
┌─────────────────────────────────────────────────────────┐
│                    采集器进程 (Python)                    │
│  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │  CLI / 手动触发      │  │  Task Polling Loop      │  │
│  │  (poetry run ...)   │  │  (轮询 pending 任务)     │  │
│  └──────────┬──────────┘  └───────────┬─────────────┘  │
│             │                         │                │
│             └──────────┬──────────────┘                │
│                        ↓                               │
│              ┌─────────────────────┐                   │
│              │  Collection Scripts │                   │
│              │  stock_full.py      │  ← AKShare        │
│              │  company_full.py    │  ← AKShare        │
│              │  field_supplement.py│  ← Tushare        │
│              └──────────┬──────────┘                   │
│                         ↓                              │
│              ┌─────────────────────┐                   │
│              │  PostgreSQL         │                   │
│              │  (直接读写)          │                   │
│              └─────────────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

#### 3.2 采集脚本

| 脚本 | 数据源 | 功能 |
|------|--------|------|
| `scripts/stock_full.py` | AKShare | 全量股票列表采集，写入 `tb_stock_basic` |
| `scripts/company_full.py` | AKShare | 遍历股票列表，逐条采集公司详情，写入 `tb_company_basic`，更新 `tb_stock_basic.company_id` |
| `scripts/field_supplement.py` | Tushare | 补充缺失字段（area、ts_code、管理层、实控人等）|

脚本直接调用数据源 API，无抽象适配层。单条失败记录日志并继续下一条。

#### 3.3 任务执行器（TaskExecutor）

按 `task_type` 路由到对应采集脚本：

| task_type | 说明 |
|-----------|------|
| `stock_full` | 全量采集股票列表 |
| `company_full` | 遍历已有股票，逐条采集公司信息 |
| `field_supplement` | 字段补充采集 |
| `stock_single` | 单条股票更新（`task_params.stock_code`）|
| `company_single` | 单条公司更新（`task_params.stock_code`）|

### 4. 数据采集通用规范

- **限流**：遵守数据源访问策略，配置合理请求间隔
- **并发**：单线程顺序执行，避免触发封禁
- **数据校验**：使用 dataclass 校验采集数据
- **关联**：股票通过 `tb_stock_basic.company_id` 直接关联公司

### 5. 测试规范

- 测试文件命名：`test_{被测模块}.py`
- 测试类命名：`Test{被测类}`
- 测试方法命名：`test_should_{预期}_when_{条件}`
- 目录结构：
  - `tests/unit/`：单元测试（不依赖外部服务）
  - `tests/integration/`：集成测试（依赖数据库/网络）

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
