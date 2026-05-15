from unittest.mock import patch

import pandas as pd
import pytest

from data_collector.adapters.financial_sina_adapter import FinancialSinaAdapter
from data_collector.core.config.field_mapping_config import SourceConfig


class TestFinancialSinaAdapter:
    def test_fetch_income_success(self):
        adapter = FinancialSinaAdapter()
        mock_df = pd.DataFrame(
            [
                {
                    "报告日": "2023-12-31",
                    "基本每股收益": "2.50",
                    "营业总收入": "1,000,000.00",
                },
                {
                    "报告日": "2023-09-30",
                    "基本每股收益": "1.80",
                    "营业总收入": "750,000.00",
                },
            ]
        )
        source = SourceConfig(
            name="akshare",
            adapter="financial_sina_adapter",
            priority=1,
            params={"symbol": "利润表"},
        )
        with patch("akshare.stock_financial_report_sina", return_value=mock_df):
            result = adapter.fetch("000001", source)

        assert len(result) == 2
        assert result[0]["报告日"] == "2023-12-31"
        assert result[0]["基本每股收益"] == "2.50"
        assert result[0]["营业总收入"] == "1,000,000.00"
        assert result[1]["报告日"] == "2023-09-30"

    def test_fetch_balance_success(self):
        adapter = FinancialSinaAdapter()
        mock_df = pd.DataFrame(
            [
                {
                    "报告期": "2023-12-31",
                    "资产总计": "500,000.00",
                    "负债合计": "200,000.00",
                }
            ]
        )
        source = SourceConfig(
            name="akshare",
            adapter="financial_sina_adapter",
            priority=1,
            params={"symbol": "资产负债表"},
        )
        with patch("akshare.stock_financial_report_sina", return_value=mock_df):
            result = adapter.fetch("000001", source)

        assert len(result) == 1
        assert result[0]["报告期"] == "2023-12-31"
        assert result[0]["资产总计"] == "500,000.00"

    def test_fetch_cashflow_success(self):
        adapter = FinancialSinaAdapter()
        mock_df = pd.DataFrame(
            [
                {
                    "报告日期": "2023-12-31",
                    "经营活动产生的现金流量净额": "100,000.00",
                    "投资活动产生的现金流量净额": "-50,000.00",
                }
            ]
        )
        source = SourceConfig(
            name="akshare",
            adapter="financial_sina_adapter",
            priority=1,
            params={"symbol": "现金流量表"},
        )
        with patch("akshare.stock_financial_report_sina", return_value=mock_df):
            result = adapter.fetch("000001", source)

        assert len(result) == 1
        assert result[0]["报告日期"] == "2023-12-31"
        assert result[0]["经营活动产生的现金流量净额"] == "100,000.00"

    def test_fetch_empty_returns_empty_list(self):
        adapter = FinancialSinaAdapter()
        source = SourceConfig(
            name="akshare",
            adapter="financial_sina_adapter",
            priority=1,
            params={"symbol": "利润表"},
        )
        with patch("akshare.stock_financial_report_sina", return_value=pd.DataFrame()):
            result = adapter.fetch("000001", source)
        assert result == []

    def test_fetch_none_returns_empty_list(self):
        adapter = FinancialSinaAdapter()
        source = SourceConfig(
            name="akshare",
            adapter="financial_sina_adapter",
            priority=1,
            params={"symbol": "利润表"},
        )
        with patch("akshare.stock_financial_report_sina", return_value=None):
            result = adapter.fetch("000001", source)
        assert result == []

    def test_fetch_missing_symbol_raises(self):
        adapter = FinancialSinaAdapter()
        source = SourceConfig(
            name="akshare",
            adapter="financial_sina_adapter",
            priority=1,
            params={},
        )
        with pytest.raises(ValueError, match="source_config.params 必须包含 symbol"):
            adapter.fetch("000001", source)
