"""多数据源股票采集器单元测试。"""

from datetime import date

import pytest

from data_collector.core.domain.data_source_error import DataSourceError
from data_collector.core.domain.stock import Stock
from data_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus
from data_collector.core.services.multi_source_stock_collector import MultiSourceStockCollector


class MockPrimarySource(DataSource):
    """测试用主数据源（模拟 akshare）。"""

    def __init__(self, available: bool = True, stocks: list[Stock] | None = None) -> None:
        self._available = available
        self._stocks = stocks if stocks is not None else [
            Stock(stock_code="000001", name="平安银行", ts_code="000001.SZ", market="SZ"),
            Stock(stock_code="000002", name="万科A", ts_code="000002.SZ", market="SZ"),
            Stock(stock_code="600000", name="浦发银行", ts_code="600000.SH", market="SH"),
        ]

    @property
    def name(self) -> str:
        return "akshare"

    @property
    def priority(self) -> int:
        return 1

    def fetch_stock_list(self) -> list[Stock]:
        return list(self._stocks)

    def fetch_company_info(self, stock_code: str) -> None:
        return None

    def check_health(self) -> SourceHealth:
        status = SourceStatus.HEALTHY if self._available else SourceStatus.UNAVAILABLE
        return SourceHealth(status=status, latency_ms=10.0, error_rate=0.0, last_check="")


class MockSupplementSource(DataSource):
    """测试用补充数据源（模拟 tushare）。"""

    def __init__(
        self,
        available: bool = True,
        stocks: list[Stock] | None = None,
        raise_error: bool = False,
    ) -> None:
        self._available = available
        self._stocks = stocks
        self._raise_error = raise_error

    @property
    def name(self) -> str:
        return "tushare"

    @property
    def priority(self) -> int:
        return 2

    def fetch_stock_list(self) -> list[Stock]:
        if self._raise_error:
            raise DataSourceError(self.name, "模拟限流")
        return list(self._stocks) if self._stocks else []

    def fetch_company_info(self, stock_code: str) -> None:
        return None

    def check_health(self) -> SourceHealth:
        status = SourceStatus.HEALTHY if self._available else SourceStatus.UNAVAILABLE
        return SourceHealth(status=status, latency_ms=10.0, error_rate=0.0, last_check="")


class TestMultiSourceStockCollector:
    """MultiSourceStockCollector 测试。"""

    def test_should_merge_fields_from_supplement(self) -> None:
        """正常场景：双源合并，补充字段应被正确填充。"""
        primary = MockPrimarySource()
        supplement = MockSupplementSource(
            stocks=[
                Stock(
                    stock_code="000001",
                    name="平安银行",
                    ts_code="000001.SZ",
                    full_name="平安银行股份有限公司",
                    market="主板",
                    exchange="SZSE",
                    list_date=date(1991, 4, 3),
                    industry="银行",
                    area="深圳",
                ),
                Stock(
                    stock_code="000002",
                    name="万科A",
                    ts_code="000002.SZ",
                    full_name="万科企业股份有限公司",
                    market="主板",
                    exchange="SZSE",
                    list_date=date(1991, 1, 29),
                    industry="全国地产",
                    area="深圳",
                ),
                Stock(
                    stock_code="600000",
                    name="浦发银行",
                    ts_code="600000.SH",
                    full_name="上海浦东发展银行股份有限公司",
                    market="主板",
                    exchange="SSE",
                    list_date=date(1999, 11, 10),
                    industry="银行",
                    area="上海",
                ),
            ]
        )

        collector = MultiSourceStockCollector(primary, supplement)
        result = collector.fetch_merged_stock_list()

        assert len(result) == 3

        # 验证骨架字段保留
        paz = next(s for s in result if s.stock_code == "000001")
        assert paz.name == "平安银行"
        assert paz.ts_code == "000001.SZ"
        assert paz.market == "SZ"

        # 验证补充字段被填充
        assert paz.full_name == "平安银行股份有限公司"
        assert paz.exchange == "SZSE"
        assert paz.list_date == date(1991, 4, 3)
        assert paz.industry == "银行"
        assert paz.area == "深圳"

        vanke = next(s for s in result if s.stock_code == "000002")
        assert vanke.full_name == "万科企业股份有限公司"
        assert vanke.industry == "全国地产"

        pufa = next(s for s in result if s.stock_code == "600000")
        assert pufa.exchange == "SSE"
        assert pufa.area == "上海"

    def test_should_fallback_when_supplement_unavailable(self) -> None:
        """降级场景：补充数据源不可用，仅返回骨架数据。"""
        primary = MockPrimarySource()
        supplement = MockSupplementSource(available=False)

        collector = MultiSourceStockCollector(primary, supplement)
        result = collector.fetch_merged_stock_list()

        assert len(result) == 3
        for stock in result:
            assert stock.industry is None
            assert stock.area is None
            assert stock.exchange is None
            assert stock.list_date is None

    def test_should_fallback_when_supplement_raises_error(self) -> None:
        """降级场景：补充数据源抛出异常，仅返回骨架数据。"""
        primary = MockPrimarySource()
        supplement = MockSupplementSource(raise_error=True)

        collector = MultiSourceStockCollector(primary, supplement)
        result = collector.fetch_merged_stock_list()

        assert len(result) == 3
        for stock in result:
            assert stock.industry is None

    def test_should_handle_partial_match(self) -> None:
        """部分匹配：只有部分股票能在补充源中找到详情。"""
        primary = MockPrimarySource()
        # 补充源只返回 000001 和 600000，缺少 000002
        supplement = MockSupplementSource(
            stocks=[
                Stock(
                    stock_code="000001",
                    name="平安银行",
                    industry="银行",
                    area="深圳",
                ),
                Stock(
                    stock_code="600000",
                    name="浦发银行",
                    industry="银行",
                    area="上海",
                ),
            ]
        )

        collector = MultiSourceStockCollector(primary, supplement)
        result = collector.fetch_merged_stock_list()

        assert len(result) == 3

        paz = next(s for s in result if s.stock_code == "000001")
        assert paz.industry == "银行"

        vanke = next(s for s in result if s.stock_code == "000002")
        assert vanke.industry is None  # 未匹配到补充数据

        pufa = next(s for s in result if s.stock_code == "600000")
        assert pufa.industry == "银行"

    def test_should_return_empty_when_primary_empty(self) -> None:
        """主数据源返回空列表时，直接返回空。"""
        primary = MockPrimarySource(stocks=[])
        supplement = MockSupplementSource(stocks=[Stock(stock_code="000001", name="平安银行", industry="银行")])

        collector = MultiSourceStockCollector(primary, supplement)
        result = collector.fetch_merged_stock_list()

        assert result == []

    def test_should_prefer_detail_full_name(self) -> None:
        """full_name 应以补充源（tushare fullname）为准。"""
        primary = MockPrimarySource(
            stocks=[Stock(stock_code="000001", name="平安银行", full_name="平安银行")]
        )
        supplement = MockSupplementSource(
            stocks=[Stock(stock_code="000001", name="平安银行", full_name="平安银行股份有限公司")]
        )

        collector = MultiSourceStockCollector(primary, supplement)
        result = collector.fetch_merged_stock_list()

        assert result[0].full_name == "平安银行股份有限公司"
