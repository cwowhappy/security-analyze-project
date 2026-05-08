"""Test Monitor enhancements: task_key support in log_task_progress."""
from unittest.mock import MagicMock
import pytest

from collector.monitor import Monitor


class TestMonitorEnhanced:
    def _create_monitor(self):
        mock_db = MagicMock()
        mock_db.execute_returning.return_value = (1,)
        return Monitor(db=mock_db)

    def test_log_task_progress_with_task_key(self):
        """log_task_progress 应支持 task_key 参数作为 stock_code 的语义替代"""
        monitor = self._create_monitor()
        monitor.log_task_progress(
            session_id="sess-1",
            task_key="000001#day",
            status="success",
            rows_created=10,
            rows_updated=5,
        )
        monitor.db.execute.assert_called_once()
        call_args = monitor.db.execute.call_args
        # SQL should use the task_key value in the stock_code column
        assert "sess-1" in call_args[0][1]
        assert "000001#day" in call_args[0][1]
        assert "success" in call_args[0][1]

    def test_log_task_progress_with_stock_code_backward_compat(self):
        """log_task_progress 应保留 stock_code 参数向后兼容"""
        monitor = self._create_monitor()
        monitor.log_task_progress(
            session_id="sess-1",
            stock_code="000001",
            status="success",
            rows_created=10,
            rows_updated=5,
        )
        monitor.db.execute.assert_called_once()
        call_args = monitor.db.execute.call_args
        assert "000001" in call_args[0][1]

    def test_get_session_progress_returns_task_keys(self):
        """get_session_progress 返回的集合元素应为通用 task_key"""
        monitor = self._create_monitor()
        monitor.db.fetchall.return_value = [
            ("000001#day",),
            ("000001#week",),
            ("000002#day",),
        ]
        result = monitor.get_session_progress("sess-1")
        assert result == {"000001#day", "000001#week", "000002#day"}
