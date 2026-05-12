# 采集器架构设计 v2

> 本文档描述 Python 采集器的内部架构，采用极简设计，废弃 v1.0 中的 FastAPI、数据源适配器抽象与实时降级逻辑。

---

## 一、总体架构

采集器作为独立后台进程运行，内部由两个核心组件组成：

```
┌─────────────────────────────────────────────────────────┐
│                    采集器进程 (Python)                    │
│  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │  APScheduler        │  │  Task Polling Loop      │  │
│  │  BackgroundScheduler│  │  (轮询 pending 任务)     │  │
│  │  内部 Cron 配置      │  │                          │  │
│  └──────────┬──────────┘  └───────────┬─────────────┘  │
│             │                         │                │
│             └──────────┬──────────────┘                │
│                        ↓                               │
│              ┌─────────────────────┐                   │
│              │  Collection Scripts │                   │
│              │  stock_full.py      │  ← AKShare 全量   │
│              │  company_full.py    │  ← AKShare 全量   │
│              │  field_supplement.py│  ← Tushare 补充   │
│              └──────────┬──────────┘                   │
│                         ↓                              │
│              ┌─────────────────────┐                   │
│              │  PostgreSQL         │                   │
│              │  (直接读写)          │                   │
│              └─────────────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

---

## 二、组件职责

### 2.1 APScheduler BackgroundScheduler

核心调度引擎，Cron 规则从环境变量或配置文件读取（**不读取数据库**）。

```python
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.executors.pool import ThreadPoolExecutor

executors = {
    'default': ThreadPoolExecutor(max_workers=3)  # 降低并发，避免限流
}
job_defaults = {
    'coalesce': True,
    'max_instances': 1,
    'misfire_grace_time': 3600
}

scheduler = BackgroundScheduler(executors=executors, job_defaults=job_defaults)

# 定时任务从配置读取，硬编码示例：
scheduler.add_job(run_stock_full, 'cron', hour=2, minute=0)
scheduler.add_job(run_company_full, 'cron', day_of_week='sun', hour=3, minute=0)
scheduler.add_job(run_field_supplement, 'cron', day_of_week='mon', hour=4, minute=0)
```

### 2.2 Task Polling Loop（手动任务轮询）

采集器每 30 秒轮询 `tb_collection_task` 表中 `status = 'pending'` 的记录：

```python
import time
from datetime import datetime

def poll_pending_tasks():
    """每30秒执行一次"""
    tasks = db.query("""
        SELECT * FROM tb_collection_task
        WHERE status = 'pending'
        ORDER BY created_at ASC
    """)
    for task in tasks:
        execute_task(task)

# 作为独立线程运行
while True:
    poll_pending_tasks()
    time.sleep(30)
```

**执行流程：**
1. 读取 `pending` 任务
2. 更新 `status = 'running'`，`started_at = NOW()`
3. 根据 `task_type` 调用对应脚本函数
4. 脚本执行完毕后，更新 `status = 'success'/'failed'`，`completed_at = NOW()`，写入统计数字

### 2.3 采集脚本（顺序执行，无适配器抽象）

脚本直接调用 AKShare / Tushare API，无抽象层：

| 脚本 | 数据源 | 功能 |
|------|--------|------|
| `scripts/stock_full.py` | AKShare | 全量股票列表采集，写入 `tb_stock_basic` |
| `scripts/company_full.py` | AKShare | 遍历股票列表，逐条调用 `stock_profile_cninfo`，写入 `tb_company_basic`，并更新 `tb_stock_basic.company_id` |
| `scripts/field_supplement.py` | Tushare | 补充缺失字段（area、ts_code、管理层、实控人等） |

**单条失败处理：**
```python
def fetch_and_save_stock(stock_code):
    try:
        data = ak.stock_individual_info_em(symbol=stock_code)
        db.upsert('tb_stock_basic', data)
        return True
    except Exception as e:
        logger.warning(f"{stock_code} 采集失败: {e}")
        return False

# 批量执行
fail_count = 0
for code in stock_codes:
    if not fetch_and_save_stock(code):
        fail_count += 1
    time.sleep(random.uniform(1, 3))  # 随机延迟

# 失败率超过阈值则整体标记 failed
if fail_count / len(stock_codes) > 0.1:
    raise BatchFailThresholdExceeded()
```

---

## 三、执行流程

### 3.1 定时任务执行流程

```
APScheduler CronTrigger 到期
    → 调用 run_stock_full() / run_company_full() / run_field_supplement()
    → 插入 tb_collection_task (status='running')
    → 顺序执行采集逻辑
        → 单条成功：写入 PostgreSQL
        → 单条失败：记录 fail_count，继续下一条
    → 更新 tb_collection_task (status='success'/'failed')
```

### 3.2 手动任务执行流程

```
后端 POST /api/v1/collection/tasks
    → 插入 tb_collection_task (status='pending')
    → 采集器轮询到 pending 记录
    → 执行流程与定时任务相同
```

---

## 四、配置项

采集器通过环境变量配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `TUSHARE_TOKEN` | Tushare API Token | — |
| `DB_HOST/PORT/NAME/USER/PASSWORD` | PostgreSQL 连接 | — |
| `SOURCE_REQUEST_DELAY_MIN` | 请求间最小延迟（秒）| 1 |
| `SOURCE_REQUEST_DELAY_MAX` | 请求间最大延迟（秒）| 3 |
| `BATCH_FAIL_THRESHOLD` | 批次失败率阈值 | 0.1（10%）|

> 废弃 v1.0 配置：`COLLECTOR_MAX_WORKERS`（改为固定3）、`SOURCE_MAX_RETRIES`（改为单次尝试）、`SOURCE_RETRY_DELAY/BACKOFF`（去除重试逻辑）。

---

## 五、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-11 | 简化架构：去除 FastAPI、适配器抽象、实时降级；改为顺序脚本 + pending 轮询 |
| v1.0 | 2026-05-10 | 初始版本（已废弃） |
