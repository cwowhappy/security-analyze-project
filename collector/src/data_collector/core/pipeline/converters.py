"""字段值转换器注册表。"""

import datetime
from collections.abc import Callable
from typing import Any

_CONVERTERS: dict[str, Callable[[Any], Any]] = {}


def register_converter(name: str, fn: Callable[[Any], Any]) -> None:
    _CONVERTERS[name] = fn


def convert(name: str, value: Any) -> Any:
    if name not in _CONVERTERS:
        raise ValueError(f"未知的转换器: {name}")
    return _CONVERTERS[name](value)


# 内置转换器
register_converter("str", lambda x: str(x).strip() if x is not None else None)
register_converter("int", lambda x: int(x) if x is not None else None)
register_converter("float", lambda x: float(x) if x is not None else None)
register_converter("date", lambda x: datetime.datetime.strptime(str(x).strip(), "%Y%m%d").date() if x else None)
register_converter("datetime", lambda x: datetime.datetime.strptime(str(x).strip(), "%Y%m%d %H:%M:%S") if x else None)
register_converter("shares_10k", lambda x: int(float(x) * 10000) if x is not None else None)
register_converter("percent", lambda x: float(x) / 100 if x is not None else None)
