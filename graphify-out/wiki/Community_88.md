# Community 88

> 7 nodes · cohesion 0.29

## Key Concepts

- **TestStockRepositoryIntegration** (6 connections) — `collector/tests/integration/test_collection_flow.py`
- **test_collection_flow.py** (3 connections) — `collector/tests/integration/test_collection_flow.py`
- **temp_sqlite_db()** (2 connections) — `collector/tests/integration/test_collection_flow.py`
- **采集模块集成测试。  验证 task_executor → domain → db 的完整链路。 使用内存数据库（SQLite）替代 PostgreSQL，便于** (1 connections) — `collector/tests/integration/test_collection_flow.py`
- **创建临时 SQLite 数据库并注入到 DbStockRepository。** (1 connections) — `collector/tests/integration/test_collection_flow.py`
- **Stock 仓储集成测试（SQLite）。** (1 connections) — `collector/tests/integration/test_collection_flow.py`
- **.test_save_and_find_stock()** (1 connections) — `collector/tests/integration/test_collection_flow.py`

## Relationships

- [[Stock Repository Python]] (1 shared connections)
- [[Collector Config & Stock Domain]] (1 shared connections)
- [[Community 33]] (1 shared connections)

## Source Files

- `collector/tests/integration/test_collection_flow.py`

## Audit Trail

- EXTRACTED: 12 (80%)
- INFERRED: 3 (20%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*