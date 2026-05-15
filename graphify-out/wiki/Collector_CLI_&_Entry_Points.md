# Collector CLI & Entry Points

> 58 nodes · cohesion 0.06

## Key Concepts

- **close_pool()** (12 connections) — `collector/src/data_collector/infrastructure/db.py`
- **cli.py** (9 connections) — `collector/src/data_collector/cli.py`
- **db.py** (8 connections) — `collector/src/data_collector/infrastructure/db.py`
- **build_parser()** (8 connections) — `collector/src/data_collector/cli.py`
- **_init_context()** (8 connections) — `collector/src/data_collector/cli.py`
- **get_cursor()** (7 connections) — `collector/src/data_collector/infrastructure/db.py`
- **configure_logging()** (7 connections) — `collector/src/data_collector/infrastructure/logging/config.py`
- **main()** (6 connections) — `collector/run_financial_task.py`
- **_run_data_type()** (6 connections) — `collector/src/data_collector/cli.py`
- **init_pool()** (6 connections) — `collector/src/data_collector/infrastructure/db.py`
- **TestBuildParser** (6 connections) — `collector/tests/unit/test_cli.py`
- **test_db_exceptions.py** (5 connections) — `collector/tests/unit/test_db_exceptions.py`
- **_run_financial()** (5 connections) — `collector/src/data_collector/cli.py`
- **get_connection()** (5 connections) — `collector/src/data_collector/infrastructure/db.py`
- **TestMain** (5 connections) — `collector/tests/unit/test_cli.py`
- **TestGetConnection** (5 connections) — `collector/tests/unit/test_db_exceptions.py`
- **TestGetCursor** (5 connections) — `collector/tests/unit/test_db_exceptions.py`
- **main()** (4 connections) — `collector/src/data_collector/cli.py`
- **get_pool()** (4 connections) — `collector/src/data_collector/infrastructure/db.py`
- **.test_get_cursor_with_custom_cursor_factory()** (4 connections) — `collector/tests/unit/test_db_exceptions.py`
- **.test_get_pool_without_init_raises()** (4 connections) — `collector/tests/unit/test_db_exceptions.py`
- **.test_init_pool_returns_existing_pool()** (4 connections) — `collector/tests/unit/test_db_exceptions.py`
- **TestLoggingConfig** (4 connections) — `collector/tests/unit/test_logging_config.py`
- **_build_task()** (3 connections) — `collector/src/data_collector/cli.py`
- **_execute_and_report()** (3 connections) — `collector/src/data_collector/cli.py`
- *... and 33 more nodes in this community*

## Relationships

- [[Collector Config & Stock Domain]] (7 shared connections)
- [[Python Repositories]] (3 shared connections)
- [[Community 33]] (2 shared connections)
- [[Stock Repository Python]] (2 shared connections)
- [[Community 151]] (1 shared connections)
- [[Frontend Stock & Company Views]] (1 shared connections)
- [[Financial Report Service]] (1 shared connections)

## Source Files

- `collector/run_financial_task.py`
- `collector/src/data_collector/cli.py`
- `collector/src/data_collector/infrastructure/db.py`
- `collector/src/data_collector/infrastructure/logging/config.py`
- `collector/tests/unit/test_cli.py`
- `collector/tests/unit/test_db_exceptions.py`
- `collector/tests/unit/test_logging_config.py`

## Audit Trail

- EXTRACTED: 128 (64%)
- INFERRED: 73 (36%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*