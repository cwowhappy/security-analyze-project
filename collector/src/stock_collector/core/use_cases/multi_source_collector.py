"""多数据源采集用例，支持主动降级和切换。"""

from typing import Any

import structlog

from stock_collector.core.domain.data_source_error import DataSourceError, SourceUnavailableError
from stock_collector.core.ports.data_source import DataSource

logger = structlog.get_logger(__name__)


class MultiSourceCollector:
    """多数据源采集器。

    管理多个数据源，支持按优先级采集、自动降级切换、主动切换。
    """

    def __init__(self, sources: list[DataSource]) -> None:
        if not sources:
            raise ValueError("至少需要一个数据源")
        self._sources = sorted(sources, key=lambda s: s.priority)
        self._current_index = 0
        self._fallback_history: list[str] = []

    @property
    def current_source(self) -> DataSource:
        """当前使用的数据源。"""
        return self._sources[self._current_index]

    def collect(self, symbol: str) -> dict[str, Any]:
        """采集数据，自动在数据源间降级切换。

        按优先级依次尝试各数据源，直到成功或全部失败。

        Args:
            symbol: 股票代码。

        Returns:
            采集到的原始数据。

        Raises:
            DataSourceError: 所有数据源均采集失败。
        """
        errors: list[str] = []

        for idx, source in enumerate(self._sources):
            try:
                if not source.is_available():
                    logger.warning(
                        "数据源不可用，跳过",
                        source=source.name,
                        symbol=symbol,
                    )
                    errors.append(f"{source.name}: 不可用")
                    continue

                logger.info(
                    "尝试采集",
                    source=source.name,
                    symbol=symbol,
                    priority=source.priority,
                )

                data = source.fetch(symbol)

                # 如果使用了降级数据源，记录切换历史
                if idx > 0 and self._current_index != idx:
                    self._current_index = idx
                    self._fallback_history.append(source.name)
                    logger.warning(
                        "数据源降级切换",
                        from_source=self._sources[0].name,
                        to_source=source.name,
                        symbol=symbol,
                    )

                logger.info(
                    "采集成功",
                    source=source.name,
                    symbol=symbol,
                )
                return data

            except DataSourceError as e:
                logger.warning(
                    "数据源采集失败",
                    source=source.name,
                    symbol=symbol,
                    error=str(e),
                )
                errors.append(f"{source.name}: {e}")
                continue

        # 所有数据源均失败
        logger.error(
            "所有数据源采集失败",
            symbol=symbol,
            errors=errors,
        )
        raise SourceUnavailableError("all_sources")

    def switch_to(self, source_name: str) -> None:
        """主动切换到指定数据源。

        Args:
            source_name: 目标数据源名称。

        Raises:
            ValueError: 数据源不存在。
        """
        for idx, source in enumerate(self._sources):
            if source.name == source_name:
                old_source = self.current_source.name
                self._current_index = idx
                logger.info(
                    "主动切换数据源",
                    from_source=old_source,
                    to_source=source_name,
                )
                return

        available = [s.name for s in self._sources]
        raise ValueError(f"数据源 '{source_name}' 不存在，可用数据源: {available}")

    def reset(self) -> None:
        """重置到最高优先级数据源。"""
        old_source = self.current_source.name
        self._current_index = 0
        self._fallback_history.clear()
        logger.info(
            "重置到主数据源",
            from_source=old_source,
            to_source=self._sources[0].name,
        )

    def health_report(self) -> dict[str, Any]:
        """生成数据源健康报告。"""
        return {
            "current_source": self.current_source.name,
            "fallback_history": self._fallback_history.copy(),
            "sources": [
                {
                    "name": s.name,
                    "priority": s.priority,
                    "health": s.check_health(),
                }
                for s in self._sources
            ],
        }
