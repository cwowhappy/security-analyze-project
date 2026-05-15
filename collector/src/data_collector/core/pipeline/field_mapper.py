"""字段映射器：按配置规则将原始数据转换为标准化记录。"""

from typing import Any

from data_collector.core.config.field_mapping_config import FieldMappingRule
from data_collector.core.pipeline.converters import convert


class FieldMapper:
    def __init__(self, rules: list[FieldMappingRule]) -> None:
        self._rules = rules

    def apply(self, raw_data: dict[str, Any]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for rule in self._rules:
            value = self._extract_value(raw_data, rule.api_field)
            if value is None or value == "":
                if rule.null_policy == "fail":
                    raise ValueError(f"字段 {rule.db_field} 不能为空")
                elif rule.null_policy == "default":
                    result[rule.db_field] = rule.default_value
                else:  # skip
                    result[rule.db_field] = None
            else:
                result[rule.db_field] = convert(rule.converter, value)
        return result

    def _extract_value(self, raw_data: dict[str, Any], api_fields: list[str]) -> Any:
        for field in api_fields:
            if field in raw_data:
                return raw_data[field]
        return None
