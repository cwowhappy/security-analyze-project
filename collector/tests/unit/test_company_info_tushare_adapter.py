from unittest.mock import Mock, patch

import pandas as pd

from data_collector.adapters.company_info_tushare_adapter import (
    CompanyInfoTushareAdapter,
    _guess_ts_code,
)
from data_collector.core.config.field_mapping_config import SourceConfig


class TestCompanyInfoTushareAdapter:
    @patch("data_collector.adapters.company_info_tushare_adapter._init_tushare")
    def test_fetch_success(self, mock_init):
        pro = Mock()
        mock_init.return_value = pro

        df = pd.DataFrame(
            [
                {
                    "ts_code": "000001.SZ",
                    "chairman": "谢永林",
                    "manager": "冀光恒",
                    "secretary": "周强",
                    "employees": 35324,
                }
            ]
        )
        pro.stock_company.return_value = df

        adapter = CompanyInfoTushareAdapter()
        source = SourceConfig(
            name="tushare", adapter="company_info_tushare_adapter", priority=2
        )
        result = adapter.fetch("000001", source)

        assert result["chairman"] == "谢永林"
        assert result["manager"] == "冀光恒"
        assert result["secretary"] == "周强"
        assert result["employees"] == 35324
        pro.stock_company.assert_called_once_with(
            ts_code="000001.SZ",
            fields="ts_code,exchange,chairman,manager,secretary,reg_capital,setup_date,employees,main_business",
        )

    @patch("data_collector.adapters.company_info_tushare_adapter._init_tushare")
    def test_fetch_empty(self, mock_init):
        pro = Mock()
        mock_init.return_value = pro
        pro.stock_company.return_value = pd.DataFrame()

        adapter = CompanyInfoTushareAdapter()
        source = SourceConfig(
            name="tushare", adapter="company_info_tushare_adapter", priority=2
        )
        result = adapter.fetch("000001", source)
        assert result == {}

    @patch("data_collector.adapters.company_info_tushare_adapter._init_tushare")
    def test_fetch_none(self, mock_init):
        pro = Mock()
        mock_init.return_value = pro
        pro.stock_company.return_value = None

        adapter = CompanyInfoTushareAdapter()
        source = SourceConfig(
            name="tushare", adapter="company_info_tushare_adapter", priority=2
        )
        result = adapter.fetch("000001", source)
        assert result == {}


class TestGuessTsCode:
    def test_sh(self):
        assert _guess_ts_code("600000") == "600000.SH"

    def test_sz_0(self):
        assert _guess_ts_code("000001") == "000001.SZ"

    def test_sz_3(self):
        assert _guess_ts_code("300001") == "300001.SZ"

    def test_bj_4(self):
        assert _guess_ts_code("430001") == "430001.BJ"

    def test_bj_8(self):
        assert _guess_ts_code("830001") == "830001.BJ"

    def test_bj_82(self):
        assert _guess_ts_code("820001") == "820001.BJ"

    def test_bj_92(self):
        assert _guess_ts_code("920001") == "920001.BJ"

    def test_unknown(self):
        assert _guess_ts_code("unknown") is None
