from unittest.mock import MagicMock, patch
import pandas as pd
import pytest
from collector.sources.akshare_source import AkshareSource


class TestAkshareSource:
    def setup_method(self):
        self.mock_ak = MagicMock()
        with patch("collector.sources.akshare_source.pd"):
            with patch.dict("sys.modules", {"akshare": self.mock_ak}):
                # AkshareSource imports akshare inside __init__, we mock it via constructor injection
                pass
        # Re-instantiate with mocked ak module by patching __import__
        self.source = self._create_source()

    def _create_source(self):
        with patch.object(AkshareSource, "__init__", lambda self: None):
            source = AkshareSource.__new__(AkshareSource)
            source._ak = MagicMock()
            source._stock_list = []
            source._max_retries = 3
            source._retry_delay = 0.01
            source._retry_backoff = 1.0
            return source

    def test_search_by_name_exact_code(self):
        result = self.source.search_by_name("600519")
        assert result == [{"code": "600519", "name": ""}]

    def test_search_by_name_fuzzy(self):
        self.source._stock_list = [
            {"code": "600519", "name": "贵州茅台"},
            {"code": "000001", "name": "平安银行"},
        ]
        result = self.source.search_by_name("茅台")
        assert len(result) == 1
        assert result[0]["code"] == "600519"

    def test_filter_by_year(self):
        df = pd.DataFrame({
            "REPORT_DATE": ["2023-12-31", "2024-03-31", "2024-06-30"],
            "VALUE": [1, 2, 3],
        })
        filtered = AkshareSource._filter_by_year(df, 2024, None)
        assert len(filtered) == 2
        assert list(filtered["VALUE"]) == [2, 3]

    def test_infer_market(self):
        assert AkshareSource.infer_market("600519") == "SH"
        assert AkshareSource.infer_market("000001") == "SZ"
        assert AkshareSource.infer_market("430047") == "BJ"
        assert AkshareSource.infer_market("") == "SH"
