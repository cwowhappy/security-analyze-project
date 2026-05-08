"""Tests for refactored EtfBasicTask (BaseTask subclass)."""
from unittest.mock import MagicMock
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.etf_basic_task import EtfBasicTask


class TestEtfBasicTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = EtfBasicTask(db=db, source=source, monitor=monitor)
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(EtfBasicTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert EtfBasicTask.task_name == "etf_basic"
        assert EtfBasicTask.data_type == "etf_basic"

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        source.get_etf_spot_list.return_value = [
            {"代码": "510050", "名称": "华夏上证50ETF"},
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

    def test_run_partial(self):
        task, db, source, monitor = self._make_task()
        source.get_etf_spot_list.return_value = [
            {"代码": "510050", "名称": "华夏上证50ETF"},
            {"代码": "510300", "名称": "华泰柏瑞沪深300ETF"},
        ]
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        result = task.run_partial(identifiers=["510050"])
        assert isinstance(result, TaskResult)
        assert result.rows == 1

    def test_run_incremental_not_implemented(self):
        task, db, source, monitor = self._make_task()
        result = task.run_incremental()
        assert isinstance(result, TaskResult)
        assert result.rows == 0

    def test_run_backward_compat(self):
        task, db, source, monitor = self._make_task()
        source.get_etf_spot_list.return_value = [
            {"代码": "510050", "名称": "华夏上证50ETF"},
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
