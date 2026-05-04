from unittest.mock import MagicMock, patch
import pytest
from collector.db.postgres import PostgresDB


class TestPostgresDB:
    def _mock_pool(self, db):
        """辅助：将 _pool 替换为 MagicMock"""
        mock_pool = MagicMock()
        db._pool = mock_pool
        return mock_pool

    def test_init(self):
        with patch("collector.db.postgres.ConnectionPool") as MockPool:
            db = PostgresDB("localhost", 5432, "testdb", "user", "pass", pool_min_size=2, pool_max_size=10)
            MockPool.assert_called_once()
            call_kwargs = MockPool.call_args.kwargs
            assert call_kwargs["min_size"] == 2
            assert call_kwargs["max_size"] == 10

    def test_connection_context(self):
        db = PostgresDB("localhost", 5432, "testdb", "user", "pass")
        mock_pool = self._mock_pool(db)
        mock_conn = MagicMock()
        mock_pool.connection.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_pool.connection.return_value.__exit__ = MagicMock(return_value=False)

        with db.connection() as conn:
            assert conn is mock_conn
        mock_pool.connection.assert_called_once()

    def test_transaction_commit(self):
        db = PostgresDB("localhost", 5432, "testdb", "user", "pass")
        mock_pool = self._mock_pool(db)
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        mock_pool.connection.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_pool.connection.return_value.__exit__ = MagicMock(return_value=False)

        with db.transaction() as cur:
            cur.execute("SELECT 1")
        mock_conn.commit.assert_called_once()
        mock_conn.rollback.assert_not_called()

    def test_transaction_rollback(self):
        db = PostgresDB("localhost", 5432, "testdb", "user", "pass")
        mock_pool = self._mock_pool(db)
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        mock_pool.connection.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_pool.connection.return_value.__exit__ = MagicMock(return_value=False)

        with pytest.raises(RuntimeError):
            with db.transaction() as cur:
                cur.execute("SELECT 1")
                raise RuntimeError("boom")
        mock_conn.rollback.assert_called_once()
        mock_conn.commit.assert_not_called()

    def test_context_manager(self):
        db = PostgresDB("localhost", 5432, "testdb", "user", "pass")
        mock_pool = self._mock_pool(db)
        with db as d:
            assert d is db
        mock_pool.close.assert_called_once()

    def test_insert_many(self):
        db = PostgresDB("localhost", 5432, "testdb", "user", "pass")
        mock_pool = self._mock_pool(db)
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.rowcount = 2
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        mock_pool.connection.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_pool.connection.return_value.__exit__ = MagicMock(return_value=False)

        rows = [(1, "a"), (2, "b")]
        count = db.insert_many("INSERT INTO t (id, name) VALUES (%s, %s)", rows)
        assert count == 2
        mock_cur.executemany.assert_called_once_with("INSERT INTO t (id, name) VALUES (%s, %s)", rows)
        mock_conn.commit.assert_called_once()

    def test_upsert_many(self):
        db = PostgresDB("localhost", 5432, "testdb", "user", "pass")
        mock_pool = self._mock_pool(db)
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.rowcount = 3
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        mock_pool.connection.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_pool.connection.return_value.__exit__ = MagicMock(return_value=False)

        sql = "INSERT INTO t (id) VALUES (%s) ON CONFLICT (id) DO UPDATE SET id=EXCLUDED.id"
        count = db.upsert_many(sql, [(1,), (2,), (3,)])
        assert count == 3
        mock_cur.executemany.assert_called_once_with(sql, [(1,), (2,), (3,)])
