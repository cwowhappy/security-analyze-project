"""CLI 模块单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector.cli import build_parser, main


class TestBuildParser:
    """参数解析器测试。"""

    def test_should_parse_stock_full_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["stock", "--full"])
        assert args.command == "stock"
        assert args.full is True
        assert args.code is None

    def test_should_parse_stock_code_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["stock", "--code", "000001"])
        assert args.command == "stock"
        assert args.code == "000001"
        assert args.full is False

    def test_should_parse_company_full_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["company", "--full"])
        assert args.command == "company"
        assert args.full is True

    def test_should_parse_supplement_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["supplement", "--full"])
        assert args.command == "supplement"
        assert args.full is True

    def test_should_reject_mutually_exclusive_modes(self) -> None:
        parser = build_parser()
        with pytest.raises(SystemExit):
            parser.parse_args(["stock", "--full", "--code", "000001"])


class TestMain:
    """主入口测试。"""

    def test_should_print_help_when_no_command(self, capsys) -> None:
        result = main([])
        assert result == 1
        captured = capsys.readouterr()
        assert "data-collector" in captured.out or "data-collector" in captured.err

    def test_should_execute_stock_full(self) -> None:
        with (
            patch("data_collector.cli.init_pool") as mock_init,
            patch("data_collector.cli.close_pool") as mock_close,
            patch("data_collector.cli.DbCollectionTaskRepository") as MockTaskRepo,
            patch("data_collector.cli.TaskExecutor") as MockExecutor,
        ):
            mock_executor = MagicMock()
            mock_result = MagicMock()
            mock_result.status = "success"
            mock_result.task_type = "stock_full"
            mock_result.total_count = 10
            mock_result.success_count = 10
            mock_result.fail_count = 0
            mock_result.error_message = None
            mock_executor.execute.return_value = mock_result
            MockExecutor.return_value = mock_executor

            mock_task_repo = MagicMock()
            MockTaskRepo.return_value = mock_task_repo

            result = main(["stock", "--full"])

            assert result == 0
            mock_init.assert_called_once()
            mock_executor.execute.assert_called_once()
            mock_task_repo.save.assert_called_once()
            mock_close.assert_called_once()

    def test_should_execute_company_single(self) -> None:
        with (
            patch("data_collector.cli.init_pool"),
            patch("data_collector.cli.close_pool"),
            patch("data_collector.cli.DbCollectionTaskRepository"),
            patch("data_collector.cli.TaskExecutor") as MockExecutor,
        ):
            mock_executor = MagicMock()
            mock_result = MagicMock()
            mock_result.status = "success"
            mock_result.task_type = "company_single"
            mock_result.total_count = 1
            mock_result.success_count = 1
            mock_result.fail_count = 0
            mock_result.error_message = None
            mock_executor.execute.return_value = mock_result
            MockExecutor.return_value = mock_executor

            result = main(["company", "--code", "000001"])

            assert result == 0
            executed_task = mock_executor.execute.call_args[0][0]
            assert executed_task.task_type == "company_single"
            assert executed_task.task_params["stock_code"] == "000001"

    def test_should_exit_with_error_on_failure(self) -> None:
        with (
            patch("data_collector.cli.init_pool"),
            patch("data_collector.cli.close_pool"),
            patch("data_collector.cli.DbCollectionTaskRepository"),
            patch("data_collector.cli.TaskExecutor") as MockExecutor,
        ):
            mock_executor = MagicMock()
            mock_result = MagicMock()
            mock_result.status = "failed"
            mock_result.error_message = "Something went wrong"
            mock_executor.execute.return_value = mock_result
            MockExecutor.return_value = mock_executor

            result = main(["stock", "--full"])

            assert result == 1
