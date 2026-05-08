"""Tests for refactored CompanyTask (BaseTask subclass)."""
from unittest.mock import MagicMock, patch
import pytest

from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.company_task import CompanyTask


class TestCompanyTaskRefactored:
    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        monitor = MagicMock()
        task = CompanyTask(db=db, source=source, monitor=monitor)
        return task, db, source, monitor

    def test_is_base_task_subclass(self):
        assert issubclass(CompanyTask, BaseTask)

    def test_task_name_and_data_type(self):
        assert CompanyTask.task_name == "company"
        assert CompanyTask.data_type == "company"

    def test_no_hardcoded_mappings(self):
        """硬编码映射表应从类属性中移除"""
        assert not hasattr(CompanyTask, "L1_TO_801_MAPPING")
        assert not hasattr(CompanyTask, "L2_TO_801_MAPPING")

    def test_run_full(self):
        task, db, source, monitor = self._make_task()
        source.get_stock_list.return_value = [
            {"code": "600519", "name": "贵州茅台"},
        ]
        source.get_company_detail.return_value = {
            "公司名称": "贵州茅台酒股份有限公司",
            "A股简称": "贵州茅台",
            "所属行业": "白酒",
            "注册地址": "贵州省遵义市",
            "成立日期": "1999-11-20",
            "注册资金": "125619.78万元",
        }
        source.get_company_info_em.return_value = {"股票简称": "贵州茅台", "行业": "白酒"}
        db.fetchone.side_effect = [
            None,  # company 不存在
            (1,),  # RETURNING id
            None,  # security 不存在
        ]

        result = task.run_full()
        assert isinstance(result, TaskResult)
        assert result.rows >= 1

    def test_run_partial(self):
        task, db, source, monitor = self._make_task()
        source.search_by_name.return_value = [
            {"code": "600519", "name": "贵州茅台"},
        ]
        source.get_company_detail.return_value = {
            "公司名称": "贵州茅台酒股份有限公司",
            "A股简称": "贵州茅台",
        }
        source.get_company_info_em.return_value = None
        db.fetchone.side_effect = [
            None,  # company 不存在
            (1,),  # RETURNING id
            None,  # security 不存在
        ]

        result = task.run_partial(identifiers=["贵州茅台"])
        assert isinstance(result, TaskResult)
        assert result.rows >= 1

    def test_run_incremental(self):
        task, db, source, monitor = self._make_task()
        result = task.run_incremental()
        assert isinstance(result, TaskResult)

    def test_run_backward_compat(self):
        task, db, source, monitor = self._make_task()
        source.get_stock_list.return_value = [
            {"code": "600519", "name": "贵州茅台"},
        ]
        source.get_company_detail.return_value = {
            "公司名称": "贵州茅台酒股份有限公司",
            "A股简称": "贵州茅台",
        }
        source.get_company_info_em.return_value = None
        db.fetchone.side_effect = [
            None,  # company 不存在
            (1,),  # RETURNING id
            None,  # security 不存在
        ]

        task.run()
        source.get_stock_list.assert_called_once()

    def test_run_by_name_backward_compat(self):
        task, db, source, monitor = self._make_task()
        source.search_by_name.return_value = [
            {"code": "600519", "name": "贵州茅台"},
        ]
        source.get_company_detail.return_value = {
            "公司名称": "贵州茅台酒股份有限公司",
            "A股简称": "贵州茅台",
        }
        source.get_company_info_em.return_value = None
        db.fetchone.side_effect = [
            None,  # company 不存在
            (1,),  # RETURNING id
            None,  # security 不存在
        ]

        task.run_by_name("贵州茅台")
        source.search_by_name.assert_called_once_with("贵州茅台")
