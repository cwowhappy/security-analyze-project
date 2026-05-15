# Frontend Router & Auth

> 18 nodes · cohesion 0.14

## Key Concepts

- **.execute()** (14 connections) — `collector/src/data_collector/task_executor.py`
- **run_financial_indicator()** (13 connections) — `collector/src/data_collector/scripts/financial_indicator.py`
- **run_financial_cashflow()** (8 connections) — `collector/src/data_collector/scripts/financial_cashflow.py`
- **run_financial_full()** (8 connections) — `collector/src/data_collector/scripts/financial_full.py`
- **._execute_stock_single()** (6 connections) — `collector/src/data_collector/task_executor.py`
- **financial_cashflow.py** (5 connections) — `collector/src/data_collector/scripts/financial_cashflow.py`
- **._execute_company_single()** (5 connections) — `collector/src/data_collector/task_executor.py`
- **fetch_cashflow_for_stock()** (5 connections) — `collector/src/data_collector/scripts/financial_cashflow.py`
- **_parse_date()** (3 connections) — `collector/src/data_collector/scripts/financial_cashflow.py`
- **_to_decimal()** (3 connections) — `collector/src/data_collector/scripts/financial_cashflow.py`
- **financial_full.py** (2 connections) — `collector/src/data_collector/scripts/financial_full.py`
- **financial_indicator.py** (2 connections) — `collector/src/data_collector/scripts/financial_indicator.py`
- **执行采集任务并更新状态。          Args:             task: 待执行的任务。          Returns:** (1 connections) — `collector/src/data_collector/task_executor.py`
- **现金流量表采集脚本。  使用 AKShare 的 stock_financial_report_sina 接口获取现金流量表数据。** (1 connections) — `collector/src/data_collector/scripts/financial_cashflow.py`
- **财务三表批量采集编排脚本。  依次执行利润表、资产负债表、现金流量表采集，最后计算指标。** (1 connections) — `collector/src/data_collector/scripts/financial_full.py`
- **执行财务三表全量采集与指标计算。      Args:         stock_code: 单只股票代码，None 则采集全市场         setti** (1 connections) — `collector/src/data_collector/scripts/financial_full.py`
- **财务指标计算与入库脚本。  读取 tb_financial_income / balance / cashflow，计算指标后写入 tb_financial_i** (1 connections) — `collector/src/data_collector/scripts/financial_indicator.py`
- **执行财务指标计算与入库。      Args:         stock_code: 单只股票代码，None 则计算全市场         settings:** (1 connections) — `collector/src/data_collector/scripts/financial_indicator.py`

## Relationships

- [[Financial Report Service]] (8 shared connections)
- [[Collector Config & Stock Domain]] (4 shared connections)
- [[Stock Repository Python]] (4 shared connections)
- [[Community 33]] (3 shared connections)
- [[Community 77]] (2 shared connections)
- [[Community 107]] (2 shared connections)
- [[Python Repositories]] (2 shared connections)
- [[Community 60]] (2 shared connections)
- [[Python Domain Models]] (2 shared connections)
- [[Community 42]] (1 shared connections)
- [[Frontend Stock & Company Views]] (1 shared connections)
- [[Community 38]] (1 shared connections)

## Source Files

- `collector/src/data_collector/scripts/financial_cashflow.py`
- `collector/src/data_collector/scripts/financial_full.py`
- `collector/src/data_collector/scripts/financial_indicator.py`
- `collector/src/data_collector/task_executor.py`

## Audit Trail

- EXTRACTED: 37 (46%)
- INFERRED: 43 (54%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*