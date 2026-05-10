"""日志配置单元测试。"""

import logging

from data_collector.infrastructure.logging.config import configure_logging


class TestLoggingConfig:
    """日志配置测试。"""

    def test_should_configure_root_logger(self) -> None:
        configure_logging(log_level="DEBUG", json_format=False)
        root = logging.getLogger()
        assert root.level == logging.DEBUG
        assert len(root.handlers) > 0

    def test_should_set_urllib3_warning_level(self) -> None:
        configure_logging(log_level="INFO", json_format=False)
        assert logging.getLogger("urllib3").level == logging.WARNING
        assert logging.getLogger("requests").level == logging.WARNING

    def test_should_configure_json_format(self) -> None:
        # 不报错即成功
        configure_logging(log_level="INFO", json_format=True)
        root = logging.getLogger()
        assert root.level == logging.INFO
