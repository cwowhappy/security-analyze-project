"""数据源异常单元测试。"""


from data_collector.core.domain.data_source_error import (
    DataSourceError,
    SourceRateLimitError,
    SourceUnavailableError,
)


class TestDataSourceError:
    """数据源异常测试。"""

    def test_should_include_source_name_in_message(self) -> None:
        err = DataSourceError("akshare", "连接超时")
        assert "akshare" in str(err)
        assert "连接超时" in str(err)
        assert err.source_name == "akshare"

    def test_should_create_source_unavailable_error(self) -> None:
        err = SourceUnavailableError("tushare")
        assert "tushare" in str(err)
        assert "不可用" in str(err)

    def test_should_create_source_rate_limit_error(self) -> None:
        err = SourceRateLimitError("akshare")
        assert "akshare" in str(err)
        assert "限流" in str(err)
