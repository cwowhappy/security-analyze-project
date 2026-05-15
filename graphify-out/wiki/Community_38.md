# Community 38

> 15 nodes · cohesion 0.21

## Key Concepts

- **_parse_capital()** (9 connections) — `collector/src/data_collector/scripts/company_full.py`
- **fetch_company_for_stock()** (8 connections) — `collector/src/data_collector/scripts/company_full.py`
- **TestParseCapital** (8 connections) — `collector/tests/unit/test_scripts.py`
- **company_full.py** (6 connections) — `collector/src/data_collector/scripts/company_full.py`
- **_parse_province_city()** (4 connections) — `collector/src/data_collector/scripts/company_full.py`
- **_parse_date()** (3 connections) — `collector/src/data_collector/scripts/company_full.py`
- **.test_parse_capital_empty()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_capital_invalid()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_capital_none()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_capital_with_comma()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_capital_with_wan()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **.test_parse_capital_with_yi()** (2 connections) — `collector/tests/unit/test_scripts.py`
- **公司信息全量采集脚本。  遍历 tb_stock_basic 中所有股票，逐条调用 akshare.stock_profile_cninfo 获取公司详情，写入** (1 connections) — `collector/src/data_collector/scripts/company_full.py`
- **从注册地址解析省份和城市。      中国地址通常以"省/市/自治区"开头， followed by 城市名。** (1 connections) — `collector/src/data_collector/scripts/company_full.py`
- **为单只股票获取公司详情。      Returns:         Company 实例，未找到或失败时返回 None。** (1 connections) — `collector/src/data_collector/scripts/company_full.py`

## Relationships

- [[Financial Report Service]] (4 shared connections)
- [[Collector Config & Stock Domain]] (3 shared connections)
- [[Frontend Router & Auth]] (1 shared connections)
- [[Frontend Stock & Company Views]] (1 shared connections)

## Source Files

- `collector/src/data_collector/scripts/company_full.py`
- `collector/tests/unit/test_scripts.py`

## Audit Trail

- EXTRACTED: 35 (66%)
- INFERRED: 18 (34%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*