"""Tests for refactored FinanceTask (BaseTask subclass)."""
from unittest.mock import MagicMock, patch
import pandas as pd
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.finance_task import FinanceTask


class TestFinanceTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = FinanceTask(
            db=db, source=source, monitor=monitor,
            max_workers=1, batch_concurrent_workers=1,
        )
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(FinanceTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert FinanceTask.task_name == "finance_report"
        assert FinanceTask.data_type == "finance_report"

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        db.fetchall.return_value = [("600519",)]
        source.get_balance_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "REPORT_TYPE": ["年报"],
            "NOTICE_DATE": ["2024-04-01"], "CURRENCY": ["CNY"],
            "TOTAL_ASSETS": [1000000.0],
        })
        source.get_profit_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "TOTAL_OPERATE_INCOME": [500000.0],
        })
        source.get_cash_flow_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "NETCASH_OPERATE": [200000.0],
        })
        monitor.get_session_progress.return_value = set()

        result = task.run_full(batch_size=1)
        assert isinstance(result, TaskResult)
        assert result.rows >= 1

    def test_run_partial(self):
        task, db, source, monitor = self._make_task()
        source.get_balance_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "REPORT_TYPE": ["年报"],
            "NOTICE_DATE": ["2024-04-01"], "CURRENCY": ["CNY"],
            "TOTAL_ASSETS": [1000000.0],
        })
        source.get_profit_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "TOTAL_OPERATE_INCOME": [500000.0],
        })
        source.get_cash_flow_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "NETCASH_OPERATE": [200000.0],
        })

        result = task.run_partial(identifiers=["600519"])
        assert isinstance(result, TaskResult)
        assert result.rows >= 1

    def test_run_incremental(self):
        task, db, source, monitor = self._make_task()
        db.fetchall.return_value = [("600519",)]
        db.fetchone.return_value = ("2023-06-30",)
        source.get_balance_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31", "2023-06-30"], "REPORT_TYPE": ["年报", "中报"],
            "NOTICE_DATE": ["2024-04-01", "2023-08-01"], "CURRENCY": ["CNY", "CNY"],
            "TOTAL_ASSETS": [1000000.0, 900000.0],
        })
        source.get_profit_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31", "2023-06-30"], "TOTAL_OPERATE_INCOME": [500000.0, 250000.0],
        })
        source.get_cash_flow_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31", "2023-06-30"], "NETCASH_OPERATE": [200000.0, 100000.0],
        })
        monitor.get_session_progress.return_value = set()

        result = task.run_incremental(batch_size=1)
        assert isinstance(result, TaskResult)

    def test_resume_session(self):
        task, db, source, monitor = self._make_task()
        monitor.get_session_params.return_value = {
            "stock_codes": ["600519"],
            "batch_size": 1,
        }
        monitor.get_session_progress.return_value = set()
        db.fetchall.return_value = [("600519",)]
        source.get_balance_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "REPORT_TYPE": ["年报"],
            "NOTICE_DATE": ["2024-04-01"], "CURRENCY": ["CNY"],
            "TOTAL_ASSETS": [1000000.0],
        })
        source.get_profit_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "TOTAL_OPERATE_INCOME": [500000.0],
        })
        source.get_cash_flow_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "NETCASH_OPERATE": [200000.0],
        })

        result = task.resume_session(session_id="sess-123")
        assert isinstance(result, TaskResult)

    def test_run_backward_compat(self):
        task, db, source, monitor = self._make_task()
        db.fetchall.return_value = [("600519",)]
        source.get_balance_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "REPORT_TYPE": ["年报"],
            "NOTICE_DATE": ["2024-04-01"], "CURRENCY": ["CNY"],
            "TOTAL_ASSETS": [1000000.0],
        })
        source.get_profit_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "TOTAL_OPERATE_INCOME": [500000.0],
        })
        source.get_cash_flow_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "NETCASH_OPERATE": [200000.0],
        })
        monitor.get_session_progress.return_value = set()

        task.run(batch_size=1)
        source.get_balance_sheet.assert_called()

    def test_run_by_stock_code_backward_compat(self):
        task, db, source, monitor = self._make_task()
        source.get_balance_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "REPORT_TYPE": ["年报"],
            "NOTICE_DATE": ["2024-04-01"], "CURRENCY": ["CNY"],
            "TOTAL_ASSETS": [1000000.0],
        })
        source.get_profit_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "TOTAL_OPERATE_INCOME": [500000.0],
        })
        source.get_cash_flow_sheet.return_value = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31"], "NETCASH_OPERATE": [200000.0],
        })

        task.run_by_stock_code("600519")
        source.get_balance_sheet.assert_called()
