# Collector 模块重构方案

> 分支：`refactor/collector-structure-and-granularity`  
> 目标：代码结构更合理，任务执行粒度更丰富、统一。

---

## 一、现有结构诊断

### 1.1 当前文件树

```
collector/
├── main.py                          # 入口：405行，参数解析+任务编排+对象创建
├── collector/
│   ├── config.py                    # DBConfig / CollectorConfig
│   ├── db/
│   │   └── postgres.py              # PostgresDB 连接池
│   ├── sources/
│   │   └── akshare_source.py        # 唯一数据源，无抽象接口
│   ├── tasks/
│   │   ├── company_task.py          # 453行
│   │   ├── finance_task.py          # 728行（最复杂）
│   │   ├── quote_task.py            # 105行
│   │   ├── index_basic_task.py      # 112行
│   │   ├── index_history_task.py    # 304行
│   │   ├── etf_basic_task.py        # 101行
│   │   └── industry_classification_sync.py  # 118行
│   ├── models.py                    # Pydantic 实体
│   ├── monitor.py                   # 任务监控/Session/进度
│   ├── scheduler.py                 # 388行，调度+执行耦合
│   ├── utils.py                     # 工具函数
│   └── decorators.py                # 重试装饰器
├── tests/                           # 各任务单元测试
└── scripts/                         # 零散脚本
```

### 1.2 核心问题

| 问题 | 说明 | 影响 |
|------|------|------|
| **main.py 臃肿** | 参数解析、对象组装、执行编排全部混在一处 | 新增任务需改入口，违背开闭原则 |
| **Scheduler 职责过重** | 既管 APScheduler 定时，又内含每个任务的执行体 | 大量重复 add/run 方法，调度与执行耦合 |
| **Task 无统一抽象** | 7 个任务各自为政，无基类/接口 | 重复代码（monitor 日志、异常处理），新增任务成本高 |
| **数据源无接口** | 只有 `AkshareSource` 具体类 | 多数据源扩展只是注释，无法落地 |
| **执行粒度参差不齐** | 仅 Finance/IndexHistory 支持 Session 恢复、增量、并发 | 其他任务无法享受同等能力 |
| **Monitor 使用不一致** | IndexHistoryTask 直接写 SQL 到 progress 表，且字段语义滥用 | 维护困难，容易出错 |
| **硬编码映射表** | CompanyTask 内含 ~130 行申万行业编码字典 | 更新映射需改代码、重新部署 |
| **根目录脚本混乱** | batch_company_import.py、finance_range_task.py 等遗留文件 | 干扰理解，职责不清 |

### 1.3 当前执行粒度矩阵

| 任务 | 全量 | 单只/指定 | 年份/日期范围 | 增量 | Session 恢复 | 并发 |
|------|:--:|:--:|:--:|:--:|:--:|:--:|
| Company | ✓ | ✓(按名称) | ✗ | ✗ | ✗ | ✗ |
| Finance | ✓ | ✓(按代码) | ✓ | ✓ | ✓ | ✓ |
| Quote | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| IndexBasic | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| IndexHistory | ✓ | ✓(列表) | ✓ | ✓ | ✓ | ✓ |
| ETFBasic | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Industry | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |

**目标：所有任务统一支持「全量 / 指定范围 / 增量 / Session 恢复」四种粒度。**

---

## 二、重构目标

1. **引入统一抽象层**：`BaseDataSource` 接口 + `BaseTask` 抽象基类。
2. **解耦调度与执行**：`Scheduler` 只负责定时触发，`TaskRunner` 负责任务组装与执行。
3. **统一执行粒度**：所有任务通过基类支持 `run_full()` / `run_partial()` / `run_incremental()` / `resume_session()`。
4. **统一监控与进度**：所有 Session/进度/日志统一走 `Monitor`，消除直接 SQL。
5. **清理与归位**：废弃根目录零散脚本，将硬编码映射表外置。

---

## 三、新架构设计

### 3.1 目标文件树

```
collector/
├── main.py                          # 仅保留：参数解析 → 交给 TaskCLI
├── collector/
│   ├── __init__.py
│   ├── config.py                    # 不变
│   ├── cli.py                       # 【新增】命令行入口封装（原 main.py 逻辑迁移）
│   ├── runner.py                    # 【新增】TaskRunner：任务组装、执行、生命周期
│   ├── scheduler.py                 # 【精简】仅保留 APScheduler 包装，通过 callable 注册
│   ├── db/
│   │   └── postgres.py              # 不变
│   ├── sources/
│   │   ├── __init__.py
│   │   ├── base.py                  # 【新增】BaseDataSource 抽象接口
│   │   └── akshare_source.py        # 实现 BaseDataSource
│   ├── tasks/
│   │   ├── __init__.py              # 导出所有 Task
│   │   ├── base.py                  # 【新增】BaseTask 抽象基类 + 粒度支持
│   │   ├── company_task.py          # 继承 BaseTask
│   │   ├── finance_task.py          # 继承 BaseTask
│   │   ├── quote_task.py            # 继承 BaseTask
│   │   ├── index_basic_task.py      # 继承 BaseTask
│   │   ├── index_history_task.py    # 继承 BaseTask
│   │   ├── etf_basic_task.py        # 继承 BaseTask
│   │   └── industry_sync_task.py    # 继承 BaseTask（原 industry_classification_sync）
│   ├── models.py                    # 不变
│   ├── monitor.py                   # 增强：支持通用 task_key（替代 stock_code 字段滥用）
│   ├── utils.py                     # 不变
│   └── decorators.py                # 不变
├── data/                            # 【新增】外置数据文件
│   └── sw_industry_mapping.json     # 申万行业编码映射（替代硬编码字典）
├── tests/                           # 随结构迁移
└── scripts/                         # 仅保留真正独立的辅助脚本
```

### 3.2 分层依赖图

```
┌─────────────┐
│   main.py   │  ← 仅解析参数，调用 TaskCLI
│   cli.py    │
└──────┬──────┘
       │
┌──────▼──────┐
│  scheduler  │  ← 纯调度层：APScheduler 包装，注册 callable
└──────┬──────┘
       │
┌──────▼──────┐
│   runner    │  ← 编排层：TaskRunner 组装 db + source + monitor + task
└──────┬──────┘
       │
┌──────▼──────┐
│   tasks/    │  ← 业务层：BaseTask 子类，统一粒度接口
│   base.py   │
└──────┬──────┘
       │
┌──────▼──────┐     ┌─────────────┐
│   sources/  │ ←── │   models    │
│   base.py   │     │   monitor   │
└─────────────┘     └─────────────┘
```

**依赖规则**：上层可调用下层，下层不感知上层。`tasks` 只依赖 `sources/base`、`models`、`monitor`、`db`。

---

## 四、关键设计细节

### 4.1 BaseDataSource 接口（`collector/sources/base.py`）

```python
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
import pandas as pd


class BaseDataSource(ABC):
    """数据采集源抽象接口，所有具体数据源须实现此接口。"""

    @abstractmethod
    def get_stock_list(self) -> List[Dict[str, Any]]: ...

    @abstractmethod
    def get_company_detail(self, stock_code: str) -> Optional[Dict[str, Any]]: ...

    @abstractmethod
    def get_stock_daily_quote(self, stock_code: str, start_date: str, end_date: str) -> Optional[pd.DataFrame]: ...

    @abstractmethod
    def get_balance_sheet(self, symbol: str, start_year: Optional[int], end_year: Optional[int]) -> Optional[pd.DataFrame]: ...

    @abstractmethod
    def get_profit_sheet(self, symbol: str, start_year: Optional[int], end_year: Optional[int]) -> Optional[pd.DataFrame]: ...

    @abstractmethod
    def get_cash_flow_sheet(self, symbol: str, start_year: Optional[int], end_year: Optional[int]) -> Optional[pd.DataFrame]: ...

    @abstractmethod
    def get_index_list(self) -> List[Dict[str, Any]]: ...

    @abstractmethod
    def get_index_history(self, symbol: str, period: str, start_date: Optional[str], end_date: Optional[str]) -> Optional[pd.DataFrame]: ...

    @abstractmethod
    def get_etf_spot_list(self) -> List[Dict[str, Any]]: ...

    @abstractmethod
    def search_by_name(self, query: str) -> List[Dict[str, Any]]: ...
```

> `AkshareSource` 直接实现 `BaseDataSource`，新增数据源时只需再写一个实现类。

### 4.2 BaseTask 抽象基类（`collector/tasks/base.py`）

```python
from abc import ABC, abstractmethod
from typing import Any, Optional, Dict, List
import uuid
import logging

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor

logger = logging.getLogger(__name__)


class TaskResult:
    """统一任务执行结果"""
    def __init__(self, created: int = 0, updated: int = 0, failed: int = 0, rows: int = 0):
        self.created = created
        self.updated = updated
        self.failed = failed
        self.rows = rows

    def __repr__(self):
        return f"TaskResult(created={self.created}, updated={self.updated}, failed={self.failed}, rows={self.rows})"


class BaseTask(ABC):
    """
    采集任务抽象基类。

    所有具体任务必须实现：
      - task_name: 任务标识字符串
      - data_type: 数据类型标识（用于 monitor 数据快照）
      - run_full(): 全量采集
      - run_partial(identifiers): 指定范围采集
      - run_incremental(): 增量采集
    """

    task_name: str = ""
    data_type: str = ""

    def __init__(self, db: PostgresDB, source: BaseDataSource, monitor: Optional[Monitor] = None):
        self.db = db
        self.source = source
        self.monitor = monitor

    # ------------------------------------------------------------------
    # 公共入口（由 Runner 或 Scheduler 调用）
    # ------------------------------------------------------------------
    def execute(self, mode: str = "full", **kwargs) -> TaskResult:
        """
        统一执行入口。

        Args:
            mode: "full" | "partial" | "incremental" | "resume"
            **kwargs: 各模式所需的额外参数
        """
        session_id = kwargs.get("session_id")
        if session_id:
            mode = "resume"

        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start(self.task_name, self.data_type, session_id=session_id)

        result = TaskResult()
        try:
            if mode == "full":
                result = self.run_full(**kwargs)
            elif mode == "partial":
                result = self.run_partial(**kwargs)
            elif mode == "incremental":
                result = self.run_incremental(**kwargs)
            elif mode == "resume":
                result = self.resume_session(**kwargs)
            else:
                raise ValueError(f"Unknown execution mode: {mode}")

            if self.monitor:
                self.monitor.log_task_end(task_id, "success", result.rows)
                self.monitor.upsert_data_status(self.data_type, result.rows, task_id)
        except Exception as e:
            logger.error(f"Task {self.task_name} failed: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", error_message=str(e))
            raise

        return result

    # ------------------------------------------------------------------
    # 子类必须实现
    # ------------------------------------------------------------------
    @abstractmethod
    def run_full(self, **kwargs) -> TaskResult:
        """全量采集"""
        ...

    @abstractmethod
    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """指定范围采集（如指定股票代码列表、指数代码列表等）"""
        ...

    @abstractmethod
    def run_incremental(self, **kwargs) -> TaskResult:
        """增量采集"""
        ...

    def resume_session(self, session_id: str, **kwargs) -> TaskResult:
        """
        从 Session 断点恢复。默认实现基于 Monitor 的 progress 表跳过已成功的记录。
        子类可覆盖以支持更复杂的恢复逻辑。
        """
        success_set = self.monitor.get_session_progress(session_id) if self.monitor else set()
        kwargs["exclude_set"] = success_set
        kwargs["session_id"] = session_id
        return self.run_full(**kwargs)
```

**设计要点**：
- `execute(mode, **kwargs)` 统一所有任务的调用方式。
- 生命周期（task_start → 执行 → task_end → data_status）在基类中完成，子类只需关注业务逻辑。
- `resume_session` 提供默认实现：利用 `Monitor.get_session_progress()` 获取已成功集合，传给 `run_full` 跳过。
- 不支持某种粒度的子类可以抛出 `NotImplementedError`，或提供空实现。

### 4.3 TaskRunner（`collector/runner.py`）

```python
"""任务运行器：负责根据配置组装依赖（db + source + monitor），并执行任务。"""

from typing import Optional, Type

from collector.config import CollectorConfig
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor
from collector.tasks.base import BaseTask, TaskResult


class TaskRunner:
    def __init__(self, cfg: Optional[CollectorConfig] = None):
        self.cfg = cfg or CollectorConfig.from_env()
        self.db = self._create_db()
        self.source = self._create_source()
        self.monitor = Monitor(self.db)

    def _create_db(self) -> PostgresDB:
        db = self.cfg.db
        return PostgresDB(
            host=db.host, port=db.port, database=db.database,
            user=db.user, password=db.password,
            pool_min_size=self.cfg.db_pool_min_size,
            pool_max_size=self.cfg.db_pool_max_size,
        )

    def _create_source(self) -> AkshareSource:
        return AkshareSource(
            max_retries=self.cfg.source_max_retries,
            retry_delay=self.cfg.source_retry_delay,
            retry_backoff=self.cfg.source_retry_backoff,
        )

    def run(self, task_cls: Type[BaseTask], mode: str = "full", **kwargs) -> TaskResult:
        """实例化并执行指定任务类"""
        task = task_cls(db=self.db, source=self.source, monitor=self.monitor)
        return task.execute(mode=mode, **kwargs)

    def close(self):
        self.db.close()
```

**设计要点**：
- 所有任务的对象组装（db/source/monitor）集中到一处，消除 main.py 中的重复创建代码。
- `run(task_cls, mode, **kwargs)` 一行即可执行任意任务。

### 4.4 Scheduler 精简（`collector/scheduler.py`）

```python
"""精简后的调度器：只负责 APScheduler 的启动/停止/Job 注册，不内含任务执行体。"""

from typing import Optional, Callable
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

from collector.config import DBConfig


class Scheduler:
    def __init__(self, db_cfg: Optional[DBConfig] = None):
        self._scheduler = BackgroundScheduler()
        if db_cfg:
            # 可选：配置 SQLAlchemyJobStore
            ...

    def start(self): ...
    def stop(self): ...

    def register(self, job_id: str, name: str, cron: str, func: Callable, **kwargs) -> bool:
        """通用 Job 注册方法，替代原来每个任务独立的 add_x_job 方法。"""
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
            else:
                self._scheduler.add_job(func, trigger=trigger, id=job_id, name=name, replace_existing=True, kwargs=kwargs)
            return True
        except Exception as e:
            logger.error(f"Failed to register job {job_id}: {e}")
            return False
```

**设计要点**：
- 从 388 行压缩到 ~80 行。
- 用 `register(job_id, name, cron, func, **kwargs)` 一个方法替代 7 个 `add_x_job` + 7 个 `_run_x_task`。
- `func` 由外部传入（如 `lambda: runner.run(FinanceTask, mode="full")`），Scheduler 不再感知具体任务类型。

### 4.5 CLI 封装（`collector/cli.py`）

将原 `main.py` 的 405 行逻辑迁移到 `collector/cli.py`，`main.py` 只保留一行：

```python
# main.py
from collector.cli import main
if __name__ == "__main__":
    main()
```

`cli.py` 内部使用 `argparse` + `TaskRunner`，结构示例：

```python
def main():
    parser = argparse.ArgumentParser(...)
    # ... 所有参数定义 ...
    args = parser.parse_args()

    runner = TaskRunner()
    try:
        if args.run_company:
            runner.run(CompanyTask, mode="full")
        elif args.company:
            runner.run(CompanyTask, mode="partial", identifiers=[args.company])
        elif args.run_finance:
            runner.run(FinanceTask, mode="full", batch_size=args.finance_batch_size)
        elif args.finance:
            runner.run(FinanceTask, mode="partial", identifiers=[args.finance])
        elif args.finance_session_id:
            runner.run(FinanceTask, mode="resume", session_id=args.finance_session_id)
        # ... 其他任务同理 ...
        else:
            # 启动调度器模式
            scheduler = Scheduler()
            if args.scheduler_cron_company:
                scheduler.register("company", "Company Sync", args.scheduler_cron_company,
                                   lambda: runner.run(CompanyTask, mode="full"))
            # ...
            scheduler.start()
    finally:
        runner.close()
```

### 4.6 Monitor 增强

解决 `IndexHistoryTask` 直接写 SQL 和字段语义滥用问题：

```python
class Monitor:
    # 现有方法不变...

    def log_task_progress(
        self,
        session_id: str,
        task_key: str,        # 【改名】原来是 stock_code，实际是通用任务标识
        status: str,
        rows_created: int = 0,
        rows_updated: int = 0,
        error_message: Optional[str] = None,
    ):
        ...

    def get_session_progress(self, session_id: str) -> Set[str]:
        # 查询 task_key 字段（兼容已有数据）
        ...
```

> 数据库层面：`collector_task_progress.stock_code` 不改列名（避免迁移），但在代码中统一按 `task_key` 语义使用。

### 4.7 硬编码映射表外置

将 `CompanyTask` 中的 `L1_TO_801_MAPPING` 和 `L2_TO_801_MAPPING` 提取为 `collector/data/sw_industry_mapping.json`：

```json
{
  "L1": {"11": "801030", "22": "801030", ...},
  "L2": {"1101": "801038", "1102": "801015", ...}
}
```

`CompanyTask` 启动时懒加载该 JSON 文件。

---

## 五、各任务改造要点

### 5.1 CompanyTask

| 粒度 | 改造后支持 |
|------|-----------|
| full | 现有 `run()` 逻辑迁移到 `run_full()` |
| partial | 现有 `run_by_name()` 迁移到 `run_partial(identifiers)`，支持批量代码列表 |
| incremental | 新增：基于 `company.updated_at` 只采集近期变更 |
| resume | 继承基类默认实现即可 |

### 5.2 FinanceTask

| 粒度 | 改造后支持 |
|------|-----------|
| full | 现有 `run()` 迁移到 `run_full()`，保留 batch_size / 并发 |
| partial | 现有 `run_by_stock_code()` / `run_by_stock_code_and_years()` 合并到 `run_partial()` |
| incremental | 现有增量逻辑迁移到 `run_incremental()` |
| resume | 现有 Session 恢复逻辑迁移到 `resume_session()`，覆盖基类默认实现 |

### 5.3 QuoteTask

| 粒度 | 改造后支持 |
|------|-----------|
| full | 现有 `run()` 迁移到 `run_full()`（采集持仓股票） |
| partial | 新增：支持传入指定股票代码列表 |
| incremental | 新增：基于 `daily_quote` 最大日期只补最新数据 |
| resume | 继承基类默认实现 |

### 5.4 IndexBasicTask / ETFBasicTask

两者结构相似，直接继承 `BaseTask`：
- `run_full()`：全量 upsert
- `run_partial(identifiers)`：指定代码列表 upsert
- `run_incremental()`：基于 `updated_at` 或数据哈希判断变更（可选）
- `resume`：继承默认实现（通常全量任务很快，不需要 Session）

### 5.5 IndexHistoryTask

| 粒度 | 改造后支持 |
|------|-----------|
| full | 现有 `run()` 迁移到 `run_full()` |
| partial | 现有 `index_codes` 参数能力显式化到 `run_partial()` |
| incremental | 现有增量逻辑迁移到 `run_incremental()` |
| resume | 现有 Session 恢复逻辑迁移到 `resume_session()`，移除直接 SQL |

### 5.6 IndustryTask（原 IndustrySyncTask）

- 原 `industry_classification_sync.py` 中的函数改造为 `IndustryTask` 类，支持**分别同步申万（SW）和东方财富（EM）行业分类**。
- `run_full()`：同步申万 + 东财全部行业分类。
- `run_partial(identifiers)`：`identifiers` 传入 `["SW"]` 或 `["EM"]` 或 `["SW", "EM"]`，分别同步指定来源。
- `run_incremental()`：基于 `industry_category.updated_at` 或数据哈希，只同步变更项（可选，因数据量小可暂不实现）。

---

## 六、实施步骤

按以下顺序推进，每一步均可独立测试：

### Step 1：基础设施（无业务变更）
1. 创建 `collector/sources/base.py` — `BaseDataSource` 抽象接口。
2. 让 `AkshareSource` 实现 `BaseDataSource`。
3. 创建 `collector/tasks/base.py` — `BaseTask` + `TaskResult`。
4. 创建 `collector/runner.py` — `TaskRunner`。
5. 精简 `collector/scheduler.py` — 提取通用 `register()` 方法。

### Step 2：硬编码外置 + Monitor 增强
1. 提取申万映射到 `collector/data/sw_industry_mapping.json`。
2. `Monitor` 增加 `task_key` 语义（不改 DB 列名）。

### Step 3：逐个改造 Task（从简单到复杂）
1. `IndexBasicTask` → 继承 `BaseTask`
2. `ETFBasicTask` → 继承 `BaseTask`
3. `QuoteTask` → 继承 `BaseTask`，补充 partial / incremental
4. `IndustryTask` → 类化（支持 SW/EM 分别同步）
5. `CompanyTask` → 继承 `BaseTask`，补充 incremental / resume
6. `IndexHistoryTask` → 继承 `BaseTask`，移除直接 SQL
7. `FinanceTask` → 继承 `BaseTask`，保留并发与 Session 逻辑

### Step 4：入口迁移
1. 创建 `collector/cli.py`，将 main.py 逻辑迁移至此。
2. `main.py` 精简为入口桩。

### Step 5：清理与测试
1. 删除/归档根目录废弃脚本（`batch_company_import.py`、`finance_range_task.py` 等）。
2. 更新所有测试，确保通过。
3. 更新 `AGENTS.md` 中 collector 相关描述。

---

## 七、风险与回滚

| 风险 | 缓解措施 |
|------|---------|
| 重构期间引入 Bug | 每改造一个 Task 后运行对应测试；FinanceTask 最后改，因其最复杂 |
| CLI 参数变更影响现有使用习惯 | 保持 argparse 参数名完全不变，内部实现变更对用户透明 |
| Session 恢复逻辑改动导致已有 session 无法恢复 | `resume_session` 保持对 `collector_task_progress` 表的读取兼容 |
| 硬编码映射外置后加载失败 | JSON 文件随代码打包，加载失败时回退到内存空映射并告警 |

---

## 八、预期收益

1. **新增任务成本从 ~200 行降至 ~80 行**：只需实现 `run_full/partial/incremental` 三个方法。
2. **main.py 从 405 行降至 <20 行**：入口极简。
3. **Scheduler 从 388 行降至 ~80 行**：调度与执行彻底解耦。
4. **所有任务统一享受 4 种执行粒度**：不再出现"只有 Finance 支持恢复"的能力断层。
5. **数据源可替换**：新增数据源只需实现 `BaseDataSource`，无需改动 Task。
