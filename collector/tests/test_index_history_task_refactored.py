"""Tests for refactored IndexHistoryTask (BaseTask subclass)."""
from unittest.mock import MagicMock, patch
import pandas as pd
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.index_history_task import IndexHistoryTask, GRANULARITY_MAP


class TestIndexHistoryTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = IndexHistoryTask(db=db, source=source, monitor=monitor, max_workers=1)
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(IndexHistoryTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert IndexHistoryTask.task_name == "index_history"
        assert IndexHistoryTask.data_type == "index_history"

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        # mock db.connection -> cursor -> fetchall for _get_all_index_codes
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.fetchall.return_value = [("000001",)]
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        source.get_index_history.return_value = pd.DataFrame({
            "日期": ["2024-01-01"], "开盘": [3000.0], "收盘": [3050.0],
            "最高": [3100.0], "最低": [2950.0], "成交量": [1000000],
            "成交额": [500000000.0], "振幅": [3.33], "涨跌幅": [1.67],
            "涨跌额": [50.0], "换手率": [0.50],
        })
        monitor.get_session_progress.return_value = set()

        result = task.run_full()
        assert isinstance(result, TaskResult)
        assert result.rows >= 1

    def test_run_partial(self):
        task, db, source, monitor = self._make_task()
        source.get_index_history.return_value = pd.DataFrame({
            "日期": ["2024-01-01"], "开盘": [3000.0], "收盘": [3050.0],
            "最高": [3100.0], "最低": [2950.0], "成交量": [1000000],
            "成交额": [500000000.0], "振幅": [3.33], "涨跌幅": [1.67],
            "涨跌额": [50.0], "换手率": [0.50],
        })
        monitor.get_session_progress.return_value = set()

        result = task.run_partial(identifiers=["000001"], granularities=["day"])
        assert isinstance(result, TaskResult)
        assert result.rows >= 1

    def test_run_incremental(self):
        task, db, source, monitor = self._make_task()
        db.fetchall.return_value = [("000001",)]
        db.fetchone.return_value = ("2024-01-10",)
        source.get_index_history.return_value = pd.DataFrame({
            "日期": ["2024-01-11"], "开盘": [3000.0], "收盘": [3050.0],
            "最高": [3100.0], "最低": [2950.0], "成交量": [1000000],
            "成交额": [500000000.0], "振幅": [3.33], "涨跌幅": [1.67],
            "涨跌额": [50.0], "换手率": [0.50],
        })
        monitor.get_session_progress.return_value = set()

        result = task.run_incremental()
        assert isinstance(result, TaskResult)

    def test_run_backward_compat(self):
        task, db, source, monitor = self._make_task()
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.fetchall.return_value = [("000001",)]
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        source.get_index_history.return_value = pd.DataFrame({
            "日期": ["2024-01-01"], "开盘": [3000.0], "收盘": [3050.0],
            "最高": [3100.0], "最低": [2950.0], "成交量": [1000000],
            "成交额": [500000000.0], "振幅": [3.33], "涨跌幅": [1.67],
            "涨跌额": [50.0], "换手率": [0.50],
        })
        monitor.get_session_progress.return_value = set()

        task.run()
        source.get_index_history.assert_called()

    def test_uses_monitor_for_progress(self):
        """应使用 monitor 而非直接 SQL 操作 progress 表"""
        task, db, source, monitor = self._make_task()
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.fetchall.return_value = [("000001",)]
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        source.get_index_history.return_value = pd.DataFrame({
            "日期": ["2024-01-01"], "开盘": [3000.0], "收盘": [3050.0],
            "最高": [3100.0], "最低": [2950.0], "成交量": [1000000],
            "成交额": [500000000.0], "振幅": [3.33], "涨跌幅": [1.67],
            "涨跌额": [50.0], "换手率": [0.50],
        })
        monitor.get_session_progress.return_value = set()

        task.run_full()
        # 成功后应调用 monitor.log_task_progress 标记进度
        monitor.log_task_progress.assert_called()
