"""结构化日志配置。"""

import logging
import sys

import structlog


def configure_logging(log_level: str = "INFO", json_format: bool = False) -> None:
    """配置结构化日志。

    Args:
        log_level: 日志级别，默认为 INFO。
        json_format: 是否使用 JSON 格式输出，生产环境建议开启。
    """
    shared_processors = [
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso", utc=True),
        structlog.processors.StackInfoRenderer(),
        structlog.stdlib.ExtraAdder(),
    ]

    if json_format:
        formatter = structlog.processors.JSONRenderer()
    else:
        formatter = structlog.dev.ConsoleRenderer(colors=sys.stdout.isatty())

    structlog.configure(
        processors=shared_processors
        + [
            structlog.stdlib.ProcessorFormatter.wrap_for_formatter,
        ],
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )

    formatter_config = structlog.stdlib.ProcessorFormatter(
        foreign_pre_chain=shared_processors,
        processor=formatter,
    )

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter_config)

    root_logger = logging.getLogger()
    root_logger.handlers.clear()
    root_logger.addHandler(handler)
    root_logger.setLevel(getattr(logging, log_level.upper(), logging.INFO))

    # 设置第三方库的日志级别
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("requests").setLevel(logging.WARNING)
