"""股票仓库接口（Port）。"""

from abc import ABC, abstractmethod
from collections.abc import Sequence

from stock_collector.core.domain.stock import Stock


class StockRepository(ABC):
    """股票仓库抽象接口。"""

    @abstractmethod
    def save(self, stock: Stock) -> None:
        """保存股票数据。"""

    @abstractmethod
    def find_by_symbol(self, symbol: str) -> Stock | None:
        """根据股票代码查询。"""

    @abstractmethod
    def find_all(self) -> Sequence[Stock]:
        """查询所有股票。"""
