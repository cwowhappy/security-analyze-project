import pytest
from unittest.mock import Mock

from data_collector.core.config.field_mapping_config import SourceConfig, FieldMappingRule
from data_collector.core.pipeline.source_fallback_pipeline import SourceFallbackPipeline


class TestSourceFallbackPipeline:
    def test_single_source_success(self):
        adapter = Mock()
        adapter.fetch.return_value = {"代码": "000001", "名称": "平安银行"}
        pipeline = SourceFallbackPipeline(adapters={"akshare": adapter})
        source = SourceConfig(name="akshare", adapter="akshare_adapter", priority=1, field_mapping=[
            FieldMappingRule(api_field=["代码"], db_field="stock_code", converter="str", null_policy="skip"),
            FieldMappingRule(api_field=["名称"], db_field="name", converter="str", null_policy="skip"),
        ])
        result = pipeline.execute("000001", [source])
        assert result == {"stock_code": "000001", "name": "平安银行"}

    def test_fallback_to_second_source(self):
        adapter1 = Mock()
        adapter1.fetch.side_effect = Exception("timeout")
        adapter2 = Mock()
        adapter2.fetch.return_value = {"ts_code": "000001.SZ", "name": "平安银行"}
        pipeline = SourceFallbackPipeline(adapters={"akshare": adapter1, "tushare": adapter2})
        sources = [
            SourceConfig(name="akshare", adapter="akshare_adapter", priority=1, field_mapping=[
                FieldMappingRule(api_field=["代码"], db_field="stock_code", converter="str", null_policy="skip"),
            ]),
            SourceConfig(name="tushare", adapter="tushare_adapter", priority=2, field_mapping=[
                FieldMappingRule(api_field=["ts_code"], db_field="ts_code", converter="str", null_policy="skip"),
                FieldMappingRule(api_field=["name"], db_field="name", converter="str", null_policy="skip"),
            ]),
        ]
        result = pipeline.execute("000001", sources)
        assert result == {"ts_code": "000001.SZ", "name": "平安银行", "stock_code": None}

    def test_non_null_override_blocked(self):
        adapter1 = Mock()
        adapter1.fetch.return_value = {"代码": "000001", "名称": "平安银行"}
        adapter2 = Mock()
        adapter2.fetch.return_value = {"名称": "PAB", "行业": "银行"}
        pipeline = SourceFallbackPipeline(adapters={"akshare": adapter1, "tushare": adapter2})
        sources = [
            SourceConfig(name="akshare", adapter="akshare_adapter", priority=1, field_mapping=[
                FieldMappingRule(api_field=["代码"], db_field="stock_code", converter="str", null_policy="skip"),
                FieldMappingRule(api_field=["名称"], db_field="name", converter="str", null_policy="skip"),
            ]),
            SourceConfig(name="tushare", adapter="tushare_adapter", priority=2, field_mapping=[
                FieldMappingRule(api_field=["名称"], db_field="name", converter="str", null_policy="skip"),
                FieldMappingRule(api_field=["行业"], db_field="industry", converter="str", null_policy="skip"),
            ]),
        ]
        result = pipeline.execute("000001", sources)
        # name 来自 akshare 且非空，不应被 tushare 覆盖
        assert result["name"] == "平安银行"
        assert result["industry"] == "银行"

    def test_all_sources_failed(self):
        adapter = Mock()
        adapter.fetch.side_effect = Exception("fail")
        pipeline = SourceFallbackPipeline(adapters={"akshare": adapter})
        source = SourceConfig(name="akshare", adapter="akshare_adapter", priority=1, field_mapping=[])
        result = pipeline.execute("000001", [source])
        assert result is None
