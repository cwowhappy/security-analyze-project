"""多数据源采集器单元测试。"""

import pytest

from stock_collector.adapters.dummy_eastmoney_source import DummyEastmoneySource
from stock_collector.adapters.dummy_sina_source import DummySinaSource
from stock_collector.core.domain.data_source_error import SourceUnavailableError
from stock_collector.core.use_cases.multi_source_collector import MultiSourceCollector


class TestMultiSourceCollector:
    """多数据源采集器测试。"""

    def setup_method(self) -> None:
        self.eastmoney = DummyEastmoneySource()
        self.sina = DummySinaSource()
        self.collector = MultiSourceCollector([self.eastmoney, self.sina])

    def test_should_use_primary_source_first(self) -> None:
        result = self.collector.collect("000001")
        assert result["source"] == "eastmoney"

    def test_should_fallback_to_secondary_when_primary_fails(self) -> None:
        # 重置计数器，确保可预测
        DummyEastmoneySource._call_count = 0
        # 构造一个新的 collector 避免状态残留
        collector = MultiSourceCollector([DummyEastmoneySource(), DummySinaSource()])

        # 前3次东方财富成功
        for _ in range(3):
            collector.collect("000001")

        # 第4次触发限流，降级到新浪
        result = collector.collect("000001")
        assert result["source"] == "sina"
        assert "sina" in collector.health_report()["fallback_history"]

    def test_should_raise_error_when_all_sources_fail(self) -> None:
        class AlwaysFailSource:
            @property
            def name(self):
                return "fail"

            @property
            def priority(self):
                return 1

            def fetch(self, symbol):
                raise Exception("fail")

            def check_health(self):
                from stock_collector.core.ports.data_source import SourceHealth, SourceStatus

                return SourceHealth(SourceStatus.UNAVAILABLE, 0, 1.0, "")

            def is_available(self):
                return False

        collector = MultiSourceCollector([AlwaysFailSource()])
        with pytest.raises(SourceUnavailableError):
            collector.collect("000001")

    def test_should_switch_source_actively(self) -> None:
        self.collector.switch_to("sina")
        assert self.collector.current_source.name == "sina"

    def test_should_raise_when_switch_to_nonexistent_source(self) -> None:
        with pytest.raises(ValueError, match="数据源 'nonexistent' 不存在"):
            self.collector.switch_to("nonexistent")

    def test_should_reset_to_primary_source(self) -> None:
        self.collector.switch_to("sina")
        self.collector.reset()
        assert self.collector.current_source.name == "eastmoney"
        assert not self.collector.health_report()["fallback_history"]
