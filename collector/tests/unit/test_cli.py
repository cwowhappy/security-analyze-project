"""CLI 模块单元测试。"""

from unittest.mock import MagicMock, patch

import pytest

from data_collector.cli import build_parser, main, run_api


class TestBuildParser:
    """参数解析器测试。"""

    def test_should_parse_stock_full_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["stock", "--full"])
        assert args.command == "stock"
        assert args.full is True
        assert args.code is None
        assert args.source is None

    def test_should_parse_stock_code_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["stock", "--code", "000001"])
        assert args.command == "stock"
        assert args.code == "000001"
        assert args.full is False

    def test_should_parse_company_full_with_source(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["company", "--full", "--source", "akshare"])
        assert args.command == "company"
        assert args.full is True
        assert args.source == "akshare"

    def test_should_parse_api_command(self) -> None:
        parser = build_parser()
        args = parser.parse_args(["api", "--port", "9000"])
        assert args.command == "api"
        assert args.port == 9000
        assert args.host == "0.0.0.0"

    def test_should_reject_mutually_exclusive_modes(self) -> None:
        parser = build_parser()
        with pytest.raises(SystemExit):
            parser.parse_args(["stock", "--full", "--code", "000001"])


class TestRunApi:
    """API 模式测试。"""

    def test_should_run_uvicorn_with_defaults(self) -> None:
        with patch("uvicorn.run") as mock_run:
            parser = build_parser()
            args = parser.parse_args(["api"])
            run_api(args)

        mock_run.assert_called_once()
        call_kwargs = mock_run.call_args[1]
        assert call_kwargs["host"] == "0.0.0.0"
        assert call_kwargs["port"] == 8000

    def test_should_run_uvicorn_with_custom_port(self) -> None:
        with patch("uvicorn.run") as mock_run:
            parser = build_parser()
            args = parser.parse_args(["api", "--port", "9000", "--host", "127.0.0.1"])
            run_api(args)

        call_kwargs = mock_run.call_args[1]
        assert call_kwargs["port"] == 9000
        assert call_kwargs["host"] == "127.0.0.1"


class TestMain:
    """主入口测试。"""

    def test_should_print_help_when_no_command(self, capsys) -> None:
        result = main([])
        assert result == 1
        captured = capsys.readouterr()
        assert "data-collector" in captured.out or "data-collector" in captured.err

    def test_should_execute_stock_full(self) -> None:
        with patch("data_collector.cli.init_pool") as mock_init, \
             patch("data_collector.cli.close_pool") as mock_close, \
             patch("data_collector.cli.DbStockRepository"), \
             patch("data_collector.cli.DbCompanyRepository"), \
             patch("data_collector.cli.DbCollectionTaskRepository") as MockTaskRepo, \
             patch("data_collector.cli.AkshareDataSource") as MockAkshare, \
             patch("data_collector.cli.TushareDataSource") as MockTushare, \
             patch("data_collector.cli.TaskExecutor") as MockExecutor:

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

            mock_source = MagicMock()
            mock_source.priority = 1
            MockAkshare.return_value = mock_source
            mock_tushare = MagicMock()
            mock_tushare.priority = 2
            MockTushare.return_value = mock_tushare

            result = main(["stock", "--full"])

            assert result == 0
            mock_init.assert_called_once()
            mock_executor.execute.assert_called_once()
            mock_task_repo.save.assert_called_once()
            mock_close.assert_called_once()

    def test_should_execute_company_single(self) -> None:
        with patch("data_collector.cli.init_pool"), \
             patch("data_collector.cli.close_pool"), \
             patch("data_collector.cli.DbStockRepository"), \
             patch("data_collector.cli.DbCompanyRepository"), \
             patch("data_collector.cli.DbCollectionTaskRepository"), \
             patch("data_collector.cli.AkshareDataSource") as MockAkshare, \
             patch("data_collector.cli.TushareDataSource") as MockTushare, \
             patch("data_collector.cli.TaskExecutor") as MockExecutor:

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

            mock_source = MagicMock()
            mock_source.priority = 1
            MockAkshare.return_value = mock_source
            mock_tushare = MagicMock()
            mock_tushare.priority = 2
            MockTushare.return_value = mock_tushare

            result = main(["company", "--code", "000001", "--source", "akshare"])

            assert result == 0
            executed_task = mock_executor.execute.call_args[0][0]
            assert executed_task.task_type == "company_single"
            assert executed_task.task_params["stock_code"] == "000001"
            assert executed_task.data_source == "akshare"

    def test_should_exit_with_error_on_failure(self) -> None:
        with patch("data_collector.cli.init_pool"), \
             patch("data_collector.cli.close_pool"), \
             patch("data_collector.cli.DbStockRepository"), \
             patch("data_collector.cli.DbCompanyRepository"), \
             patch("data_collector.cli.DbCollectionTaskRepository"), \
             patch("data_collector.cli.AkshareDataSource") as MockAkshare, \
             patch("data_collector.cli.TushareDataSource") as MockTushare, \
             patch("data_collector.cli.TaskExecutor") as MockExecutor:

            mock_executor = MagicMock()
            mock_result = MagicMock()
            mock_result.status = "failed"
            mock_result.error_message = "Something went wrong"
            mock_executor.execute.return_value = mock_result
            MockExecutor.return_value = mock_executor

            mock_source = MagicMock()
            mock_source.priority = 1
            MockAkshare.return_value = mock_source
            mock_tushare = MagicMock()
            mock_tushare.priority = 2
            MockTushare.return_value = mock_tushare

            result = main(["stock", "--full"])

            assert result == 1
