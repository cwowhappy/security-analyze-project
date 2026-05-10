"""新浪数据源模拟实现（示例）。"""

from typing import Any

import structlog

from stock_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus

logger = structlog.get_logger(__name__)


class DummySinaSource(DataSource):
    """新浪数据源（模拟实现，作为备用）。"""

    @property
    def name(self) -> str:
        return "sina"

    @property
    def priority(self) -> int:
        return 2

    def fetch(self, symbol: str) -> dict[str, Any]:
        logger.debug("新浪采集成功", symbol=symbol)
        return {
            "source": self.name,
            "symbol": symbol,
            "price": 10.48,
            "change": 1.18,
        }

    def check_health(self) -> SourceHealth:
        return SourceHealth(
            status=SourceStatus.HEALTHY,
            latency_ms=200.0,
            error_rate=0.02,
            last_check="2026-05-10T10:00:00Z",
        )
