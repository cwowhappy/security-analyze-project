"""数据源适配器协议。"""

from typing import Any, Protocol

from data_collector.core.config.field_mapping_config import SourceConfig


class DataSourceAdapter(Protocol):
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        """调用外部 API，返回原始字段字典。"""
        ...
