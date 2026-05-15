# TaskExecutor

> God node · 23 connections · `collector/src/data_collector/task_executor.py`

**Community:** [[Community 33]]

## Connections by Relation

### calls
- [[_init_context()]] `INFERRED`
- [[main()]] `INFERRED`
- [[.test_execute_stock_single_missing_stock_code()]] `INFERRED`
- [[.test_execute_company_single_missing_stock_code()]] `INFERRED`
- [[.test_execute_sets_running_and_timestamps()]] `INFERRED`
- [[.setup_method()]] `INFERRED`
- [[.test_execute_unknown_task_type()]] `INFERRED`
- [[.test_execute_generates_id_if_missing()]] `INFERRED`

### contains
- [[task_executor.py]] `EXTRACTED`

### method
- [[.execute()]] `EXTRACTED`
- [[._execute_stock_single()]] `EXTRACTED`
- [[._execute_company_single()]] `EXTRACTED`
- [[.__init__()]] `EXTRACTED`

### rationale_for
- [[采集任务执行器。      负责根据 task_type 调用对应的采集脚本，记录执行状态。]] `EXTRACTED`

### uses
- [[Settings]] `INFERRED`
- [[DbStockRepository]] `INFERRED`
- [[TestTaskExecutor]] `INFERRED`
- [[DbCompanyRepository]] `INFERRED`
- [[Stock]] `INFERRED`
- [[TestTaskExecutorExceptions]] `INFERRED`
- [[CollectionTask]] `INFERRED`
- [[TestStockRepositoryIntegration]] `INFERRED`
- [[TaskStatus]] `INFERRED`

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*