"""数据源降级管道：串行 fallback + 非空字段补充。"""

from typing import Any

import structlog

from data_collector.core.config.field_mapping_config import SourceConfig
from data_collector.core.pipeline.field_mapper import FieldMapper

logger = structlog.get_logger(__name__)


class SourceFallbackPipeline:
    def __init__(self, adapters: dict[str, Any]) -> None:
        self._adapters = adapters

    def execute(self, stock_code: str, sources: list[SourceConfig]) -> dict[str, Any] | None:
        base_record: dict[str, Any] = {}
        any_success = False
        for source in sorted(sources, key=lambda s: s.priority):
            adapter = self._adapters.get(source.name)
            if adapter is None:
                logger.warning("适配器未找到", source=source.name)
                continue
            mapper = FieldMapper(source.field_mapping)
            try:
                raw = adapter.fetch(stock_code, source)
                mapped = mapper.apply(raw)
                any_success = True
            except Exception as e:
                logger.warning(
                    "数据源采集失败",
                    source=source.name,
                    stock_code=stock_code,
                    error=str(e),
                )
                mapped = mapper.apply({})
            # 合并映射结果：只填充 base_record 中为 None 或空字符串的字段
            for key, value in mapped.items():
                if key not in base_record or base_record.get(key) in (None, ""):
                    base_record[key] = value
        return base_record if any_success else None
