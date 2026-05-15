from unittest.mock import Mock, patch
import pandas as pd

from data_collector.adapters.stock_basic_akshare_adapter import StockBasicAkshareAdapter
from data_collector.core.config.field_mapping_config import SourceConfig


class TestStockBasicAkshareAdapter:
    def test_fetch_single_stock(self):
        adapter = StockBasicAkshareAdapter()
        mock_df = pd.DataFrame([{"代码": "000001", "名称": "平安银行"}])
        with patch("akshare.stock_info_a_code_name", return_value=mock_df):
            result = adapter.fetch("000001", SourceConfig(name="akshare", adapter="", priority=1))
        assert result == {"代码": "000001", "名称": "平安银行"}
