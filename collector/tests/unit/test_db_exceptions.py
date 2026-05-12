"""数据库模块异常分支单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector import infrastructure
from data_collector.config import Settings
from data_collector.infrastructure.db import (
    close_pool,
    get_connection,
    get_cursor,
    get_pool,
    init_pool,
)


class TestGetPool:
    """get_pool 异常路径测试。"""

    def test_get_pool_without_init_raises(self):
        """未初始化连接池时应抛出 RuntimeError。"""
        close_pool()  # 确保状态干净
        with pytest.raises(RuntimeError, match="数据库连接池未初始化"):
            get_pool()


class TestInitPool:
    """init_pool 行为测试。"""

    def test_init_pool_returns_existing_pool(self):
        """重复初始化应返回已有连接池。"""
        close_pool()
        settings = Settings()
        with patch("data_collector.infrastructure.db.ThreadedConnectionPool") as mock_pool_cls:
            mock_pool = MagicMock()
            mock_pool_cls.return_value = mock_pool

            pool1 = init_pool(settings)
            pool2 = init_pool(settings)

            assert pool1 is pool2
            mock_pool_cls.assert_called_once()

        close_pool()


class TestGetConnection:
    """get_connection 上下文管理器测试。"""

    def test_get_connection_puts_back_on_success(self):
        """正常退出时应归还连接。"""
        close_pool()
        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_pool.getconn.return_value = mock_conn

        with patch("data_collector.infrastructure.db._pool", mock_pool):
            with get_connection() as conn:
                assert conn is mock_conn

        mock_pool.putconn.assert_called_once_with(mock_conn)

    def test_get_connection_puts_back_on_exception(self):
        """异常退出时也应归还连接。"""
        close_pool()
        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_pool.getconn.return_value = mock_conn

        with patch("data_collector.infrastructure.db._pool", mock_pool):
            with pytest.raises(ValueError):
                with get_connection() as conn:
                    assert conn is mock_conn
                    raise ValueError("boom")

        mock_pool.putconn.assert_called_once_with(mock_conn)


class TestGetCursor:
    """get_cursor 上下文管理器测试。"""

    def test_get_cursor_commits_on_success(self):
        """正常执行时应提交事务。"""
        close_pool()
        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        with patch("data_collector.infrastructure.db._pool", mock_pool):
            with get_cursor() as cursor:
                assert cursor is mock_cursor

        mock_conn.commit.assert_called_once()
        mock_conn.rollback.assert_not_called()
        mock_cursor.close.assert_called_once()

    def test_get_cursor_rollback_on_exception(self):
        """异常时应回滚事务并重新抛出。"""
        close_pool()
        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor

        with patch("data_collector.infrastructure.db._pool", mock_pool):
            with pytest.raises(ValueError, match="cursor-error"):
                with get_cursor() as cursor:
                    assert cursor is mock_cursor
                    raise ValueError("cursor-error")

        mock_conn.commit.assert_not_called()
        mock_conn.rollback.assert_called_once()
        mock_cursor.close.assert_called_once()

    def test_get_cursor_with_custom_cursor_factory(self):
        """支持自定义 cursor_factory。"""
        close_pool()
        mock_pool = MagicMock()
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_pool.getconn.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cursor
        factory = MagicMock()

        with patch("data_collector.infrastructure.db._pool", mock_pool):
            with get_cursor(cursor_factory=factory) as cursor:
                assert cursor is mock_cursor

        mock_conn.cursor.assert_called_once_with(cursor_factory=factory)


class TestClosePool:
    """close_pool 测试。"""

    def test_close_pool_closes_all(self):
        """应关闭所有连接并重置状态。"""
        close_pool()
        mock_pool = MagicMock()

        with patch("data_collector.infrastructure.db._pool", mock_pool):
            close_pool()
            mock_pool.closeall.assert_called_once()

        # 再次关闭不应报错
        close_pool()
