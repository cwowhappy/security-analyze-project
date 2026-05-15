"""财务全量采集编排器单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.core.pipeline.financial_full_orchestrator import (
    FinancialFullOrchestrator,
)
from data_collector.task_executor import TaskExecutor


class TestFinancialFullOrchestrator:
    """FinancialFullOrchestrator 测试。"""

    def test_run_full_mode_executes_all_four_subtasks(self) -> None:
        """full 模式下应顺序执行 4 个子任务并汇总结果。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            task.total_count = 10
            task.success_count = 10
            task.fail_count = 0
            task.status = "success"
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="full")
        result = orchestrator.run(None, parent_task, Settings())

        assert result["income"] == {"total": 10, "success": 10, "failed": 0}
        assert result["balance"] == {"total": 10, "success": 10, "failed": 0}
        assert result["cashflow"] == {"total": 10, "success": 10, "failed": 0}
        assert result["indicator"] == {"total": 10, "success": 10, "failed": 0}

        assert mock_executor.execute.call_count == 4
        call_types = [call.args[0].task_type for call in mock_executor.execute.call_args_list]
        assert call_types == [
            "financial_income",
            "financial_balance",
            "financial_cashflow",
            "financial_indicator",
        ]

    def test_run_single_mode_success_executes_all_subtasks(self) -> None:
        """single 模式下全部成功时应执行指标计算。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            task.total_count = 1
            task.success_count = 1
            task.fail_count = 0
            task.status = "success"
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="single")
        result = orchestrator.run("000001", parent_task, Settings())

        assert result["indicator"] == {"total": 1, "success": 1, "failed": 0}
        assert mock_executor.execute.call_count == 4

    def test_run_single_mode_skips_indicator_when_income_fails(self) -> None:
        """single 模式下利润表失败时应跳过指标计算。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            if task.task_type == "financial_income":
                task.status = "failed"
                task.total_count = 1
                task.success_count = 0
                task.fail_count = 1
            else:
                task.status = "success"
                task.total_count = 1
                task.success_count = 1
                task.fail_count = 0
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="single")
        result = orchestrator.run("000001", parent_task, Settings())

        assert result["income"] == {"total": 1, "success": 0, "failed": 1}
        assert result["indicator"] == {"total": 0, "success": 0, "failed": 0}
        assert mock_executor.execute.call_count == 3

    def test_run_single_mode_skips_indicator_when_balance_fails(self) -> None:
        """single 模式下资产负债表失败时应跳过指标计算。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            if task.task_type == "financial_balance":
                task.status = "failed"
                task.total_count = 1
                task.success_count = 0
                task.fail_count = 1
            else:
                task.status = "success"
                task.total_count = 1
                task.success_count = 1
                task.fail_count = 0
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="single")
        result = orchestrator.run("000001", parent_task, Settings())

        assert result["balance"] == {"total": 1, "success": 0, "failed": 1}
        assert result["indicator"] == {"total": 0, "success": 0, "failed": 0}
        assert mock_executor.execute.call_count == 3

    def test_run_single_mode_skips_indicator_when_cashflow_fails(self) -> None:
        """single 模式下现金流量表失败时应跳过指标计算。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            if task.task_type == "financial_cashflow":
                task.status = "failed"
                task.total_count = 1
                task.success_count = 0
                task.fail_count = 1
            else:
                task.status = "success"
                task.total_count = 1
                task.success_count = 1
                task.fail_count = 0
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="single")
        result = orchestrator.run("000001", parent_task, Settings())

        assert result["cashflow"] == {"total": 1, "success": 0, "failed": 1}
        assert result["indicator"] == {"total": 0, "success": 0, "failed": 0}
        assert mock_executor.execute.call_count == 3

    def test_run_full_mode_runs_indicator_even_if_subtask_fails(self) -> None:
        """full 模式下即使子任务失败也应执行指标计算（无法按股票维度精确跳过）。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            if task.task_type == "financial_income":
                task.status = "failed"
                task.total_count = 10
                task.success_count = 5
                task.fail_count = 5
            else:
                task.status = "success"
                task.total_count = 10
                task.success_count = 10
                task.fail_count = 0
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="full")
        result = orchestrator.run(None, parent_task, Settings())

        assert result["income"] == {"total": 10, "success": 5, "failed": 5}
        assert result["indicator"] == {"total": 10, "success": 10, "failed": 0}
        assert mock_executor.execute.call_count == 4

    def test_run_passes_stock_code_to_subtasks(self) -> None:
        """应将 stock_code 传递到子任务参数中。"""
        mock_executor = MagicMock(spec=TaskExecutor)

        def mock_execute(task: CollectionTask) -> CollectionTask:
            task.status = "success"
            task.total_count = 1
            task.success_count = 1
            task.fail_count = 0
            return task

        mock_executor.execute.side_effect = mock_execute

        orchestrator = FinancialFullOrchestrator(executor=mock_executor)
        parent_task = CollectionTask(task_type="financial_full", mode="single")
        orchestrator.run("000001", parent_task, Settings())

        for call in mock_executor.execute.call_args_list:
            sub_task = call.args[0]
            assert sub_task.task_params.get("stock_code") == "000001"
