"""任务执行器单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.task_executor import TaskExecutor


class TestTaskExecutor:
    """TaskExecutor 测试。"""

    def setup_method(self) -> None:
        self.executor = TaskExecutor(settings=Settings())

    def test_should_execute_stock_full(self) -> None:
        with patch(
            "data_collector.task_executor.run_stock_full",
            return_value={"total": 5, "success": 5, "failed": 0},
        ):
            task = CollectionTask(task_type="stock_full")
            result = self.executor.execute(task)

        assert result.status == "success"
        assert result.task_type == "stock_full"
        assert result.total_count == 5
        assert result.success_count == 5
        assert result.fail_count == 0
        assert result.data_source == "akshare"

    def test_should_execute_company_full(self) -> None:
        with patch(
            "data_collector.task_executor.run_company_full",
            return_value={"total": 3, "success": 3, "failed": 0},
        ):
            task = CollectionTask(task_type="company_full")
            result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 3
        assert result.success_count == 3
        assert result.fail_count == 0
        assert result.data_source == "akshare"

    def test_should_execute_field_supplement(self) -> None:
        with patch(
            "data_collector.task_executor.run_field_supplement",
            return_value={
                "stock_total": 10, "stock_success": 9, "stock_failed": 1,
                "company_total": 5, "company_success": 5, "company_failed": 0,
            },
        ):
            task = CollectionTask(task_type="field_supplement")
            result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 15
        assert result.success_count == 14
        assert result.fail_count == 1
        assert result.data_source == "tushare"

    def test_should_execute_stock_single(self) -> None:
        with (
            patch("data_collector.scripts.stock_full.ak.stock_info_a_code_name") as mock_ak,
            patch("data_collector.adapters.db_stock_repository.DbStockRepository") as MockRepo,
        ):
            mock_df = MagicMock()
            mock_df.__getitem__ = MagicMock(return_value=mock_df)
            mock_df.empty = False
            mock_row = MagicMock()
            mock_row.get.return_value = "SZ"
            mock_df.iloc = [mock_row]
            mock_ak.return_value = mock_df

            mock_repo = MagicMock()
            MockRepo.return_value = mock_repo

            task = CollectionTask(
                task_type="stock_single",
                task_params={"stock_code": "000001"},
            )
            result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 1
        assert result.success_count == 1

    def test_should_fail_stock_single_when_not_found(self) -> None:
        with patch(
            "data_collector.scripts.stock_full.ak.stock_info_a_code_name"
        ) as mock_ak:
            mock_df = MagicMock()
            mock_df.__getitem__ = MagicMock(return_value=mock_df)
            mock_df.empty = True
            mock_ak.return_value = mock_df

            task = CollectionTask(
                task_type="stock_single",
                task_params={"stock_code": "000001"},
            )
            result = self.executor.execute(task)

        assert result.status == "failed"
        assert result.fail_count == 1

    def test_should_execute_company_single(self) -> None:
        with (
            patch(
                "data_collector.scripts.company_full.fetch_company_for_stock"
            ) as mock_fetch,
            patch("data_collector.adapters.db_company_repository.DbCompanyRepository") as MockCompanyRepo,
            patch("data_collector.adapters.db_stock_repository.DbStockRepository") as MockStockRepo,
        ):
            from data_collector.core.domain.company import Company

            mock_company = Company(id="C001", name="测试公司", unified_social_credit_code="USC001")
            mock_fetch.return_value = mock_company

            task = CollectionTask(
                task_type="company_single",
                task_params={"stock_code": "000001"},
            )
            result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 1
        assert result.success_count == 1

    def test_should_fail_company_single_when_not_found(self) -> None:
        with patch(
            "data_collector.scripts.company_full.fetch_company_for_stock",
            return_value=None,
        ):
            task = CollectionTask(
                task_type="company_single",
                task_params={"stock_code": "NOTFOUND"},
            )
            result = self.executor.execute(task)

        assert result.status == "failed"
        assert result.fail_count == 1

    def test_should_fail_on_unknown_task_type(self) -> None:
        task = CollectionTask(task_type="unknown_type")
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert "未知的任务类型" in (result.error_message or "")

    def test_should_mark_running_then_success(self) -> None:
        with patch(
            "data_collector.task_executor.run_stock_full",
            return_value={"total": 1, "success": 1, "failed": 0},
        ):
            task = CollectionTask(task_type="stock_full")
            result = self.executor.execute(task)

        assert result.started_at is not None
        assert result.completed_at is not None
        assert result.id is not None
