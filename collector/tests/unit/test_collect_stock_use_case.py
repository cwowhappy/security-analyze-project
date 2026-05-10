"""采集股票用例单元测试。"""

from decimal import Decimal

from stock_collector.adapters.memory_stock_repository import MemoryStockRepository
from stock_collector.core.use_cases.collect_stock import CollectStockUseCase


class TestCollectStockUseCase:
    """采集股票用例测试。"""

    def setup_method(self) -> None:
        self.repository = MemoryStockRepository()
        self.use_case = CollectStockUseCase(self.repository)

    def test_should_save_stock_when_execute(self) -> None:
        result = self.use_case.execute(
            symbol="000001",
            name="平安银行",
            market="SZ",
            current_price=Decimal("12.50"),
        )

        assert result.symbol == "000001"
        assert self.repository.find_by_symbol("000001") is not None

    def test_should_save_stock_without_price(self) -> None:
        result = self.use_case.execute(
            symbol="000002",
            name="万科A",
            market="SZ",
        )

        assert result.current_price is None
        assert self.repository.find_by_symbol("000002") is not None
