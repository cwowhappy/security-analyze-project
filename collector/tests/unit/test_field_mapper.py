import pytest
from data_collector.core.config.field_mapping_config import FieldMappingRule, SourceConfig
from data_collector.core.pipeline.field_mapper import FieldMapper


class TestFieldMapper:
    def test_basic_mapping(self):
        rules = [
            FieldMappingRule(api_field=["代码"], db_field="stock_code", converter="str", null_policy="skip"),
            FieldMappingRule(api_field=["名称"], db_field="name", converter="str", null_policy="fail"),
        ]
        mapper = FieldMapper(rules)
        result = mapper.apply({"代码": "000001", "名称": "平安银行"})
        assert result == {"stock_code": "000001", "name": "平安银行"}

    def test_null_policy_skip(self):
        rules = [
            FieldMappingRule(api_field=["行业"], db_field="industry", converter="str", null_policy="skip"),
        ]
        mapper = FieldMapper(rules)
        result = mapper.apply({})
        assert result == {"industry": None}

    def test_null_policy_default(self):
        rules = [
            FieldMappingRule(api_field=["行业"], db_field="industry", converter="str", null_policy="default", default_value=""),
        ]
        mapper = FieldMapper(rules)
        result = mapper.apply({})
        assert result == {"industry": ""}

    def test_null_policy_fail(self):
        rules = [
            FieldMappingRule(api_field=["名称"], db_field="name", converter="str", null_policy="fail"),
        ]
        mapper = FieldMapper(rules)
        with pytest.raises(ValueError, match="字段.*不能为空"):
            mapper.apply({})

    def test_multi_alias(self):
        rules = [
            FieldMappingRule(api_field=["资产总计", "总资产"], db_field="total_assets", converter="int", null_policy="skip"),
        ]
        mapper = FieldMapper(rules)
        assert mapper.apply({"资产总计": "100"}) == {"total_assets": 100}
        assert mapper.apply({"总资产": "200"}) == {"total_assets": 200}
