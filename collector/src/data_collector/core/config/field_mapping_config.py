"""字段映射配置加载器，从 YAML 文件解析采集规则。"""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml


@dataclass
class FieldMappingRule:
    api_field: str | list[str]
    db_field: str
    converter: str = "str"
    null_policy: str = "skip"
    default_value: Any = None


@dataclass
class SourceConfig:
    name: str
    adapter: str
    priority: int
    field_mapping: list[FieldMappingRule] = field(default_factory=list)
    params: dict = field(default_factory=dict)
    min_delay: float | None = None
    max_delay: float | None = None


@dataclass
class TaskMappingConfig:
    task_type: str
    ttl_hours: int | None = None
    sources: list[SourceConfig] = field(default_factory=list)


class FieldMappingConfigLoader:
    def __init__(self, config_dir: str) -> None:
        self._config_dir = Path(config_dir)

    def load(self, task_type: str) -> TaskMappingConfig:
        file_path = self._config_dir / f"{task_type}.yaml"
        if not file_path.exists():
            raise FileNotFoundError(f"配置文件不存在: {file_path}")
        with open(file_path, "r", encoding="utf-8") as f:
            raw = yaml.safe_load(f)
        return self._parse(raw)

    def _parse(self, raw: dict) -> TaskMappingConfig:
        sources = []
        for s in raw.get("sources", []):
            rules = []
            for r in s.get("field_mapping", []):
                api_field = r["api_field"]
                if isinstance(api_field, str):
                    api_field = [api_field]
                rules.append(FieldMappingRule(
                    api_field=api_field,
                    db_field=r["db_field"],
                    converter=r.get("converter", "str"),
                    null_policy=r.get("null_policy", "skip"),
                    default_value=r.get("default_value"),
                ))
            sources.append(SourceConfig(
                name=s["name"],
                adapter=s["adapter"],
                priority=s["priority"],
                field_mapping=rules,
                params=s.get("params", {}),
                min_delay=s.get("min_delay"),
                max_delay=s.get("max_delay"),
            ))
        return TaskMappingConfig(
            task_type=raw["task_type"],
            ttl_hours=raw.get("ttl_hours"),
            sources=sources,
        )
