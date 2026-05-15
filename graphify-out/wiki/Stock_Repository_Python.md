# Stock Repository Python

> 24 nodes · cohesion 0.09

## Key Concepts

- **DbStockRepository** (23 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **execute_update()** (12 connections) — `collector/src/data_collector/infrastructure/db.py`
- **TestDbStockRepository** (12 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.save()** (5 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **.save_all()** (4 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **.save()** (3 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **.count()** (2 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **.find_all()** (2 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **.find_by_symbol()** (2 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **.update_company_id()** (2 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **.setup_method()** (2 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **基于 PostgreSQL 的股票仓库实现。** (1 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **保存或更新股票数据（Upsert 语义）。** (1 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **批量保存股票，返回 (成功数, 失败数)。** (1 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **db_stock_repository.py** (1 connections) — `collector/src/data_collector/adapters/db_stock_repository.py`
- **test_db_stock_repository.py** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **DbStockRepository 测试。** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_count()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_find_all()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_find_by_symbol()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_handle_save_all_failure()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_return_none_when_not_found()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_save_all_stocks()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`
- **.test_should_save_stock()** (1 connections) — `collector/tests/unit/test_db_stock_repository.py`

## Relationships

- [[Financial Report Service]] (5 shared connections)
- [[Python Repositories]] (5 shared connections)
- [[Frontend Router & Auth]] (4 shared connections)
- [[Collector Config & Stock Domain]] (3 shared connections)
- [[Frontend Stock & Company Views]] (3 shared connections)
- [[Collector CLI & Entry Points]] (2 shared connections)
- [[Community 88]] (1 shared connections)
- [[Community 33]] (1 shared connections)
- [[Community 107]] (1 shared connections)
- [[Community 77]] (1 shared connections)
- [[Community 66]] (1 shared connections)
- [[Company Domain & Repository]] (1 shared connections)

## Source Files

- `collector/src/data_collector/adapters/db_collection_task_repository.py`
- `collector/src/data_collector/adapters/db_stock_repository.py`
- `collector/src/data_collector/infrastructure/db.py`
- `collector/tests/unit/test_db_stock_repository.py`

## Audit Trail

- EXTRACTED: 45 (55%)
- INFERRED: 37 (45%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*