"""核心抽象接口。"""

from stock_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus
from stock_collector.core.ports.stock_repository import StockRepository

__all__ = ["DataSource", "SourceHealth", "SourceStatus", "StockRepository"]
