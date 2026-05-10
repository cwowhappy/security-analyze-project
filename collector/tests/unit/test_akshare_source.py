"""Akshare 数据源适配器单元测试。"""

from unittest.mock import MagicMock, patch

from data_collector.adapters.akshare_source import AkshareDataSource
from data_collector.config import Settings


class TestAkshareDataSource:
    """AkshareDataSource 测试。"""

    def setup_method(self) -> None:
        self.source = AkshareDataSource(Settings())

    def test_should_return_name_and_priority(self) -> None:
        assert self.source.name == "akshare"
        assert self.source.priority == 1

    def test_should_fetch_stock_list(self) -> None:
        mock_df = MagicMock()
        mock_df.empty = False
        mock_df.iterrows.return_value = iter([
            (0, {"code": "000001", "name": "平安银行", "market": "SZ"}),
            (1, {"code": "000002", "name": "万科A", "market": "SZ"}),
        ])

        with patch("akshare.stock_info_a_code_name") as mock_fn:
            mock_fn.return_value = mock_df
            result = self.source.fetch_stock_list()

        assert len(result) == 2
        assert result[0].stock_code == "000001"
        assert result[0].name == "平安银行"
        assert result[1].stock_code == "000002"

    def test_should_return_empty_list_when_no_data(self) -> None:
        mock_df = MagicMock()
        mock_df.empty = True

        with patch("akshare.stock_info_a_code_name") as mock_fn:
            mock_fn.return_value = mock_df
            result = self.source.fetch_stock_list()

        assert result == []

    def test_should_fetch_company_info(self) -> None:
        mock_df = MagicMock()
        mock_df.empty = False
        row = MagicMock()
        row.get.side_effect = lambda key, default="": {
            "公司名称": "平安银行股份有限公司",
            "统一社会信用代码": "9144030019218537XX",
            "法人代表": "谢永林",
            "成立日期": "1987-12-22",
        }.get(key, default)
        mock_df.iloc = [row]

        with patch("akshare.stock_profile_cninfo") as mock_fn:
            mock_fn.return_value = mock_df
            result = self.source.fetch_company_info("000001")

        assert result is not None
        assert result.name == "平安银行股份有限公司"
        assert result.unified_social_credit_code == "9144030019218537XX"
        assert result.legal_representative == "谢永林"

    def test_should_return_none_when_company_not_found(self) -> None:
        mock_df = MagicMock()
        mock_df.empty = True

        with patch("akshare.stock_profile_cninfo") as mock_fn:
            mock_fn.return_value = mock_df
            result = self.source.fetch_company_info("999999")

        assert result is None

    def test_should_check_health(self) -> None:
        mock_df = MagicMock()
        mock_df.empty = True

        with patch("akshare.stock_info_a_code_name") as mock_fn:
            mock_fn.return_value = mock_df
            health = self.source.check_health()

        assert health.status.value == "healthy"
        assert health.latency_ms >= 0

    def test_should_return_unavailable_on_health_check_failure(self) -> None:
        with patch("akshare.stock_info_a_code_name") as mock_fn:
            mock_fn.side_effect = Exception("Network Error")
            health = self.source.check_health()

        assert health.status.value == "unavailable"

    def test_should_retry_on_failure(self) -> None:
        with patch("akshare.stock_info_a_code_name") as mock_fn:
            mock_fn.side_effect = [Exception("Error"), Exception("Error"), MagicMock(empty=True)]
            result = self.source.fetch_stock_list()

        assert result == []
        assert mock_fn.call_count == 3

    def test_should_convert_to_ts_code(self) -> None:
        assert self.source._to_ts_code("000001", "SZ") == "000001.SZ"
        assert self.source._to_ts_code("600000", "SH") == "600000.SH"
        assert self.source._to_ts_code("600000", None) == "600000.SH"
        assert self.source._to_ts_code("000001", None) == "000001.SZ"
        assert self.source._to_ts_code("430001", None) == "430001.BJ"
        assert self.source._to_ts_code("", None) is None
