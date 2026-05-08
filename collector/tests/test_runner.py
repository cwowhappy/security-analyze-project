"""Test TaskRunner."""
from unittest.mock import MagicMock, patch, ANY
import pytest

from collector.runner import TaskRunner
from collector.tasks.base import BaseTask, TaskResult


class DummyTask(BaseTask):
    task_name = "dummy"
    data_type = "dummy_data"

    def run_full(self, **kwargs):
        return TaskResult(created=1, rows=1)

    def run_partial(self, identifiers, **kwargs):
        return TaskResult(created=len(identifiers), rows=len(identifiers))

    def run_incremental(self, **kwargs):
        return TaskResult(updated=1, rows=1)


class TestTaskRunner:
    @patch("collector.runner.PostgresDB")
    @patch("collector.runner.AkshareSource")
    @patch("collector.runner.Monitor")
    def test_init_creates_dependencies(self, mock_monitor_cls, mock_source_cls, mock_db_cls):
        runner = TaskRunner()
        mock_db_cls.assert_called_once()
        mock_source_cls.assert_called_once()
        mock_monitor_cls.assert_called_once()

    @patch("collector.runner.PostgresDB")
    @patch("collector.runner.AkshareSource")
    @patch("collector.runner.Monitor")
    def test_run_instantiates_and_executes_task(self, mock_monitor_cls, mock_source_cls, mock_db_cls):
        runner = TaskRunner()
        result = runner.run(DummyTask, mode="full")
        assert result.created == 1
        assert result.rows == 1

    @patch("collector.runner.PostgresDB")
    @patch("collector.runner.AkshareSource")
    @patch("collector.runner.Monitor")
    def test_run_passes_kwargs(self, mock_monitor_cls, mock_source_cls, mock_db_cls):
        runner = TaskRunner()
        result = runner.run(DummyTask, mode="partial", identifiers=["a", "b"])
        assert result.created == 2

    @patch("collector.runner.PostgresDB")
    @patch("collector.runner.AkshareSource")
    @patch("collector.runner.Monitor")
    def test_close_calls_db_close(self, mock_monitor_cls, mock_source_cls, mock_db_cls):
        runner = TaskRunner()
        runner.close()
        runner.db.close.assert_called_once()
