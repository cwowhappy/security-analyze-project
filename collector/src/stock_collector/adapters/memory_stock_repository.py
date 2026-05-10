"""内存股票仓库实现（用于测试和开发）。"""

from collections.abc import Sequence

from stock_collector.core.domain.stock import Stock
from stock_collector.core.ports.stock_repository import StockRepository


class MemoryStockRepository(StockRepository):
    """内存股票仓库实现。"""

    def __init__(self) -> None:
        self._data: dict[str, Stock] = {}

    def save(self, stock: Stock) -> None:
        self._data[stock.symbol] = stock

    def find_by_symbol(self, symbol: str) -> Stock | None:
        return self._data.get(symbol)

    def find_all(self) -> Sequence[Stock]:
        return list(self._data.values())
