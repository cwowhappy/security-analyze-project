"""数据源抽象接口（Phase 2 扩展版）。"""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum

from data_collector.core.domain.company import Company
from data_collector.core.domain.stock import Stock


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

    所有具体数据源（akshare、tushare 等）必须实现此接口。
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
    def fetch_stock_list(self) -> list[Stock]:
        """获取全量股票列表。

        Returns:
            股票领域实体列表。

        Raises:
            DataSourceError: 采集失败时抛出。
        """

    @abstractmethod
    def fetch_company_info(self, stock_code: str) -> Company | None:
        """获取单只股票对应的公司详情。

        Args:
            stock_code: 股票代码（如 000001）。

        Returns:
            公司领域实体，未找到时返回 None。

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
