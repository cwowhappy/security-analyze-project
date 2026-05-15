# Community 77

> 8 nodes · cohesion 0.36

## Key Concepts

- **run_financial_income()** (9 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **fetch_income_for_stock()** (6 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **financial_income.py** (5 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **_parse_date()** (3 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **_to_decimal()** (3 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **利润表采集脚本。  使用 AKShare 的 stock_financial_report_sina 接口获取利润表数据。** (1 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **执行利润表采集。      Args:         stock_code: 单只股票代码，None 则采集全市场         settings: 配置** (1 connections) — `collector/src/data_collector/scripts/financial_income.py`
- **获取单只股票的利润表数据。      Args:         stock_code: 股票代码，如 "000001"      Returns:** (1 connections) — `collector/src/data_collector/scripts/financial_income.py`

## Relationships

- [[Financial Report Service]] (4 shared connections)
- [[Frontend Router & Auth]] (2 shared connections)
- [[Collector Config & Stock Domain]] (1 shared connections)
- [[Community 66]] (1 shared connections)
- [[Stock Repository Python]] (1 shared connections)

## Source Files

- `collector/src/data_collector/scripts/financial_income.py`

## Audit Trail

- EXTRACTED: 20 (69%)
- INFERRED: 9 (31%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*