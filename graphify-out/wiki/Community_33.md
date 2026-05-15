# Community 33

> 17 nodes · cohesion 0.16

## Key Concepts

- **TaskExecutor** (23 connections) — `collector/src/data_collector/task_executor.py`
- **CollectionTask** (9 connections) — `collector/src/data_collector/core/domain/collection_task.py`
- **TestTaskExecutorExceptions** (9 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **.test_execute_company_single_missing_stock_code()** (3 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **.test_execute_sets_running_and_timestamps()** (3 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **.test_execute_stock_single_missing_stock_code()** (3 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **task_executor.py** (2 connections) — `collector/src/data_collector/task_executor.py`
- **.test_execute_generates_id_if_missing()** (2 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **.test_execute_unknown_task_type()** (2 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **test_task_executor_exceptions.py** (1 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **任务执行器：根据 task_type 路由到对应的采集脚本。** (1 connections) — `collector/src/data_collector/task_executor.py`
- **采集任务执行器。      负责根据 task_type 调用对应的采集脚本，记录执行状态。** (1 connections) — `collector/src/data_collector/task_executor.py`
- **.__post_init__()** (1 connections) — `collector/src/data_collector/core/domain/collection_task.py`
- **.to_dict()** (1 connections) — `collector/src/data_collector/core/domain/collection_task.py`
- **stock_single 缺少 stock_code 应抛出 ValueError。** (1 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **company_single 缺少 stock_code 应抛出 ValueError。** (1 connections) — `collector/tests/unit/test_task_executor_exceptions.py`
- **执行前应设置状态为 RUNNING 并记录 started_at。** (1 connections) — `collector/tests/unit/test_task_executor_exceptions.py`

## Relationships

- [[Collection Task Domain]] (4 shared connections)
- [[Community 47]] (3 shared connections)
- [[Collector Config & Stock Domain]] (3 shared connections)
- [[Frontend Router & Auth]] (3 shared connections)
- [[Collector CLI & Entry Points]] (2 shared connections)
- [[Python Repositories]] (2 shared connections)
- [[Community 88]] (1 shared connections)
- [[Stock Repository Python]] (1 shared connections)
- [[Community 53]] (1 shared connections)

## Source Files

- `collector/src/data_collector/core/domain/collection_task.py`
- `collector/src/data_collector/task_executor.py`
- `collector/tests/unit/test_task_executor_exceptions.py`

## Audit Trail

- EXTRACTED: 33 (52%)
- INFERRED: 31 (48%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*