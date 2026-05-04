from unittest.mock import MagicMock, patch
import pytest
import numpy as np
import pandas as pd
from collector.tasks.finance_task import FinanceTask
from collector.models import FinancialReport


class TestFinanceTaskBulkWrite:
    def _create_task(self):
        db = MagicMock()
        source = MagicMock()
        task = FinanceTask(db=db, source=source)
        return task, db

    def test_bulk_insert_incremental(self):
        task, db = self._create_task()
        reports = [
            FinancialReport(stock_code="600519", report_date="2024-03-31", report_type="一季报", report_year=2024),
            FinancialReport(stock_code="600519", report_date="2023-12-31", report_type="年报", report_year=2023),
        ]
        db.insert_many.return_value = 2
        created, updated = task._bulk_write_reports(reports, incremental=True)
        assert created == 2
        assert updated == 0
        db.insert_many.assert_called_once()
        sql, params = db.insert_many.call_args[0]
        assert "INSERT INTO financial_report" in sql
        assert len(params) == 2

    def test_bulk_upsert_full(self):
        task, db = self._create_task()
        reports = [
            FinancialReport(stock_code="600519", report_date="2024-03-31", report_type="一季报", report_year=2024),
        ]
        db.upsert_many.return_value = 1
        created, updated = task._bulk_write_reports(reports, incremental=False)
        assert created == 0
        assert updated == 1
        db.upsert_many.assert_called_once()
        sql, params = db.upsert_many.call_args[0]
        assert "ON CONFLICT" in sql
        assert len(params) == 1

    def test_bulk_insert_empty(self):
        task, db = self._create_task()
        created, updated = task._bulk_write_reports([], incremental=True)
        assert created == 0
        assert updated == 0
        db.insert_many.assert_not_called()

    def test_bulk_insert_fallback_on_error(self):
        task, db = self._create_task()
        reports = [
            FinancialReport(stock_code="600519", report_date="2024-03-31", report_type="一季报", report_year=2024),
            FinancialReport(stock_code="600519", report_date="2023-12-31", report_type="年报", report_year=2023),
        ]
        db.insert_many.side_effect = Exception("批量失败")
        db.execute.return_value = None
        created, updated = task._bulk_write_reports(reports, incremental=True)
        assert created == 2
        assert updated == 0
        assert db.execute.call_count == 2


class TestFinanceTaskCleanDict:
    def test_nan_to_none(self):
        data = {"a": 1.0, "b": float("nan"), "c": None}
        result = FinanceTask._clean_dict(data)
        assert result["a"] == 1.0
        assert result["b"] is None
        assert result["c"] is None

    def test_numpy_scalar_to_python(self):
        data = {"a": np.int64(100), "b": np.float64(3.14), "c": np.array([1, 2, 3])}
        result = FinanceTask._clean_dict(data)
        assert isinstance(result["a"], int)
        assert isinstance(result["b"], float)
        assert result["c"] == [1, 2, 3]

    def test_pandas_timestamp(self):
        ts = pd.Timestamp("2024-03-31")
        data = {"date": ts}
        result = FinanceTask._clean_dict(data)
        assert result["date"] is ts  # Timestamp 可 JSON 序列化，无需转换


class TestFinanceTaskProcessBatch:
    def test_process_batch_serial(self):
        db = MagicMock()
        source = MagicMock()
        task = FinanceTask(db=db, source=source, batch_concurrent_workers=1)

        with patch.object(task, "_collect_by_stock_code", return_value=(2, 1)) as mock_collect:
            c, u, f = task._process_batch(["600519", "000001"], "sess-1", None, None, False)
            assert c == 4  # 2 stocks * (2 created + ... wait, each returns (2,1))
            assert u == 2
            assert f == 0
            assert mock_collect.call_count == 2

    def test_process_batch_concurrent(self):
        db = MagicMock()
        source = MagicMock()
        task = FinanceTask(db=db, source=source, batch_concurrent_workers=2)

        def side_effect(code, *args, **kwargs):
            if code == "600519":
                return 2, 0
            return 1, 1

        with patch.object(task, "_collect_by_stock_code", side_effect=side_effect):
            c, u, f = task._process_batch(["600519", "000001"], "sess-1", None, None, False)
            assert c == 3
            assert u == 1
            assert f == 0
