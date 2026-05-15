from unittest.mock import Mock, patch

import pandas as pd
import pytest

from data_collector.adapters.stock_basic_tushare_adapter import (
    StockBasicTushareAdapter,
    _init_tushare,
)
from data_collector.core.config.field_mapping_config import SourceConfig


class TestStockBasicTushareAdapter:
    @patch("data_collector.adapters.stock_basic_tushare_adapter._init_tushare")
    def test_fetch_from_cache(self, mock_init):
        pro = Mock()
        mock_init.return_value = pro

        df_basic = pd.DataFrame(
            [
                {
                    "ts_code": "000001.SZ",
                    "symbol": "000001",
                    "name": "平安银行",
                    "area": "深圳",
                    "industry": "银行",
                    "fullname": "平安银行股份有限公司",
                    "market": "主板",
                    "exchange": "SZSE",
                    "list_date": "1991-04-03",
                }
            ]
        )
        df_trade = pd.DataFrame([{"cal_date": "20240101", "is_open": 1}])
        df_cap = pd.DataFrame(
            [
                {
                    "ts_code": "000001.SZ",
                    "total_share": 19405.92,
                    "float_share": 19405.00,
                }
            ]
        )

        pro.stock_basic.return_value = df_basic
        pro.trade_cal.return_value = df_trade
        pro.daily_basic.return_value = df_cap

        adapter = StockBasicTushareAdapter()
        source = SourceConfig(
            name="tushare", adapter="stock_basic_tushare_adapter", priority=2
        )
        result = adapter.fetch("000001", source)

        assert result["ts_code"] == "000001.SZ"
        assert result["fullname"] == "平安银行股份有限公司"
        assert result["total_share"] == 19405.92
        assert result["float_share"] == 19405.00

        # 第二次 fetch 应直接走缓存，不再调用 API
        result2 = adapter.fetch("000001", source)
        assert result2 == result
        pro.stock_basic.assert_called_once()

    @patch("data_collector.adapters.stock_basic_tushare_adapter._init_tushare")
    def test_fetch_not_found(self, mock_init):
        pro = Mock()
        mock_init.return_value = pro
        pro.stock_basic.return_value = pd.DataFrame(
            [{"ts_code": "000001.SZ", "symbol": "000001", "name": "平安银行"}]
        )
        pro.trade_cal.return_value = pd.DataFrame()
        pro.daily_basic.return_value = pd.DataFrame()

        adapter = StockBasicTushareAdapter()
        source = SourceConfig(
            name="tushare", adapter="stock_basic_tushare_adapter", priority=2
        )
        result = adapter.fetch("999999", source)
        assert result == {}

    @patch("data_collector.adapters.stock_basic_tushare_adapter._init_tushare")
    def test_fetch_empty_basic(self, mock_init):
        pro = Mock()
        mock_init.return_value = pro
        pro.stock_basic.return_value = pd.DataFrame()

        adapter = StockBasicTushareAdapter()
        source = SourceConfig(
            name="tushare", adapter="stock_basic_tushare_adapter", priority=2
        )
        result = adapter.fetch("000001", source)
        assert result == {}

    def test_init_tushare_no_token(self):
        with patch(
            "data_collector.config.Settings"
        ) as MockSettings:
            MockSettings.return_value.tushare_token = ""
            with pytest.raises(RuntimeError, match="TUSHARE_TOKEN"):
                _init_tushare()
