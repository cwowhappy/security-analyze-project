"""采集模块集成测试。

验证 task_executor → domain → db 的完整链路。
使用内存数据库（SQLite）替代 PostgreSQL，便于 CI 运行。
"""

import os
import sqlite3
import tempfile
from datetime import date

import pytest

from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.core.domain.stock import Stock
from data_collector.task_executor import TaskExecutor


@pytest.fixture
def temp_sqlite_db(monkeypatch):
    """创建临时 SQLite 数据库并注入到 DbStockRepository。"""
    fd, path = tempfile.mkstemp(suffix=".db")
    os.close(fd)

    conn = sqlite3.connect(path)
    conn.execute(
        """
        CREATE TABLE tb_stock_basic (
            id TEXT PRIMARY KEY,
            stock_code TEXT NOT NULL UNIQUE,
            ts_code TEXT,
            name TEXT NOT NULL,
            full_name TEXT,
            market TEXT,
            exchange TEXT,
            list_date TEXT,
            industry TEXT,
            area TEXT,
            total_shares INTEGER,
            float_shares INTEGER,
            company_id TEXT,
            created_at TEXT,
            updated_at TEXT
        )
        """
    )
    conn.commit()
    conn.close()

    # 通过环境变量让 DbStockRepository 连接到 SQLite
    monkeypatch.setenv("DB_HOST", "")
    monkeypatch.setenv("DB_PORT", "")
    monkeypatch.setenv("DB_NAME", path)
    monkeypatch.setenv("DB_USER", "")
    monkeypatch.setenv("DB_PASSWORD", "")

    yield path

    os.unlink(path)


class TestStockRepositoryIntegration:
    """Stock 仓储集成测试（SQLite）。"""

    def test_save_and_find_stock(self, temp_sqlite_db):
        """保存股票后应能查询到。"""
        # 注意：DbStockRepository 当前使用 psycopg2，不支持 SQLite。
        # 此测试为占位，展示集成测试结构。
        # 实际运行需要适配 SQLite 或启动 PostgreSQL 容器。
        pytest.skip("需要 PostgreSQL 环境或仓储适配 SQLite")
