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
src/stock_collector/
├── core/                   # 核心业务层
│   ├── domain/             # 领域模型
│   ├── ports/              # 抽象接口（Repository、DataSource）
│   └── use_cases/          # 业务用例
├── adapters/               # 适配器实现
├── infrastructure/         # 基础设施层
│   ├── config/             # 配置管理
│   └── logging/            # 日志配置
└── interfaces/             # 接口层
    └── cli/                # 命令行入口
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

### 3. 多数据源采集规范（核心设计）

本模块的核心设计要求：**同一份数据支持多数据源采集，具备主动降级切换和主动切换能力**。

#### 3.1 架构设计

```
┌─────────────────────────────────────────────────────────┐
│              MultiSourceCollector                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Eastmoney   │  │    Sina      │  │   Tushare    │  │
│  │  priority=1  │  │  priority=2  │  │  priority=3  │  │
│  │   (主源)     │  │   (备用1)    │  │   (备用2)    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

#### 3.2 数据源接口（DataSource）

所有数据源必须实现 `DataSource` 抽象基类：

| 方法/属性 | 说明 |
|-----------|------|
| `name` | 数据源唯一标识（如 `eastmoney`、`sina`）|
| `priority` | 优先级，数值越小优先级越高 |
| `fetch(symbol)` | 执行数据采集 |
| `check_health()` | 返回健康状态（HEALTHY/DEGRADED/UNAVAILABLE）|
| `is_available()` | 判断是否可用 |

#### 3.3 降级切换策略

**自动降级（Failover）：**
- 按 `priority` 排序依次尝试各数据源
- 主源失败时自动降级到备用源
- 每次降级记录到 `fallback_history`
- 采集成功后在日志中标注降级路径

```python
collector = MultiSourceCollector([eastmoney, sina, tushare])
data = collector.collect("000001")  # 自动在主源和备用源间切换
```

**主动切换（Manual Switch）：**
- 支持运维人员手动切换数据源
- 适用于数据源质量评估、A/B 测试等场景

```python
collector.switch_to("sina")   # 主动切换到新浪
collector.reset()              # 重置回最高优先级主源
```

#### 3.4 健康检查与状态监控

- 每个数据源独立维护健康状态
- 采集前检查 `is_available()`，不可用直接跳过
- 提供 `health_report()` 输出完整健康报告

```python
report = collector.health_report()
# {
#   "current_source": "sina",
#   "fallback_history": ["sina"],
#   "sources": [
#     {"name": "eastmoney", "priority": 1, "health": {...}},
#     {"name": "sina", "priority": 2, "health": {...}}
#   ]
# }
```

#### 3.5 异常处理规范

| 异常类型 | 触发场景 | 处理方式 |
|----------|----------|----------|
| `SourceRateLimitError` | 触发限流 | 降级到下一数据源，记录 WARNING 日志 |
| `SourceUnavailableError` | 数据源不可用 | 跳过该源，尝试其他源 |
| `DataSourceError` | 其他采集错误 | 记录错误详情，降级或终止 |

#### 3.6 实现要求

- 所有数据源实现必须是无状态的（除配置外）
- 数据采集失败不得阻塞整体流程
- 降级切换必须记录完整上下文（from_source, to_source, symbol）
- 支持通过配置动态调整数据源优先级

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

# 运行采集模块
poetry run python -m stock_collector

# 运行测试
poetry run pytest

# 代码检查
poetry run ruff check .

# 自动修复代码
poetry run ruff check . --fix

# 格式化代码
poetry run ruff format .
```
