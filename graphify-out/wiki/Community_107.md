# Community 107

> 6 nodes · cohesion 0.53

## Key Concepts

- **run_financial_balance()** (8 connections) — `collector/src/data_collector/scripts/financial_balance.py`
- **financial_balance.py** (5 connections) — `collector/src/data_collector/scripts/financial_balance.py`
- **fetch_balance_for_stock()** (5 connections) — `collector/src/data_collector/scripts/financial_balance.py`
- **_parse_date()** (3 connections) — `collector/src/data_collector/scripts/financial_balance.py`
- **_to_decimal()** (3 connections) — `collector/src/data_collector/scripts/financial_balance.py`
- **资产负债表采集脚本。  使用 AKShare 的 stock_financial_report_sina 接口获取资产负债表数据。** (1 connections) — `collector/src/data_collector/scripts/financial_balance.py`

## Relationships

- [[Financial Report Service]] (4 shared connections)
- [[Frontend Router & Auth]] (2 shared connections)
- [[Collector Config & Stock Domain]] (1 shared connections)
- [[Python Repositories]] (1 shared connections)
- [[Stock Repository Python]] (1 shared connections)

## Source Files

- `collector/src/data_collector/scripts/financial_balance.py`

## Audit Trail

- EXTRACTED: 16 (64%)
- INFERRED: 9 (36%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*