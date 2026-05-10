"""数据源异常。"""


class DataSourceError(Exception):
    """数据源异常基类。"""

    def __init__(self, source_name: str, message: str) -> None:
        self.source_name = source_name
        super().__init__(f"[{source_name}] {message}")


class SourceUnavailableError(DataSourceError):
    """数据源不可用异常。"""

    def __init__(self, source_name: str) -> None:
        super().__init__(source_name, "数据源不可用")


class SourceRateLimitError(DataSourceError):
    """数据源限流异常。"""

    def __init__(self, source_name: str) -> None:
        super().__init__(source_name, "触发限流")
