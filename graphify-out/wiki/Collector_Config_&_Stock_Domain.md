# Collector Config & Stock Domain

> 22 nodes · cohesion 0.14

## Key Concepts

- **Settings** (44 connections) — `collector/src/data_collector/config.py`
- **Stock** (10 connections) — `collector/src/data_collector/core/domain/stock.py`
- **run_company_full()** (10 connections) — `collector/src/data_collector/scripts/company_full.py`
- **test_scripts_run.py** (8 connections) — `collector/tests/unit/test_scripts_run.py`
- **TestSettings** (5 connections) — `collector/tests/unit/test_config.py`
- **TestRunCompanyFull** (4 connections) — `collector/tests/unit/test_scripts_run.py`
- **TestRunStockFull** (4 connections) — `collector/tests/unit/test_scripts_run.py`
- **test_run_company_full_save_success()** (3 connections) — `collector/tests/unit/test_scripts_run.py`
- **test_run_company_full_with_skipped()** (3 connections) — `collector/tests/unit/test_scripts_run.py`
- **test_run_stock_full_akshare_exception()** (3 connections) — `collector/tests/unit/test_scripts_run.py`
- **test_run_stock_full_empty_data()** (3 connections) — `collector/tests/unit/test_scripts_run.py`
- **test_run_stock_full_success()** (3 connections) — `collector/tests/unit/test_scripts_run.py`
- **.__init__()** (2 connections) — `collector/src/data_collector/task_executor.py`
- **.test_should_generate_database_url()** (2 connections) — `collector/tests/unit/test_config.py`
- **.test_should_have_default_values()** (2 connections) — `collector/tests/unit/test_config.py`
- **.test_should_override_from_env()** (2 connections) — `collector/tests/unit/test_config.py`
- **BaseSettings** (1 connections)
- **test_config.py** (1 connections) — `collector/tests/unit/test_config.py`
- **.__post_init__()** (1 connections) — `collector/src/data_collector/core/domain/stock.py`
- **.to_dict()** (1 connections) — `collector/src/data_collector/core/domain/stock.py`
- **执行公司信息全量采集。      遍历股票列表，逐条采集公司信息并建立关联。      Returns:         {"total": int, "suc** (1 connections) — `collector/src/data_collector/scripts/company_full.py`
- **采集脚本 run_* 函数集成风格单元测试。  使用 unittest.mock 替换 akshare / tushare 调用与数据库仓储。** (1 connections) — `collector/tests/unit/test_scripts_run.py`

## Relationships

- [[Collector CLI & Entry Points]] (7 shared connections)
- [[Financial Report Service]] (5 shared connections)
- [[Community 58]] (4 shared connections)
- [[Frontend Stock & Company Views]] (4 shared connections)
- [[Frontend Router & Auth]] (4 shared connections)
- [[Community 38]] (3 shared connections)
- [[Community 33]] (3 shared connections)
- [[Stock Repository Python]] (3 shared connections)
- [[Community 47]] (2 shared connections)
- [[Community 151]] (2 shared connections)
- [[Community 51]] (2 shared connections)
- [[Community 54]] (1 shared connections)

## Source Files

- `collector/src/data_collector/config.py`
- `collector/src/data_collector/core/domain/stock.py`
- `collector/src/data_collector/scripts/company_full.py`
- `collector/src/data_collector/task_executor.py`
- `collector/tests/unit/test_config.py`
- `collector/tests/unit/test_scripts_run.py`

## Audit Trail

- EXTRACTED: 38 (33%)
- INFERRED: 76 (67%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*