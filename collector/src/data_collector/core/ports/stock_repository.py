"""股票仓库接口（Port）。"""

from abc import ABC, abstractmethod
from collections.abc import Sequence

from data_collector.core.domain.stock import Stock


class StockRepository(ABC):
    """股票仓库抽象接口。"""

    @abstractmethod
    def save(self, stock: Stock) -> None:
        """保存或更新股票数据。"""

    @abstractmethod
    def save_all(self, stocks: Sequence[Stock]) -> tuple[int, int]:
        """批量保存股票，返回 (成功数, 失败数)。"""

    @abstractmethod
    def find_by_symbol(self, stock_code: str) -> Stock | None:
        """根据股票代码查询。"""

    @abstractmethod
    def find_all(self) -> Sequence[Stock]:
        """查询所有股票。"""

    @abstractmethod
    def count(self) -> int:
        """返回股票总数。"""
