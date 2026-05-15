# Community 66

> 9 nodes · cohesion 0.25

## Key Concepts

- **DbFinancialIncomeRepository** (9 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **.save()** (5 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **.save_all()** (4 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **.find_by_stock_code()** (2 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **.find_latest()** (2 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **基于 PostgreSQL 的利润表仓库实现。** (1 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **保存或更新利润表数据（Upsert 语义）。** (1 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **批量保存利润表，返回 (成功数, 失败数)。** (1 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`
- **db_financial_income_repository.py** (1 connections) — `collector/src/data_collector/adapters/db_financial_income_repository.py`

## Relationships

- [[Python Repositories]] (2 shared connections)
- [[Financial Report Service]] (2 shared connections)
- [[Python Domain Models]] (1 shared connections)
- [[Frontend Router & Auth]] (1 shared connections)
- [[Community 77]] (1 shared connections)
- [[Stock Repository Python]] (1 shared connections)

## Source Files

- `collector/src/data_collector/adapters/db_financial_income_repository.py`

## Audit Trail

- EXTRACTED: 18 (69%)
- INFERRED: 8 (31%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*