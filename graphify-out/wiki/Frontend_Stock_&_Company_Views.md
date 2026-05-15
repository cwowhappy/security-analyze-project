# Frontend Stock & Company Views

> 21 nodes · cohesion 0.14

## Key Concepts

- **_parse_int()** (9 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **run_field_supplement()** (9 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **field_supplement.py** (7 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **_supplement_stocks()** (7 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **test_scripts.py** (6 connections) — `collector/tests/unit/test_scripts.py`
- **_supplement_companies()** (6 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **TestParseInt** (6 connections) — `collector/tests/unit/test_scripts.py`
- **_init_tushare()** (4 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **_parse_date()** (4 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **TestInitTushare** (3 connections) — `collector/tests/unit/test_scripts.py`
- **.test_init_tushare_no_token()** (3 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_int_float_string()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_int_invalid()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_int_none()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_int_valid()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **字段补充采集脚本。  使用 Tushare 接口补充 tb_stock_basic 和 tb_company_basic 中缺失的字段， 如 area、ts_c** (1 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **补充公司字段。      使用 tushare.stock_company 接口补充 employees、chairman、manager、secretary** (1 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **执行字段补充采集。      Returns:         {"stock_total": int, "stock_success": int, "stoc** (1 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **解析日期字符串，支持 YYYY-MM-DD 和 YYYYMMDD 格式。** (1 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **解析整数（处理 Tushare 返回的浮点数字符串）。** (1 connections) — `collector/src/data_collector/scripts/field_supplement.py`
- **补充股票字段。      使用 tushare.stock_basic 和 daily_basic 接口批量补充 industry、area、     list** (1 connections) — `collector/src/data_collector/scripts/field_supplement.py`

## Relationships

- [[Financial Report Service]] (5 shared connections)
- [[Collector Config & Stock Domain]] (4 shared connections)
- [[Stock Repository Python]] (3 shared connections)
- [[Community 54]] (1 shared connections)
- [[Community 38]] (1 shared connections)
- [[Community 39]] (1 shared connections)
- [[Community 52]] (1 shared connections)
- [[Frontend Router & Auth]] (1 shared connections)
- [[Collector CLI & Entry Points]] (1 shared connections)

## Source Files

- `collector/src/data_collector/scripts/field_supplement.py`
- `collector/tests/unit/test_scripts.py`

## Audit Trail

- EXTRACTED: 54 (69%)
- INFERRED: 24 (31%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*