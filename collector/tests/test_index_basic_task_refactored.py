"""Tests for refactored IndexBasicTask (BaseTask subclass)."""
from unittest.mock import MagicMock
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.index_basic_task import IndexBasicTask


class TestIndexBasicTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = IndexBasicTask(db=db, source=source, monitor=monitor)
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(IndexBasicTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert IndexBasicTask.task_name == "index_basic"
        assert IndexBasicTask.data_type == "index_basic"

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        source.get_index_list.return_value = [
            {"index_code": "000001", "display_name": "上证指数"},
        ]
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        result = task.run_full()
        assert isinstance(result, TaskResult)
        assert result.rows == 1
        monitor.log_task_start.assert_not_called()  # execute() handles monitor, not run_full()

    def test_run_partial(self):
        task, db, source, monitor = self._make_task()
        source.get_index_list.return_value = [
            {"index_code": "000001", "display_name": "上证指数"},
            {"index_code": "399001", "display_name": "深证成指"},
        ]
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        result = task.run_partial(identifiers=["000001"])
        assert isinstance(result, TaskResult)
        assert result.rows == 1

    def test_run_incremental_not_implemented(self):
        task, db, source, monitor = self._make_task()
        result = task.run_incremental()
        assert isinstance(result, TaskResult)
        assert result.rows == 0  # default no-op for incremental

    def test_run_backward_compat(self):
        """run() 方法应保持向后兼容"""
        task, db, source, monitor = self._make_task()
        source.get_index_list.return_value = [
            {"index_code": "000001", "display_name": "上证指数"},
        ]
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        count = task.run()
        assert count == 1
