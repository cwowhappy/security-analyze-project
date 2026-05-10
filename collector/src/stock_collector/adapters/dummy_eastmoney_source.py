"""东方财富数据源模拟实现（示例）。"""

from typing import Any

import structlog

from stock_collector.core.domain.data_source_error import SourceRateLimitError
from stock_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus

logger = structlog.get_logger(__name__)


class DummyEastmoneySource(DataSource):
    """东方财富数据源（模拟实现）。"""

    _call_count = 0

    @property
    def name(self) -> str:
        return "eastmoney"

    @property
    def priority(self) -> int:
        return 1

    def fetch(self, symbol: str) -> dict[str, Any]:
        DummyEastmoneySource._call_count += 1
        # 模拟偶数次触发限流
        if DummyEastmoneySource._call_count % 4 == 0:
            logger.warning("东方财富限流触发", symbol=symbol)
            raise SourceRateLimitError(self.name)

        logger.debug("东方财富采集成功", symbol=symbol)
        return {
            "source": self.name,
            "symbol": symbol,
            "price": 10.5,
            "change": 1.2,
        }

    def check_health(self) -> SourceHealth:
        return SourceHealth(
            status=SourceStatus.HEALTHY,
            latency_ms=120.0,
            error_rate=0.05,
            last_check="2026-05-10T10:00:00Z",
        )
