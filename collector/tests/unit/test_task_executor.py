"""任务执行器单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.task_executor import (
    BatchFailureThresholdExceeded,
    TaskExecutor,
    _TASK_REGISTRY,
)


class TestTaskExecutor:
    """TaskExecutor 测试。"""

    def setup_method(self) -> None:
        self.executor = TaskExecutor(settings=Settings())
        # 备份原始注册表，防止测试污染全局状态
        self._original_registry = dict(_TASK_REGISTRY)

    def teardown_method(self) -> None:
        # 恢复原始注册表
        _TASK_REGISTRY.clear()
        _TASK_REGISTRY.update(self._original_registry)

    def test_should_execute_stock_full(self) -> None:
        mock_handler = MagicMock(return_value={"total": 5, "success": 5, "failed": 0})
        _TASK_REGISTRY[("stock_full", "full")] = (mock_handler, "akshare")

        task = CollectionTask(task_type="stock_full")
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.task_type == "stock_full"
        assert result.total_count == 5
        assert result.success_count == 5
        assert result.fail_count == 0
        assert result.data_source == "akshare"
        mock_handler.assert_called_once()

    def test_should_execute_company_full(self) -> None:
        mock_handler = MagicMock(return_value={"total": 3, "success": 3, "failed": 0})
        _TASK_REGISTRY[("company_full", "full")] = (mock_handler, "akshare")

        task = CollectionTask(task_type="company_full")
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 3
        assert result.success_count == 3
        assert result.fail_count == 0
        assert result.data_source == "akshare"

    def test_should_execute_field_supplement(self) -> None:
        mock_handler = MagicMock(
            return_value={
                "stock_total": 10,
                "stock_success": 9,
                "stock_failed": 1,
                "company_total": 5,
                "company_success": 5,
                "company_failed": 0,
            }
        )
        _TASK_REGISTRY[("field_supplement", "full")] = (mock_handler, "tushare")

        task = CollectionTask(task_type="field_supplement")
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 15
        assert result.success_count == 14
        assert result.fail_count == 1
        assert result.data_source == "tushare"

    def test_should_execute_stock_single(self) -> None:
        mock_handler = MagicMock(return_value={"total": 1, "success": 1, "failed": 0})
        _TASK_REGISTRY[("stock_single", "single")] = (mock_handler, "akshare")

        task = CollectionTask(
            task_type="stock_single",
            mode="single",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 1
        assert result.success_count == 1

    def test_should_fail_stock_single_when_not_found(self) -> None:
        mock_handler = MagicMock(
            side_effect=ValueError("未找到股票: 000001")
        )
        _TASK_REGISTRY[("stock_single", "single")] = (mock_handler, "akshare")

        task = CollectionTask(
            task_type="stock_single",
            mode="single",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert "未找到股票" in (result.error_message or "")

    def test_should_execute_company_single(self) -> None:
        mock_handler = MagicMock(return_value={"total": 1, "success": 1, "failed": 0})
        _TASK_REGISTRY[("company_single", "single")] = (mock_handler, "akshare")

        task = CollectionTask(
            task_type="company_single",
            mode="single",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 1
        assert result.success_count == 1

    def test_should_fail_company_single_when_not_found(self) -> None:
        mock_handler = MagicMock(
            side_effect=ValueError("未找到公司信息: NOTFOUND")
        )
        _TASK_REGISTRY[("company_single", "single")] = (mock_handler, "akshare")

        task = CollectionTask(
            task_type="company_single",
            mode="single",
            task_params={"stock_code": "NOTFOUND"},
        )
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert "未找到公司信息" in (result.error_message or "")

    def test_should_fail_on_unknown_task_type(self) -> None:
        task = CollectionTask(task_type="unknown_type")
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert "未知的任务类型" in (result.error_message or "")

    def test_should_mark_running_then_success(self) -> None:
        mock_handler = MagicMock(return_value={"total": 1, "success": 1, "failed": 0})
        _TASK_REGISTRY[("stock_full", "full")] = (mock_handler, "akshare")

        task = CollectionTask(task_type="stock_full")
        result = self.executor.execute(task)

        assert result.started_at is not None
        assert result.completed_at is not None
        assert result.id is not None

    def test_should_fail_when_batch_failure_threshold_exceeded(self) -> None:
        """当批次失败率超过阈值时，任务应标记为失败。"""
        mock_handler = MagicMock(return_value={"total": 10, "success": 5, "failed": 5})
        _TASK_REGISTRY[("stock_full", "full")] = (mock_handler, "akshare")

        # 默认阈值为 0.1，失败率 50% 应触发熔断
        task = CollectionTask(task_type="stock_full")
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert "批次失败率" in (result.error_message or "")
        assert "超过阈值" in (result.error_message or "")

    def test_should_not_fail_when_threshold_not_exceeded(self) -> None:
        """当批次失败率未超过阈值时，任务应正常成功。"""
        mock_handler = MagicMock(return_value={"total": 10, "success": 9, "failed": 1})
        _TASK_REGISTRY[("stock_full", "full")] = (mock_handler, "akshare")

        task = CollectionTask(task_type="stock_full")
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.fail_count == 1

    def test_should_execute_financial_full(self) -> None:
        mock_handler = MagicMock(
            return_value={
                "income": {"total": 10, "success": 10, "failed": 0},
                "balance": {"total": 10, "success": 10, "failed": 0},
                "cashflow": {"total": 10, "success": 10, "failed": 0},
                "indicator": {"total": 10, "success": 10, "failed": 0},
            }
        )
        _TASK_REGISTRY[("financial_full", "full")] = (mock_handler, "akshare")

        task = CollectionTask(
            task_type="financial_full",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 40
        assert result.success_count == 40
        assert result.fail_count == 0


def test_executor_routes_by_task_type_and_mode():
    from data_collector.task_executor import TaskExecutor
    from data_collector.core.domain.collection_task import CollectionTask
    task = CollectionTask(task_type="stock_basic", mode="single", source_priority=["akshare"])
    executor = TaskExecutor()
    # 此时 registry 中应找不到 handler，因为尚未注册
    result = executor.execute(task)
    assert result.status == "failed"
    assert "未知的任务类型" in (result.error_message or "")
    assert "(mode=single)" in (result.error_message or "")
