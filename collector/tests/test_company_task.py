"""Tests for CompanyTask with industry classification mapping."""
from unittest.mock import MagicMock, patch
import pytest

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.company_task import CompanyTask, L1_TO_801_MAPPING, L2_TO_801_MAPPING
from collector.models import CompanyIndustryMapping


class TestCompanyTaskIndustryMapping:
    def _make_task(self):
        db = MagicMock(spec=PostgresDB)
        source = MagicMock(spec=AkshareSource)
        task = CompanyTask(db=db, source=source)
        return task, db, source

    def test_preload_sw_mapping(self):
        task, db, source = self._make_task()

        hist_df = __import__("pandas").DataFrame({
            "symbol": ["000001", "000001", "600519"],
            "start_date": ["1991-04-03", "2021-07-30", "2001-08-27"],
            "industry_code": ["440101", "480301", "340501"],
            "update_time": ["2015-10-27", "2025-12-15", "2025-12-15"],
        })

        with patch("akshare.stock_industry_clf_hist_sw", return_value=hist_df):
            task._preload_sw_mapping()

        assert "000001" in task._sw_mapping
        assert "600519" in task._sw_mapping
        # 000001 最新是 480301 -> L1=48->801780, L2=4803->801783
        assert task._sw_mapping["000001"] == [("801780", "801783")]
        # 600519 最新是 340501 -> L1=34->801760, L2=3405->801125
        assert task._sw_mapping["600519"] == [("801760", "801125")]

    def test_preload_sw_mapping_missing_code(self):
        """无法映射的 L2 code 应被跳过"""
        task, db, source = self._make_task()

        hist_df = __import__("pandas").DataFrame({
            "symbol": ["000001"],
            "start_date": ["2021-07-30"],
            "industry_code": ["999999"],  # 不存在的编码
        })

        with patch("akshare.stock_industry_clf_hist_sw", return_value=hist_df):
            task._preload_sw_mapping()

        assert "000001" not in task._sw_mapping

    def test_preload_em_mapping(self):
        task, db, source = self._make_task()
        db.fetchall.return_value = [
            ("BK0477", "白酒Ⅱ"),
            ("BK0475", "银行Ⅱ"),
        ]

        task._preload_em_mapping()

        assert task._em_name_to_code["白酒Ⅱ"] == "BK0477"
        assert task._em_name_to_code["银行Ⅱ"] == "BK0475"

    def test_save_industry_mappings_sw_only(self):
        task, db, source = self._make_task()
        task._sw_mapping = {
            "000001": [("801780", "801783")],
        }
        task._em_name_to_code = {}

        task._save_industry_mappings(company_id=1, stock_code="000001", em_industry_name=None)

        db.upsert_many.assert_called_once()
        sql, params = db.upsert_many.call_args[0]
        assert len(params) == 1
        assert params[0] == (1, "SW", "801780", "801783", True)

    def test_save_industry_mappings_em_only(self):
        task, db, source = self._make_task()
        task._sw_mapping = {}
        task._em_name_to_code = {"白酒Ⅱ": "BK0477"}

        task._save_industry_mappings(company_id=1, stock_code="600519", em_industry_name="白酒Ⅱ")

        db.upsert_many.assert_called_once()
        sql, params = db.upsert_many.call_args[0]
        assert len(params) == 1
        # EM 的 level1_code 和 level2_code 相同
        assert params[0] == (1, "EM", "BK0477", "BK0477", True)

    def test_save_industry_mappings_both(self):
        task, db, source = self._make_task()
        task._sw_mapping = {
            "000001": [("801780", "801783")],
        }
        task._em_name_to_code = {"银行Ⅱ": "BK0475"}

        task._save_industry_mappings(company_id=1, stock_code="000001", em_industry_name="银行Ⅱ")

        db.upsert_many.assert_called_once()
        sql, params = db.upsert_many.call_args[0]
        assert len(params) == 2
        sw_param = [p for p in params if p[1] == "SW"][0]
        em_param = [p for p in params if p[1] == "EM"][0]
        assert sw_param == (1, "SW", "801780", "801783", True)
        assert em_param == (1, "EM", "BK0475", "BK0475", True)

    def test_save_industry_mappings_unknown_em_name(self):
        task, db, source = self._make_task()
        task._sw_mapping = {}
        task._em_name_to_code = {}

        task._save_industry_mappings(company_id=1, stock_code="600519", em_industry_name="未知行业")

        db.upsert_many.assert_not_called()

    def test_save_industry_mappings_db_error(self):
        task, db, source = self._make_task()
        task._sw_mapping = {"000001": [("801780", "801783")]}
        db.upsert_many.side_effect = Exception("db error")

        # 不应抛出异常
        task._save_industry_mappings(company_id=1, stock_code="000001", em_industry_name=None)
        db.upsert_many.assert_called_once()

    def test_mapping_tables_not_empty(self):
        """确保硬编码的映射表不为空"""
        assert len(L1_TO_801_MAPPING) > 0
        assert len(L2_TO_801_MAPPING) > 0
