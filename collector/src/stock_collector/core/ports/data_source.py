"""数据源抽象接口。"""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum
from typing import Any


class SourceStatus(Enum):
    """数据源状态。"""

    HEALTHY = "healthy"
    DEGRADED = "degraded"
    UNAVAILABLE = "unavailable"


@dataclass(frozen=True)
class SourceHealth:
    """数据源健康检查结果。"""

    status: SourceStatus
    latency_ms: float
    error_rate: float
    last_check: str


class DataSource(ABC):
    """数据源抽象基类。

    所有具体数据源（东方财富、同花顺、Tushare 等）必须实现此接口。
    """

    @property
    @abstractmethod
    def name(self) -> str:
        """数据源名称。"""

    @property
    @abstractmethod
    def priority(self) -> int:
        """数据源优先级，数值越小优先级越高。"""

    @abstractmethod
    def fetch(self, symbol: str) -> dict[str, Any]:
        """采集单只股票数据。

        Args:
            symbol: 股票代码。

        Returns:
            原始采集数据。

        Raises:
            DataSourceError: 采集失败时抛出。
        """

    @abstractmethod
    def check_health(self) -> SourceHealth:
        """检查数据源健康状态。"""

    def is_available(self) -> bool:
        """判断数据源是否可用。"""
        health = self.check_health()
        return health.status != SourceStatus.UNAVAILABLE
