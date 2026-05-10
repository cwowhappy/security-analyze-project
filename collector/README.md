# 数据采集模块

基于 Poetry + Python 3.11 构建的股票数据采集模块。

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
├── api.py                  # FastAPI HTTP API
├── scheduler.py            # APScheduler 调度引擎
├── task_executor.py        # 任务执行器（按 task_type 路由）
├── core/                   # 核心业务层
│   ├── domain/             # 领域模型（Stock、Company、CollectionTask）
│   └── ports/              # 抽象接口（Repository、DataSource）
├── adapters/               # 适配器实现
│   ├── akshare_source.py   # akshare 数据源（主）
│   ├── tushare_source.py   # tushare 数据源（备）
│   ├── db_stock_repository.py
│   ├── db_company_repository.py
│   └── db_collection_task_repository.py
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
| `WARNING` | 请求重试、限流触发、数据源降级 |
| `ERROR` | 采集失败（必须打印堆栈）|

**最佳实践：**
- 禁止 `print()`，统一使用 `structlog.get_logger(__name__)`
- 异常使用 `logger.exception("描述")` 自动包含堆栈
- 关键上下文使用 `logger.bind(key=value)` 绑定
- 开发环境：彩色文本输出
- 生产环境：`LOG_FORMAT=json` 启用 JSON 结构化输出

### 3. 采集器架构（Phase 2）

#### 3.1 总体架构

```
┌─────────────────────────────────────────────────────────┐
│                    采集器进程 (Python)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │  HTTP API   │  │ APScheduler │  │   Event Listener │  │
│  │  (FastAPI)  │  │ Background  │  │   (状态记录)      │  │
│  │             │  │ Scheduler   │  │                  │  │
│  └──────┬──────┘  └──────┬──────┘  └─────────────────┘  │
│         │                │                                │
│         └────────────────┼────────────────┐               │
│                          ▼                ▼               │
│              ┌─────────────────┐  ┌──────────────┐       │
│              │  Task Executor  │  │ PostgreSQL   │       │
│              │  (任务路由)      │  │ (状态记录)    │       │
│              └────────┬────────┘  └──────────────┘       │
│                       │                                   │
│         ┌─────────────┼─────────────┐                    │
│         ▼             ▼             ▼                    │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ akshare    │ │ tushare    │ │ PostgreSQL │           │
│  │ 适配器      │ │ 适配器      │ │ 入库适配器  │           │
│  └────────────┘ └────────────┘ └────────────┘           │
└─────────────────────────────────────────────────────────┘
```

#### 3.2 数据源适配器

| 数据源 | 优先级 | 职责 |
|--------|--------|------|
| `akshare` | 1（主） | `fetch_stock_list()` 全量股票，`fetch_company_info()` 公司详情 |
| `tushare` | 2（备） | 同上，需配置 `TUSHARE_TOKEN`，受积分限制 |

所有数据源实现 `DataSource` 抽象基类，支持健康检查与自动降级。

#### 3.3 任务执行器（TaskExecutor）

按 `task_type` 路由到对应采集逻辑：

| task_type | 说明 |
|-----------|------|
| `stock_full` | 全量采集股票列表，调用 `fetch_stock_list()` |
| `company_full` | 遍历已有股票，逐条调用 `fetch_company_info()` |
| `stock_single` | 单条股票更新（`task_params.stock_code`）|
| `company_single` | 单条公司更新（`task_params.stock_code`）|

#### 3.4 APScheduler 调度

- 启动时从 `tb_collection_task_schedule` 加载启用的 Cron 规则
- 支持即时任务：`POST /tasks` → `add_job(DateTrigger)`
- Event Listener 自动记录任务状态到 `tb_collection_task`

#### 3.5 HTTP API（FastAPI）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/tasks` | 创建即时采集任务 |
| GET | `/tasks` | 查询任务历史 |
| GET | `/tasks/{id}` | 查询单条任务详情 |
| GET | `/health` | 健康检查（数据源 + DB）|

### 4. 数据采集通用规范

- **限流**：遵守数据源访问策略，配置合理请求间隔
- **重试**：网络错误时自动重试，指数退避策略
- **并发**：控制并发数量，避免触发封禁
- **数据校验**：使用 dataclass/Pydantic 校验采集数据

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

# 启动 API 服务
poetry run python -m data_collector api

# CLI 模式：股票数据采集
poetry run python -m data_collector stock --full
poetry run python -m data_collector stock --code 000001

# CLI 模式：公司数据采集
poetry run python -m data_collector company --full
poetry run python -m data_collector company --code 000001 --source akshare

# 运行测试
poetry run pytest

# 代码检查
poetry run ruff check .

# 自动修复代码
poetry run ruff check . --fix

# 格式化代码
poetry run ruff format .
```
