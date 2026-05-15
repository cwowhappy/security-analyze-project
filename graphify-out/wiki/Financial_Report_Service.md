# Financial Report Service

> 20 nodes · cohesion 0.21

## Key Concepts

- **str** (50 connections)
- **run_stock_full()** (17 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **stock_full.py** (11 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **_clean_name()** (7 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **_fetch_bj_stocks()** (7 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **_fetch_sz_stocks()** (7 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **_fetch_sh_stocks()** (6 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **.save()** (5 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **_parse_date()** (5 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **.save_all()** (4 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **_parse_shares()** (4 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **_infer_market()** (3 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **保存或更新公司数据（Upsert 语义）。** (1 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **批量保存公司，返回 (成功数, 失败数)。** (1 connections) — `collector/src/data_collector/adapters/db_company_repository.py`
- **股票全量采集脚本。  组合使用 AKShare 的 stock_info_a_code_name（全量覆盖）与交易所分接口 （sh_name_code / sz** (1 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **获取上交所股票详情。      Returns:         {stock_code: {name, full_name, list_date, marke** (1 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **获取深交所股票详情。      Returns:         {stock_code: {name, list_date, industry, total_** (1 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **获取北交所股票详情。      Returns:         {stock_code: {name, list_date, industry, area,** (1 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **执行股票全量采集（组合接口版）。      1. 通过 sh/sz/bj 分接口获取详细字段     2. 通过 stock_info_a_code_name** (1 connections) — `collector/src/data_collector/scripts/stock_full.py`
- **清洗股票名称中的多余空格（如 '万  科Ａ' → '万科Ａ'）。** (1 connections) — `collector/src/data_collector/scripts/stock_full.py`

## Relationships

- [[Frontend Router & Auth]] (8 shared connections)
- [[Stock Repository Python]] (5 shared connections)
- [[Collector Config & Stock Domain]] (5 shared connections)
- [[Frontend Stock & Company Views]] (5 shared connections)
- [[Python Repositories]] (4 shared connections)
- [[Community 107]] (4 shared connections)
- [[Community 38]] (4 shared connections)
- [[Community 77]] (4 shared connections)
- [[Community 39]] (3 shared connections)
- [[Community 52]] (3 shared connections)
- [[Collection Task Domain]] (2 shared connections)
- [[Community 66]] (2 shared connections)

## Source Files

- `collector/src/data_collector/adapters/db_company_repository.py`
- `collector/src/data_collector/scripts/stock_full.py`

## Audit Trail

- EXTRACTED: 68 (51%)
- INFERRED: 66 (49%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*