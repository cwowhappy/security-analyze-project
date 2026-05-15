# DbStockRepository

> God node · 23 connections · `collector/src/data_collector/adapters/db_stock_repository.py`

**Community:** [[Stock Repository Python]]

## Connections by Relation

### calls
- [[run_stock_full()]] `INFERRED`
- [[run_financial_indicator()]] `INFERRED`
- [[run_company_full()]] `INFERRED`
- [[run_field_supplement()]] `INFERRED`
- [[run_financial_income()]] `INFERRED`
- [[run_financial_balance()]] `INFERRED`
- [[run_financial_cashflow()]] `INFERRED`
- [[_supplement_stocks()]] `INFERRED`
- [[._execute_stock_single()]] `INFERRED`
- [[._execute_company_single()]] `INFERRED`
- [[.setup_method()]] `INFERRED`

### contains
- [[db_stock_repository.py]] `EXTRACTED`

### method
- [[.save()]] `EXTRACTED`
- [[.save_all()]] `EXTRACTED`
- [[.find_by_symbol()]] `EXTRACTED`
- [[.find_all()]] `EXTRACTED`
- [[.count()]] `EXTRACTED`
- [[.update_company_id()]] `EXTRACTED`

### rationale_for
- [[基于 PostgreSQL 的股票仓库实现。]] `EXTRACTED`

### uses
- [[TaskExecutor]] `INFERRED`
- [[TestDbStockRepository]] `INFERRED`
- [[Stock]] `INFERRED`
- [[TestStockRepositoryIntegration]] `INFERRED`

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*