"""Test BaseTask abstract base class and TaskResult."""
from abc import ABC
from unittest.mock import MagicMock, patch, ANY
import pytest

from collector.tasks.base import BaseTask, TaskResult


class TestTaskResult:
    def test_default_values(self):
        r = TaskResult()
        assert r.created == 0
        assert r.updated == 0
        assert r.failed == 0
        assert r.rows == 0

    def test_custom_values(self):
        r = TaskResult(created=5, updated=3, failed=1, rows=8)
        assert r.created == 5
        assert r.updated == 3
        assert r.failed == 1
        assert r.rows == 8

    def test_repr(self):
        r = TaskResult(created=1, updated=2, failed=0, rows=3)
        assert "created=1" in repr(r)
        assert "updated=2" in repr(r)


class ConcreteTask(BaseTask):
    task_name = "test_task"
    data_type = "test_data"

    def run_full(self, **kwargs):
        return TaskResult(created=10, updated=5, rows=15)

    def run_partial(self, identifiers, **kwargs):
        return TaskResult(created=len(identifiers), rows=len(identifiers))

    def run_incremental(self, **kwargs):
        return TaskResult(updated=3, rows=3)


class TestBaseTask:
    def _create_task(self, with_monitor=True):
        mock_db = MagicMock()
        mock_source = MagicMock()
        mock_monitor = MagicMock() if with_monitor else None
        return ConcreteTask(db=mock_db, source=mock_source, monitor=mock_monitor)

    def test_is_abstract_class(self):
        with pytest.raises(TypeError):
            BaseTask(db=MagicMock(), source=MagicMock())

    def test_subclass_must_implement_methods(self):
        class PartialTask(BaseTask):
            task_name = "partial"
            data_type = "partial"

        with pytest.raises(TypeError):
            PartialTask(db=MagicMock(), source=MagicMock())

    def test_execute_full_mode(self):
        task = self._create_task()
        result = task.execute(mode="full")
        assert result.created == 10
        assert result.updated == 5
        assert result.rows == 15
        task.monitor.log_task_start.assert_called_once_with("test_task", "test_data", session_id=None)
        task.monitor.log_task_end.assert_called_once()
        task.monitor.upsert_data_status.assert_called_once_with("test_data", 15, ANY)

    def test_execute_partial_mode(self):
        task = self._create_task()
        result = task.execute(mode="partial", identifiers=["600519", "000001"])
        assert result.created == 2
        assert result.rows == 2

    def test_execute_incremental_mode(self):
        task = self._create_task()
        result = task.execute(mode="incremental")
        assert result.updated == 3
        assert result.rows == 3

    def test_execute_resume_mode(self):
        """resume_session 应读取 monitor 的成功集合并传给 run_full"""
        task = self._create_task()
        task.monitor.get_session_progress.return_value = {"600519"}
        with patch.object(task, "run_full") as mock_run_full:
            mock_run_full.return_value = TaskResult(created=1, rows=1)
            result = task.execute(mode="resume", session_id="sess-123")
            mock_run_full.assert_called_once()
            call_kwargs = mock_run_full.call_args.kwargs
            assert call_kwargs["exclude_set"] == {"600519"}
            assert call_kwargs["session_id"] == "sess-123"

    def test_execute_unknown_mode_raises(self):
        task = self._create_task()
        with pytest.raises(ValueError, match="Unknown execution mode"):
            task.execute(mode="unknown")

    def test_execute_logs_task_end_on_failure(self):
        class FailingTask(BaseTask):
            task_name = "failing"
            data_type = "failing"

            def run_full(self, **kwargs):
                raise RuntimeError("boom")

            def run_partial(self, identifiers, **kwargs):
                return TaskResult()

            def run_incremental(self, **kwargs):
                return TaskResult()

        mock_monitor = MagicMock()
        task = FailingTask(db=MagicMock(), source=MagicMock(), monitor=mock_monitor)
        with pytest.raises(RuntimeError):
            task.execute(mode="full")
        mock_monitor.log_task_end.assert_called_once()
        call_args = mock_monitor.log_task_end.call_args
        assert call_args[0][1] == "failed"
        assert "boom" in call_args[1]["error_message"]

    def test_execute_without_monitor(self):
        task = self._create_task(with_monitor=False)
        result = task.execute(mode="full")
        assert result.created == 10
