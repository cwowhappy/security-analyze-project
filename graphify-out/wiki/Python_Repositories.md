# Python Repositories

> 23 nodes · cohesion 0.11

## Key Concepts

- **execute_query()** (19 connections) — `collector/src/data_collector/infrastructure/db.py`
- **DbCompanyRepository** (13 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **DbCollectionTaskRepository** (11 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **DbFinancialBalanceRepository** (9 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **.save()** (5 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **.save_all()** (4 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **.find_all()** (2 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **.find_by_id()** (2 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **.find_pending()** (2 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **.count()** (2 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **.find_all()** (2 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **.find_by_usc_code()** (2 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **.find_by_stock_code()** (2 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **.find_latest()** (2 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **db_financial_balance_repository.py** (2 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **基于 PostgreSQL 的采集任务仓库实现。** (1 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **基于 PostgreSQL 的公司仓库实现。** (1 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **PostgreSQL 资产负债表仓库实现。** (1 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **基于 PostgreSQL 的资产负债表仓库实现。** (1 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **保存或更新资产负债表数据（Upsert 语义）。** (1 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **批量保存资产负债表，返回 (成功数, 失败数)。** (1 connections) — `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- **db_collection_task_repository.py** (1 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **db_company_repository.py** (1 connections) — `collector/src/data_collector/adapters/db_company_repository.py`

## Relationships

- [[Stock Repository Python]] (5 shared connections)
- [[Financial Report Service]] (4 shared connections)
- [[Collector CLI & Entry Points]] (3 shared connections)
- [[Community 53]] (2 shared connections)
- [[Community 33]] (2 shared connections)
- [[Community 59]] (2 shared connections)
- [[Frontend Router & Auth]] (2 shared connections)
- [[Community 66]] (2 shared connections)
- [[Community 67]] (2 shared connections)
- [[Community 60]] (2 shared connections)
- [[Company Domain & Repository]] (1 shared connections)
- [[Community 51]] (1 shared connections)

## Source Files

- `collector/src/data_collector/adapters/db_collection_task_repository.py`
- `collector/src/data_collector/adapters/db_company_repository.py`
- `collector/src/data_collector/adapters/db_financial_balance_repository.py`
- `collector/src/data_collector/infrastructure/db.py`

## Audit Trail

- EXTRACTED: 46 (53%)
- INFERRED: 41 (47%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*