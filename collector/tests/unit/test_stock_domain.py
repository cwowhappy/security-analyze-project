"""股票领域模型单元测试。"""

import pytest

from data_collector.core.domain.stock import Stock


class TestStock:
    """股票领域模型测试。"""

    def test_should_create_stock_with_valid_data(self) -> None:
        stock = Stock(stock_code="000001", name="平安银行", market="SZ")
        assert stock.stock_code == "000001"
        assert stock.name == "平安银行"
        assert stock.market == "SZ"

    def test_should_raise_error_when_stock_code_empty(self) -> None:
        with pytest.raises(ValueError, match="股票代码 stock_code 不能为空"):
            Stock(stock_code="", name="平安银行")

    def test_should_raise_error_when_name_empty(self) -> None:
        with pytest.raises(ValueError, match="股票名称 name 不能为空"):
            Stock(stock_code="000001", name="")

    def test_should_convert_to_dict_and_back(self) -> None:
        stock = Stock(
            stock_code="000001",
            name="平安银行",
            ts_code="000001.SZ",
            market="SZ",
            exchange="SZSE",
        )
        data = stock.to_dict()
        restored = Stock.from_dict(data)
        assert restored.stock_code == "000001"
        assert restored.name == "平安银行"
        assert restored.ts_code == "000001.SZ"
