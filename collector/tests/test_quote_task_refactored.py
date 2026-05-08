"""Tests for refactored QuoteTask (BaseTask subclass)."""
from unittest.mock import MagicMock, patch
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.quote_task import QuoteTask


class TestQuoteTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = QuoteTask(db=db, source=source, monitor=monitor)
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(QuoteTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert QuoteTask.task_name == "quote"
        assert QuoteTask.data_type == "quote"

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        db.fetchall.return_value = [("600519",)]
        source.get_stock_daily_quote.return_value = __import__("pandas").DataFrame({
            "开盘": [1700.0], "最高": [1720.0], "最低": [1690.0],
            "收盘": [1710.0], "成交量": [10000], "成交额": [17100000.0],
        })

        result = task.run_full(trade_date="2024-01-15")
        assert isinstance(result, TaskResult)
        assert result.rows == 1
        db.upsert_many.assert_called_once()

    def test_run_partial(self):
        task, db, source, monitor = self._make_task()
        source.get_stock_daily_quote.return_value = __import__("pandas").DataFrame({
            "开盘": [1700.0], "最高": [1720.0], "最低": [1690.0],
            "收盘": [1710.0], "成交量": [10000], "成交额": [17100000.0],
        })

        result = task.run_partial(identifiers=["600519", "000001"], trade_date="2024-01-15")
        assert isinstance(result, TaskResult)
        assert result.rows == 2
        db.upsert_many.assert_called_once()

    def test_run_incremental(self):
        task, db, source, monitor = self._make_task()
        db.fetchone.return_value = ("2024-01-14",)
        db.fetchall.return_value = [("600519",)]
        source.get_stock_daily_quote.return_value = __import__("pandas").DataFrame({
            "开盘": [1700.0], "最高": [1720.0], "最低": [1690.0],
            "收盘": [1710.0], "成交量": [10000], "成交额": [17100000.0],
        })

        result = task.run_incremental()
        assert isinstance(result, TaskResult)
        assert result.rows == 1

    def test_run_backward_compat(self):
        task, db, source, monitor = self._make_task()
        db.fetchall.return_value = [("600519",)]
        source.get_stock_daily_quote.return_value = __import__("pandas").DataFrame({
            "开盘": [1700.0], "最高": [1720.0], "最低": [1690.0],
            "收盘": [1710.0], "成交量": [10000], "成交额": [17100000.0],
        })

        task.run(trade_date="2024-01-15")
        db.upsert_many.assert_called_once()
