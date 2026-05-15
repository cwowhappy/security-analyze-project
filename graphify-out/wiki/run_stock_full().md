# run_stock_full()

> God node · 17 connections · `collector/src/data_collector/scripts/stock_full.py`

**Community:** [[Financial Report Service]]

## Connections by Relation

### calls
- [[str]] `INFERRED`
- [[Settings]] `INFERRED`
- [[DbStockRepository]] `INFERRED`
- [[_to_ts_code()]] `EXTRACTED`
- [[.execute()]] `INFERRED`
- [[_to_exchange()]] `EXTRACTED`
- [[_clean_name()]] `EXTRACTED`
- [[_fetch_sz_stocks()]] `EXTRACTED`
- [[_fetch_bj_stocks()]] `EXTRACTED`
- [[_fetch_sh_stocks()]] `EXTRACTED`
- [[init_pool()]] `INFERRED`
- [[test_run_stock_full_success()]] `INFERRED`
- [[test_run_stock_full_empty_data()]] `INFERRED`
- [[test_run_stock_full_akshare_exception()]] `INFERRED`
- [[_infer_market()]] `EXTRACTED`

### contains
- [[stock_full.py]] `EXTRACTED`

### rationale_for
- [[执行股票全量采集（组合接口版）。      1. 通过 sh/sz/bj 分接口获取详细字段     2. 通过 stock_info_a_code_name]] `EXTRACTED`

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*