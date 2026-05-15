"""数据库连接池单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector.config import Settings
from data_collector.infrastructure import db as db_module


class TestDatabasePool:
    """数据库连接池测试。"""

    def setup_method(self) -> None:
        # 重置全局 pool
        db_module._pool = None

    def test_should_init_pool(self) -> None:
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool") as mock_pool_cls:
            mock_pool = MagicMock()
            mock_pool_cls.return_value = mock_pool

            settings = Settings()
            pool = db_module.init_pool(settings)

            assert pool is mock_pool
            mock_pool_cls.assert_called_once()
            db_module._pool = None

    def test_should_raise_when_pool_not_initialized(self) -> None:
        db_module._pool = None
        with pytest.raises(RuntimeError, match="未初始化"):
            db_module.get_pool()

    def test_should_get_pool_after_init(self) -> None:
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool") as mock_pool_cls:
            mock_pool = MagicMock()
            mock_pool_cls.return_value = mock_pool

            settings = Settings()
            db_module.init_pool(settings)
            assert db_module.get_pool() is mock_pool
            db_module._pool = None

    def test_should_close_pool(self) -> None:
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool") as mock_pool_cls:
            mock_pool = MagicMock()
            mock_pool_cls.return_value = mock_pool

            settings = Settings()
            db_module.init_pool(settings)
            db_module.close_pool()

            mock_pool.closeall.assert_called_once()
            assert db_module._pool is None

    def test_should_execute_query(self) -> None:
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool"):
            mock_conn = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.fetchall.return_value = [{"id": "1", "name": "test"}]

            mock_pool = MagicMock()
            mock_pool.getconn.return_value = mock_conn
            db_module._pool = mock_pool

            # mock RealDictCursor
            with patch("data_collector.infrastructure.db.psycopg2.extras.RealDictCursor"):
                mock_conn.cursor.return_value = mock_cursor
                result = db_module.execute_query("SELECT * FROM test")

            assert len(result) == 1
            assert result[0]["id"] == "1"
            mock_conn.commit.assert_called_once()

            db_module._pool = None

    def test_should_execute_update(self) -> None:
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool"):
            mock_conn = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.rowcount = 3

            mock_pool = MagicMock()
            mock_pool.getconn.return_value = mock_conn
            db_module._pool = mock_pool
            mock_conn.cursor.return_value = mock_cursor

            result = db_module.execute_update("UPDATE test SET x = %s", (1,))

            assert result == 3
            mock_conn.commit.assert_called_once()

            db_module._pool = None

    def test_should_rollback_on_error(self) -> None:
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool"):
            mock_conn = MagicMock()
            mock_cursor = MagicMock()
            mock_cursor.execute.side_effect = Exception("DB Error")

            mock_pool = MagicMock()
            mock_pool.getconn.return_value = mock_conn
            db_module._pool = mock_pool
            mock_conn.cursor.return_value = mock_cursor

            with pytest.raises(Exception, match="DB Error"):
                db_module.execute_update("UPDATE test SET x = %s", (1,))

            mock_conn.rollback.assert_called_once()

            db_module._pool = None

    def test_should_commit_transaction(self) -> None:
        """transaction() 上下文应正确提交并释放连接。"""
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool"):
            mock_conn = MagicMock()
            mock_pool = MagicMock()
            mock_pool.getconn.return_value = mock_conn
            db_module._pool = mock_pool

            with db_module.transaction() as conn:
                cursor = conn.cursor()
                cursor.execute("INSERT INTO test VALUES (%s)", (1,))
                cursor.close()

            mock_conn.commit.assert_called_once()
            mock_pool.putconn.assert_called_once_with(mock_conn)
            db_module._pool = None

    def test_should_rollback_transaction_on_error(self) -> None:
        """transaction() 上下文在异常时应回滚并释放连接。"""
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool"):
            mock_conn = MagicMock()
            mock_pool = MagicMock()
            mock_pool.getconn.return_value = mock_conn
            db_module._pool = mock_pool

            with pytest.raises(Exception, match="Batch Error"):
                with db_module.transaction() as conn:
                    cursor = conn.cursor()
                    cursor.execute("INSERT INTO test VALUES (%s)", (1,))
                    cursor.close()
                    raise Exception("Batch Error")

            mock_conn.rollback.assert_called_once()
            mock_conn.commit.assert_not_called()
            mock_pool.putconn.assert_called_once_with(mock_conn)
            db_module._pool = None
