"""股票领域模型单元测试。"""

import pytest

from stock_collector.core.domain.stock import Stock


class TestStock:
    """股票领域模型测试。"""

    def test_should_create_stock_with_valid_data(self) -> None:
        stock = Stock(symbol="000001", name="平安银行", market="SZ")
        assert stock.symbol == "000001"
        assert stock.name == "平安银行"
        assert stock.market == "SZ"

    def test_should_raise_error_when_symbol_empty(self) -> None:
        with pytest.raises(ValueError, match="股票代码不能为空"):
            Stock(symbol="", name="平安银行", market="SZ")

    def test_should_raise_error_when_name_empty(self) -> None:
        with pytest.raises(ValueError, match="股票名称不能为空"):
            Stock(symbol="000001", name="", market="SZ")
