"""股票采集用例。"""

from decimal import Decimal

import structlog

from stock_collector.core.domain.stock import Stock
from stock_collector.core.ports.stock_repository import StockRepository

logger = structlog.get_logger(__name__)


class CollectStockUseCase:
    """采集股票数据用例。"""

    def __init__(self, repository: StockRepository) -> None:
        self._repository = repository

    def execute(
        self,
        symbol: str,
        name: str,
        market: str,
        current_price: Decimal | None = None,
        change_percent: Decimal | None = None,
    ) -> Stock:
        """执行股票数据采集。

        Args:
            symbol: 股票代码。
            name: 股票名称。
            market: 市场。
            current_price: 当前价格。
            change_percent: 涨跌幅。

        Returns:
            采集后的股票实体。
        """
        logger.info(
            "开始采集股票数据",
            symbol=symbol,
            name=name,
            market=market,
        )

        stock = Stock(
            symbol=symbol,
            name=name,
            market=market,
            current_price=current_price,
            change_percent=change_percent,
        )

        self._repository.save(stock)

        logger.info(
            "股票数据采集完成",
            symbol=symbol,
            current_price=str(current_price) if current_price else None,
        )

        return stock
