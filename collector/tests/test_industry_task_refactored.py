"""Tests for IndustryTask (refactored from industry_classification_sync)."""
from unittest.mock import MagicMock, patch
import pandas as pd
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.industry_task import IndustryTask


class TestIndustryTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = IndustryTask(db=db, source=source, monitor=monitor)
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(IndustryTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert IndustryTask.task_name == "industry_sync"
        assert IndustryTask.data_type == "industry_category"

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        sw1_df = pd.DataFrame({
            "行业代码": ["801010.SI"],
            "行业名称": ["农林牧渔"],
        })
        sw2_df = pd.DataFrame({
            "行业代码": ["801016.SI"],
            "行业名称": ["种植业"],
            "上级行业": ["农林牧渔"],
        })
        em_df = pd.DataFrame({
            "板块名称": ["白酒Ⅱ"],
            "板块代码": ["BK0477"],
        })

        with patch("akshare.sw_index_first_info", return_value=sw1_df), \
             patch("akshare.sw_index_second_info", return_value=sw2_df), \
             patch("akshare.stock_board_industry_name_em", return_value=em_df):
            result = task.run_full()

        assert isinstance(result, TaskResult)
        assert result.rows == 3  # 1 SW L1 + 1 SW L2 + 1 EM
        assert db.upsert_many.call_count == 2  # SW 一次, EM 一次

    def test_run_partial_sw_only(self):
        task, db, source, monitor = self._make_task()
        sw1_df = pd.DataFrame({
            "行业代码": ["801010.SI"],
            "行业名称": ["农林牧渔"],
        })

        with patch("akshare.sw_index_first_info", return_value=sw1_df), \
             patch("akshare.sw_index_second_info", side_effect=Exception("skip")):
            result = task.run_partial(identifiers=["SW"])

        assert isinstance(result, TaskResult)
        assert result.rows == 1
        assert db.upsert_many.call_count == 1

    def test_run_partial_em_only(self):
        task, db, source, monitor = self._make_task()
        em_df = pd.DataFrame({
            "板块名称": ["白酒Ⅱ"],
            "板块代码": ["BK0477"],
        })

        with patch("akshare.stock_board_industry_name_em", return_value=em_df):
            result = task.run_partial(identifiers=["EM"])

        assert isinstance(result, TaskResult)
        assert result.rows == 1
        assert db.upsert_many.call_count == 1

    def test_run_partial_both(self):
        task, db, source, monitor = self._make_task()
        sw1_df = pd.DataFrame({
            "行业代码": ["801010.SI"],
            "行业名称": ["农林牧渔"],
        })
        em_df = pd.DataFrame({
            "板块名称": ["白酒Ⅱ"],
            "板块代码": ["BK0477"],
        })

        with patch("akshare.sw_index_first_info", return_value=sw1_df), \
             patch("akshare.sw_index_second_info", side_effect=Exception("skip")), \
             patch("akshare.stock_board_industry_name_em", return_value=em_df):
            result = task.run_partial(identifiers=["SW", "EM"])

        assert isinstance(result, TaskResult)
        assert result.rows == 2
        assert db.upsert_many.call_count == 2

    def test_run_incremental(self):
        task, db, source, monitor = self._make_task()
        with patch.object(task, "run_full", return_value=TaskResult(rows=5)):
            result = task.run_incremental()
        assert isinstance(result, TaskResult)
        assert result.rows == 5
