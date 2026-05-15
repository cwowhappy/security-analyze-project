# Python Domain Models

> 33 nodes · cohesion 0.07

## Key Concepts

- **IndicatorCalculator** (7 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **indicator_calculator.py** (6 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **FinancialBalance** (6 connections) — `collector/src/data_collector/core/domain/financial_balance.py`
- **FinancialCashflow** (6 connections) — `collector/src/data_collector/core/domain/financial_cashflow.py`
- **FinancialIncome** (6 connections) — `collector/src/data_collector/core/domain/financial_income.py`
- **FinancialIndicator** (6 connections) — `collector/src/data_collector/core/domain/financial_indicator.py`
- **FinancialDataSnapshot** (6 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **.calculate()** (5 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **financial_balance.py** (3 connections) — `collector/src/data_collector/core/domain/financial_balance.py`
- **financial_cashflow.py** (3 connections) — `collector/src/data_collector/core/domain/financial_cashflow.py`
- **financial_income.py** (3 connections) — `collector/src/data_collector/core/domain/financial_income.py`
- **financial_indicator.py** (3 connections) — `collector/src/data_collector/core/domain/financial_indicator.py`
- **_calc_growth()** (2 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **_safe_avg()** (2 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **_safe_div()** (2 connections) — `collector/src/data_collector/services/indicator_calculator.py`
- **.__post_init__()** (1 connections) — `collector/src/data_collector/core/domain/financial_balance.py`
- **.to_dict()** (1 connections) — `collector/src/data_collector/core/domain/financial_balance.py`
- **from_dict()** (1 connections) — `collector/src/data_collector/core/domain/financial_balance.py`
- **资产负债表领域模型，与 tb_financial_balance 表结构对应。** (1 connections) — `collector/src/data_collector/core/domain/financial_balance.py`
- **.__post_init__()** (1 connections) — `collector/src/data_collector/core/domain/financial_cashflow.py`
- **.to_dict()** (1 connections) — `collector/src/data_collector/core/domain/financial_cashflow.py`
- **from_dict()** (1 connections) — `collector/src/data_collector/core/domain/financial_cashflow.py`
- **现金流量表领域模型，与 tb_financial_cashflow 表结构对应。** (1 connections) — `collector/src/data_collector/core/domain/financial_cashflow.py`
- **.__post_init__()** (1 connections) — `collector/src/data_collector/core/domain/financial_income.py`
- **.to_dict()** (1 connections) — `collector/src/data_collector/core/domain/financial_income.py`
- *... and 8 more nodes in this community*

## Relationships

- [[Frontend Router & Auth]] (2 shared connections)
- [[Python Repositories]] (1 shared connections)
- [[Community 60]] (1 shared connections)
- [[Community 66]] (1 shared connections)
- [[Community 67]] (1 shared connections)

## Source Files

- `collector/src/data_collector/core/domain/financial_balance.py`
- `collector/src/data_collector/core/domain/financial_cashflow.py`
- `collector/src/data_collector/core/domain/financial_income.py`
- `collector/src/data_collector/core/domain/financial_indicator.py`
- `collector/src/data_collector/services/indicator_calculator.py`

## Audit Trail

- EXTRACTED: 62 (74%)
- INFERRED: 22 (26%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*