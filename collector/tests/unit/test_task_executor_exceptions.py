"""任务执行器异常分支单元测试。"""

from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from data_collector.core.domain.collection_task import CollectionTask, TaskStatus
from data_collector.task_executor import TaskExecutor


class TestTaskExecutorExceptions:
    """TaskExecutor 异常路径测试。"""

    def test_execute_unknown_task_type(self):
        """未知任务类型应标记为失败。"""
        executor = TaskExecutor()
        task = CollectionTask(
            id="test-id",
            task_type="unknown_type",
            task_params={},
        )

        result = executor.execute(task)

        assert result.status == TaskStatus.FAILED.value
        assert "未知的任务类型" in result.error_message
        assert result.completed_at is not None

    def test_execute_stock_single_missing_stock_code(self):
        """stock_single 缺少 stock_code 应抛出 ValueError。"""
        executor = TaskExecutor()
        task = CollectionTask(
            id="test-id",
            task_type="stock_single",
            mode="single",
            task_params={},
        )

        result = executor.execute(task)

        assert result.status == TaskStatus.FAILED.value
        assert "stock_single 任务需要提供 task_params.stock_code" in result.error_message

    def test_execute_company_single_missing_stock_code(self):
        """company_single 缺少 stock_code 应抛出 ValueError。"""
        executor = TaskExecutor()
        task = CollectionTask(
            id="test-id",
            task_type="company_single",
            mode="single",
            task_params={},
        )

        result = executor.execute(task)

        assert result.status == TaskStatus.FAILED.value
        assert "company_single 任务需要提供 task_params.stock_code" in result.error_message

    def test_execute_sets_running_and_timestamps(self):
        """执行前应设置状态为 RUNNING 并记录 started_at。"""
        executor = TaskExecutor()
        task = CollectionTask(
            id="test-id",
            task_type="unknown_type",
            task_params={},
        )

        result = executor.execute(task)

        assert result.started_at is not None
        assert result.completed_at is not None
        assert result.started_at <= result.completed_at

    def test_execute_generates_id_if_missing(self):
        """任务 id 为空时应自动生成 ULID。"""
        executor = TaskExecutor()
        task = CollectionTask(
            task_type="unknown_type",
            task_params={},
        )

        result = executor.execute(task)

        assert result.id is not None
        assert len(result.id) > 0
