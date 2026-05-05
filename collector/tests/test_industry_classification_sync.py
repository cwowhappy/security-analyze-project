"""Tests for industry_classification_sync task."""
from unittest.mock import MagicMock, patch
import pandas as pd
import pytest

from collector.db.postgres import PostgresDB
from collector.models import IndustryCategory
from collector.tasks import industry_classification_sync as sync_module


class TestSyncSwIndustries:
    def test_sync_sw_success(self):
        db = MagicMock(spec=PostgresDB)
        db.upsert_many = MagicMock(return_value=3)

        sw1_df = pd.DataFrame({
            "行业代码": ["801010.SI", "801030.SI"],
            "行业名称": ["农林牧渔", "基础化工"],
        })
        sw2_df = pd.DataFrame({
            "行业代码": ["801016.SI", "801015.SI"],
            "行业名称": ["种植业", "渔业"],
            "上级行业": ["农林牧渔", "农林牧渔"],
        })

        with patch("akshare.sw_index_first_info", return_value=sw1_df), \
             patch("akshare.sw_index_second_info", return_value=sw2_df):
            sync_module.sync_sw_industries(db)

        db.upsert_many.assert_called_once()
        sql, params = db.upsert_many.call_args[0]
        assert "industry_category" in sql
        assert len(params) == 4  # 2 一级 + 2 二级

        # 验证一级 parent_code 为空
        cat1 = [p for p in params if p[1] == 1]
        assert len(cat1) == 2
        assert cat1[0][4] is None  # parent_code

        # 验证二级 parent_code 已映射为一级 code
        cat2 = [p for p in params if p[1] == 2]
        assert len(cat2) == 2
        assert cat2[0][4] == "801010"  # 种植业的上级是农林牧渔 -> 801010

    def test_sync_sw_empty(self):
        db = MagicMock(spec=PostgresDB)
        with patch("akshare.sw_index_first_info", side_effect=Exception("network")), \
             patch("akshare.sw_index_second_info", side_effect=Exception("network")):
            sync_module.sync_sw_industries(db)
        db.upsert_many.assert_not_called()


class TestSyncEmIndustries:
    def test_sync_em_success(self):
        db = MagicMock(spec=PostgresDB)
        db.upsert_many = MagicMock(return_value=2)

        em_df = pd.DataFrame({
            "板块名称": ["白酒Ⅱ", "银行Ⅱ"],
            "板块代码": ["BK0477", "BK0475"],
        })

        with patch("akshare.stock_board_industry_name_em", return_value=em_df):
            sync_module.sync_em_industries(db)

        db.upsert_many.assert_called_once()
        sql, params = db.upsert_many.call_args[0]
        assert len(params) == 2
        assert params[0][0] == "EM"
        assert params[0][1] == 2
        assert params[0][2] == "BK0477"
        assert params[0][3] == "白酒Ⅱ"

    def test_sync_em_empty(self):
        db = MagicMock(spec=PostgresDB)
        with patch("akshare.stock_board_industry_name_em", side_effect=Exception("network")):
            sync_module.sync_em_industries(db)
        db.upsert_many.assert_not_called()


class TestRun:
    def test_run_calls_both_syncs(self):
        db = MagicMock(spec=PostgresDB)
        with patch.object(sync_module, "sync_sw_industries") as mock_sw, \
             patch.object(sync_module, "sync_em_industries") as mock_em:
            sync_module.run(db)
        mock_sw.assert_called_once_with(db)
        mock_em.assert_called_once_with(db)
