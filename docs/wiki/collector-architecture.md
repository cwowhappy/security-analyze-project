# 采集器架构设计

> 本文档描述 Python 采集器的内部架构，包括调度引擎、数据源适配、任务执行与状态记录。

---

## 一、总体架构

采集器作为独立进程运行，内部由三个核心组件组成：

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
│              │  (线程池)        │  │ (状态记录)    │       │
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

---

## 二、组件职责

### 2.1 HTTP API（FastAPI）

暴露 REST 接口供后端调用：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/tasks` | 创建即时采集任务，APScheduler 立即执行 |
| GET | `/tasks` | 查询任务执行历史（从 `tb_collection_task` 读取）|
| GET | `/tasks/{id}` | 查询单条任务详情 |
| GET | `/health` | 采集器健康检查（数据源连通性） |

### 2.2 APScheduler BackgroundScheduler

核心调度引擎，配置如下：

```python
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.executors.pool import ThreadPoolExecutor

executors = {
    'default': ThreadPoolExecutor(max_workers=5)
}
job_defaults = {
    'coalesce': True,           # 错过多次触发合并为一次
    'max_instances': 1,         # 同一任务不并发
    'misfire_grace_time': 3600  # 离线1小时内补偿执行
}

scheduler = BackgroundScheduler(
    executors=executors,
    job_defaults=job_defaults
)
```

**定时任务加载：**
1. 采集器启动时，从 `tb_collection_task_schedule` 读取 `is_enabled = true` 的规则
2. 每条规则映射为 `CronTrigger` Job
3. Job 的执行函数为 `execute_collection_task(task_type, task_params, data_source)`

**即时任务触发：**
1. 后端调用 `POST /tasks`
2. 采集器调用 `scheduler.add_job(..., trigger='date', run_date=datetime.now())`
3. APScheduler 立即调度执行

### 2.3 Task Executor（线程池）

根据 `task_type` 路由到对应的采集逻辑：

| task_type | 执行逻辑 |
|-----------|---------|
| `stock_full` | 调用 akshare `stock_info_a_code_name()` 获取全量列表，逐条 enrich 后入库 |
| `company_full` | 遍历 `tb_stock_basic`，逐条调用 `stock_profile_cninfo` 获取公司信息 |
| `stock_single` | 针对 `task_params.stock_code` 单条采集更新 |
| `company_single` | 针对 `task_params.stock_code` 调用公司详情接口 |

### 2.4 Event Listener（状态记录）

监听 APScheduler 事件，自动写入 `tb_collection_task`：

```python
from apscheduler.events import EVENT_JOB_SUBMITTED, EVENT_JOB_EXECUTED, EVENT_JOB_ERROR

def on_job_submitted(event):
    # 插入 tb_collection_task，status='running'
    pass

def on_job_executed(event):
    # 更新 tb_collection_task，status='success'，记录 success_count
    pass

def on_job_error(event):
    # 更新 tb_collection_task，status='failed'，记录 error_message
    pass

scheduler.add_listener(on_job_submitted, EVENT_JOB_SUBMITTED)
scheduler.add_listener(on_job_executed, EVENT_JOB_EXECUTED)
scheduler.add_listener(on_job_error, EVENT_JOB_ERROR)
```

---

## 三、数据源适配器

### 3.1 适配器接口

```python
from abc import ABC, abstractmethod

class DataSourceAdapter(ABC):
    @abstractmethod
    def fetch_stock_list(self) -> list[StockBasic]:
        """获取全量股票列表"""
        pass

    @abstractmethod
    def fetch_company_info(self, stock_code: str) -> CompanyBasic | None:
        """获取公司详情"""
        pass

    @abstractmethod
    def check_health(self) -> bool:
        """检查数据源可用性"""
        pass
```

### 3.2 AkshareAdapter

- `fetch_stock_list()`：组合 `stock_info_a_code_name()` + `stock_info_sh_name_code()` + `stock_info_sz_name_code()`
- `fetch_company_info()`：调用 `stock_profile_cninfo()` 按股票代码查询
- 主数据源，免费无限制

### 3.3 TushareAdapter

- `fetch_stock_list()`：调用 `pro.stock_basic()`
- `fetch_company_info()`：调用 `pro.stock_company()`
- 备用数据源，受积分限制

---

## 四、执行流程

### 4.1 定时任务执行流程

```
APScheduler CronTrigger 到期
    → 调用 execute_collection_task()
    → EventListener 插入 tb_collection_task (running)
    → 选择数据源适配器（akshare 优先）
    → 逐条采集
        → 成功：写入 PostgreSQL
        → 失败：降级到备用源 / 记录 fail_count
    → EventListener 更新 tb_collection_task (success/failed)
```

### 4.2 即时任务执行流程

```
后端 POST /tasks
    → 采集器 add_job(DateTrigger)
    → APScheduler 立即执行
    → 后续流程与定时任务相同
```

---

## 五、配置项

采集器通过环境变量或 `.env` 配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `TUSHARE_TOKEN` | Tushare API Token | — |
| `DB_HOST/PORT/NAME/USER/PASSWORD` | PostgreSQL 连接 | — |
| `COLLECTOR_MAX_WORKERS` | APScheduler 线程池大小 | 5 |
| `COLLECTOR_MISFIRE_GRACE_TIME` | 错过执行补偿时间（秒）| 3600 |
| `SOURCE_REQUEST_DELAY_MIN/MAX` | 请求间随机延迟（秒）| 1-3 |
| `SOURCE_MAX_RETRIES` | 单条失败重试次数 | 3 |
