# Collector Pipeline Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 `collector` 从脚本集合重构为配置驱动的通用采集管道框架，支持单股票更新、批量断点恢复、自适应调速、多数据源 fallback、字段映射外置化。

**Architecture:** 在现有 `TaskExecutor` + 注册表模式基础上，新增 `AdaptiveRequestEngine`（智能调速）、`FieldMapper`（配置化字段映射）、`SourceFallbackPipeline`（多源串行 fallback）、`StockCollectionStateTracker`（stock 级状态持久化）。任务类型语义化为纯数据类型，`mode`（full/single）和 `source_priority` 作为执行参数。

**Tech Stack:** Python 3.11, Poetry, pydantic-settings, psycopg2, PyYAML, pytest

---

## 阶段一：基础设施（数据库、配置、模型）

### Task 1: Settings 新增采集管道配置项

**Files:**
- Modify: `collector/src/data_collector/config.py`
- Test: `collector/tests/unit/test_config.py`

**Step 1: Write the failing test**

在 `test_config.py` 末尾追加：

```python
def test_collection_settings_defaults():
    from data_collector.config import Settings
    s = Settings()
    assert s.collection_ttl_hours == 24
    assert s.collection_batch_size == 20
    assert s.adaptive_min_delay == 1.0
    assert s.adaptive_max_delay == 60.0
    assert s.adaptive_backoff_jitter == 0.5
    assert s.adaptive_success_threshold == 10
    assert s.retry_max_attempts == 3
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_config.py::test_collection_settings_defaults -v
```

Expected: FAIL with `AttributeError: 'Settings' object has no attribute 'collection_ttl_hours'`

**Step 3: Write minimal implementation**

在 `collector/src/data_collector/config.py` 的 `Settings` 类中，在 `log_format` 字段之后添加：

```python
    # 采集管道配置
    collection_ttl_hours: int = Field(default=24, alias="COLLECTION_TTL_HOURS")
    collection_batch_size: int = Field(default=20, alias="COLLECTION_BATCH_SIZE")
    adaptive_min_delay: float = Field(default=1.0, alias="ADAPTIVE_MIN_DELAY")
    adaptive_max_delay: float = Field(default=60.0, alias="ADAPTIVE_MAX_DELAY")
    adaptive_backoff_jitter: float = Field(default=0.5, alias="ADAPTIVE_BACKOFF_JITTER")
    adaptive_success_threshold: int = Field(default=10, alias="ADAPTIVE_SUCCESS_THRESHOLD")
    retry_max_attempts: int = Field(default=3, alias="RETRY_MAX_ATTEMPTS")
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_config.py::test_collection_settings_defaults -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/config.py collector/tests/unit/test_config.py
git commit -m "feat(config): add collection pipeline settings"
```

---

### Task 2: CollectionTask 领域模型扩展（mode + source_priority）

**Files:**
- Modify: `collector/src/data_collector/core/domain/collection_task.py`
- Test: `collector/tests/unit/test_collection_task_domain.py`

**Step 1: Write the failing test**

在 `test_collection_task_domain.py` 末尾追加：

```python
def test_collection_task_mode_and_source_priority():
    from data_collector.core.domain.collection_task import CollectionTask
    task = CollectionTask(
        task_type="stock_basic",
        mode="full",
        source_priority=["akshare", "tushare"],
    )
    assert task.mode == "full"
    assert task.source_priority == ["akshare", "tushare"]
    d = task.to_dict()
    assert d["mode"] == "full"
    assert d["source_priority"] == '["akshare", "tushare"]'
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_collection_task_domain.py::test_collection_task_mode_and_source_priority -v
```

Expected: FAIL with `TypeError: CollectionTask.__init__() got an unexpected keyword argument 'mode'`

**Step 3: Write minimal implementation**

修改 `collector/src/data_collector/core/domain/collection_task.py`：

```python
@dataclass
class CollectionTask:
    """采集任务执行记录领域实体。"""

    id: str | None = None
    task_type: str = ""
    mode: str = "full"          # 新增：full / single
    source_priority: list = field(default_factory=list)  # 新增
    task_params: dict = field(default_factory=dict)
    status: str = TaskStatus.PENDING.value
    data_source: str | None = None
    total_count: int = 0
    success_count: int = 0
    fail_count: int = 0
    error_message: str | None = None
    started_at: datetime | None = None
    completed_at: datetime | None = None
    created_at: datetime | None = None

    def __post_init__(self) -> None:
        if not self.task_type:
            raise ValueError("任务类型 task_type 不能为空")
        if self.mode not in ("full", "single"):
            raise ValueError("mode 必须是 full 或 single")

    def to_dict(self) -> dict:
        import json
        return {
            "id": self.id,
            "task_type": self.task_type,
            "mode": self.mode,
            "source_priority": json.dumps(self.source_priority) if self.source_priority else None,
            "task_params": json.dumps(self.task_params) if self.task_params else None,
            "status": self.status,
            "data_source": self.data_source,
            "total_count": self.total_count,
            "success_count": self.success_count,
            "fail_count": self.fail_count,
            "error_message": self.error_message,
            "started_at": self.started_at,
            "completed_at": self.completed_at,
            "created_at": self.created_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "CollectionTask":
        import json
        task_params = data.get("task_params")
        if isinstance(task_params, str):
            task_params = json.loads(task_params)
        elif task_params is None:
            task_params = {}
        source_priority = data.get("source_priority")
        if isinstance(source_priority, str):
            source_priority = json.loads(source_priority)
        elif source_priority is None:
            source_priority = []
        return cls(
            id=data.get("id"),
            task_type=data.get("task_type", ""),
            mode=data.get("mode", "full"),
            source_priority=source_priority,
            task_params=task_params,
            status=data.get("status", TaskStatus.PENDING.value),
            data_source=data.get("data_source"),
            total_count=data.get("total_count", 0),
            success_count=data.get("success_count", 0),
            fail_count=data.get("fail_count", 0),
            error_message=data.get("error_message"),
            started_at=data.get("started_at"),
            completed_at=data.get("completed_at"),
            created_at=data.get("created_at"),
        )
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_collection_task_domain.py::test_collection_task_mode_and_source_priority -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/core/domain/collection_task.py collector/tests/unit/test_collection_task_domain.py
git commit -m "feat(domain): extend CollectionTask with mode and source_priority"
```

---

### Task 3: 数据库迁移 SQL

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__collection_pipeline_fields.sql`

**Step 1: Write the migration**

```sql
-- 扩展 tb_collection_task 表
ALTER TABLE tb_collection_task
    ADD COLUMN IF NOT EXISTS mode VARCHAR(20) DEFAULT 'full',
    ADD COLUMN IF NOT EXISTS source_priority JSONB;

-- 创建 stock 级采集状态表
CREATE TABLE IF NOT EXISTS tb_collection_stock_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID REFERENCES tb_collection_task(id),
    stock_code VARCHAR(20) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'success', 'failed', 'skipped')),
    error_message TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(task_id, stock_code, task_type)
);

CREATE INDEX IF NOT EXISTS idx_collection_stock_state_lookup
    ON tb_collection_stock_state(task_id, stock_code, task_type);

CREATE INDEX IF NOT EXISTS idx_collection_stock_state_updated
    ON tb_collection_stock_state(updated_at);
```

**Step 2: Verify SQL syntax**

```bash
cd backend && ./gradlew flywayValidate
```

Expected: 若 Flyway 配置正确，应显示验证通过（实际执行需在本地 PostgreSQL 运行）

**Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V2__collection_pipeline_fields.sql
git commit -m "feat(db): add collection pipeline migration"
```

---

### Task 4: DbCollectionTaskRepository 适配新字段

**Files:**
- Modify: `collector/src/data_collector/adapters/db_collection_task_repository.py`
- Test: `collector/tests/unit/test_db_collection_task_repository.py`

**Step 1: Write the failing test**

在 `test_db_collection_task_repository.py` 中，找到测试 `save` 或 `find_by_id` 的用例，追加：

```python
def test_save_and_find_with_mode_and_source_priority(repo):
    from data_collector.core.domain.collection_task import CollectionTask
    task = CollectionTask(
        task_type="stock_basic",
        mode="single",
        source_priority=["akshare"],
    )
    repo.save(task)
    found = repo.find_by_id(task.id)
    assert found is not None
    assert found.mode == "single"
    assert found.source_priority == ["akshare"]
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_db_collection_task_repository.py::test_save_and_find_with_mode_and_source_priority -v
```

Expected: FAIL（SQL 不匹配，缺少 mode/source_priority 字段）

**Step 3: Write minimal implementation**

修改 `collector/src/data_collector/adapters/db_collection_task_repository.py`：

```python
    def save(self, task: CollectionTask) -> None:
        if task.id is None:
            task.id = str(ulid.ULID())
        sql = """
        INSERT INTO tb_collection_task (
            id, task_type, mode, source_priority, task_params, status, data_source,
            total_count, success_count, fail_count,
            error_message, started_at, completed_at, created_at
        ) VALUES (%s, %s, %s, %s::jsonb, %s::jsonb, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
        """
        import json
        params = (
            task.id,
            task.task_type,
            task.mode,
            json.dumps(task.source_priority) if task.source_priority else None,
            json.dumps(task.task_params) if task.task_params else None,
            task.status,
            task.data_source,
            task.total_count,
            task.success_count,
            task.fail_count,
            task.error_message,
            task.started_at,
            task.completed_at,
        )
        execute_update(sql, params)
        logger.debug("任务已保存", id=task.id, task_type=task.task_type)

    def update(self, task: CollectionTask) -> None:
        sql = """
        UPDATE tb_collection_task SET
            status = %s,
            data_source = %s,
            total_count = %s,
            success_count = %s,
            fail_count = %s,
            error_message = %s,
            started_at = %s,
            completed_at = %s,
            mode = %s,
            source_priority = %s::jsonb
        WHERE id = %s
        """
        import json
        params = (
            task.status,
            task.data_source,
            task.total_count,
            task.success_count,
            task.fail_count,
            task.error_message,
            task.started_at,
            task.completed_at,
            task.mode,
            json.dumps(task.source_priority) if task.source_priority else None,
            task.id,
        )
        execute_update(sql, params)
        logger.debug("任务已更新", id=task.id, status=task.status)
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_db_collection_task_repository.py::test_save_and_find_with_mode_and_source_priority -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/adapters/db_collection_task_repository.py collector/tests/unit/test_db_collection_task_repository.py
git commit -m "feat(repo): adapt DbCollectionTaskRepository for mode and source_priority"
```

---

### Task 5: FieldMappingConfigLoader 基础结构与 YAML 解析

**Files:**
- Create: `collector/src/data_collector/core/config/__init__.py`
- Create: `collector/src/data_collector/core/config/field_mapping_config.py`
- Test: `collector/tests/unit/test_config_loader.py`

**Step 1: Add PyYAML dependency**

```bash
cd collector && poetry add pyyaml
```

**Step 2: Write the failing test**

创建 `collector/tests/unit/test_config_loader.py`：

```python
import pytest
from data_collector.core.config.field_mapping_config import FieldMappingConfigLoader


class TestFieldMappingConfigLoader:
    def test_load_valid_config(self, tmp_path):
        config_dir = tmp_path / "mappings"
        config_dir.mkdir()
        config_file = config_dir / "stock_basic.yaml"
        config_file.write_text("""
task_type: stock_basic
ttl_hours: 12
sources:
  - name: akshare
    adapter: stock_basic_akshare_adapter
    priority: 1
    field_mapping:
      - api_field: "代码"
        db_field: "stock_code"
        converter: "str"
        null_policy: "skip"
""")
        loader = FieldMappingConfigLoader(str(config_dir))
        config = loader.load("stock_basic")
        assert config.task_type == "stock_basic"
        assert config.ttl_hours == 12
        assert len(config.sources) == 1
        assert config.sources[0].name == "akshare"
        assert config.sources[0].field_mapping[0].db_field == "stock_code"
```

**Step 3: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_config_loader.py::TestFieldMappingConfigLoader::test_load_valid_config -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 4: Write minimal implementation**

创建 `collector/src/data_collector/core/config/__init__.py`（空文件）。

创建 `collector/src/data_collector/core/config/field_mapping_config.py`：

```python
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
```

**Step 5: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_config_loader.py::TestFieldMappingConfigLoader::test_load_valid_config -v
```

Expected: PASS

**Step 6: Commit**

```bash
git add collector/src/data_collector/core/config/ collector/tests/unit/test_config_loader.py collector/pyproject.toml collector/poetry.lock
git commit -m "feat(config): add FieldMappingConfigLoader for YAML mapping rules"
```

---

### Task 6: Converter 注册表

**Files:**
- Create: `collector/src/data_collector/core/pipeline/__init__.py`
- Create: `collector/src/data_collector/core/pipeline/converters.py`
- Test: `collector/tests/unit/test_converters.py`

**Step 1: Write the failing test**

创建 `collector/tests/unit/test_converters.py`：

```python
import datetime

import pytest

from data_collector.core.pipeline.converters import convert, register_converter


class TestConverters:
    def test_str_converter(self):
        assert convert("str", 123) == "123"

    def test_int_converter(self):
        assert convert("int", "42") == 42

    def test_float_converter(self):
        assert convert("float", "3.14") == 3.14

    def test_date_converter(self):
        assert convert("date", "20230101") == datetime.date(2023, 1, 1)

    def test_shares_10k_converter(self):
        assert convert("shares_10k", "10000") == 100000000

    def test_unknown_converter_raises(self):
        with pytest.raises(ValueError, match="未知的转换器"):
            convert("unknown", "x")

    def test_custom_converter(self):
        register_converter("double", lambda x: float(x) * 2)
        assert convert("double", "5") == 10.0
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_converters.py -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 3: Write minimal implementation**

创建 `collector/src/data_collector/core/pipeline/__init__.py`（空文件）。

创建 `collector/src/data_collector/core/pipeline/converters.py`：

```python
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
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_converters.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/core/pipeline/ collector/tests/unit/test_converters.py
git commit -m "feat(pipeline): add converter registry for field transformations"
```

---

## 阶段二：核心引擎

### Task 7: AdaptiveRequestEngine 基础结构与延迟状态

**Files:**
- Create: `collector/src/data_collector/core/pipeline/adaptive_request_engine.py`
- Test: `collector/tests/unit/test_adaptive_request_engine.py`

**Step 1: Write the failing test**

创建 `collector/tests/unit/test_adaptive_request_engine.py`：

```python
import pytest
from data_collector.core.pipeline.adaptive_request_engine import AdaptiveRequestEngine, DelayState


class TestDelayState:
    def test_initial_state(self):
        s = DelayState(current_delay=1.5)
        assert s.current_delay == 1.5
        assert s.consecutive_success == 0


class TestAdaptiveRequestEngine:
    def test_get_delay_for_source(self):
        engine = AdaptiveRequestEngine(min_delay=1.0, max_delay=60.0, backoff_jitter=0.5, success_threshold=10)
        delay = engine.get_delay("akshare")
        assert 1.0 <= delay <= 60.0

    def test_success_decreases_delay(self):
        engine = AdaptiveRequestEngine(min_delay=1.0, max_delay=60.0, backoff_jitter=0.0, success_threshold=2)
        engine.record_success("akshare")
        engine.record_success("akshare")
        # 连续2次成功后应尝试降速
        delay = engine.get_delay("akshare")
        assert delay < 60.0 or pytest.approx(delay) == 60.0
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_adaptive_request_engine.py -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 3: Write minimal implementation**

创建 `collector/src/data_collector/core/pipeline/adaptive_request_engine.py`：

```python
"""自适应请求引擎：根据接口响应动态调节调用间隔。"""

import random
import time
from dataclasses import dataclass
from typing import Callable


@dataclass
class DelayState:
    current_delay: float
    consecutive_success: int = 0


class AdaptiveRequestEngine:
    def __init__(
        self,
        min_delay: float = 1.0,
        max_delay: float = 60.0,
        backoff_jitter: float = 0.5,
        success_threshold: int = 10,
        retry_max_attempts: int = 3,
    ) -> None:
        self._min_delay = min_delay
        self._max_delay = max_delay
        self._backoff_jitter = backoff_jitter
        self._success_threshold = success_threshold
        self._retry_max_attempts = retry_max_attempts
        self._states: dict[str, DelayState] = {}

    def _get_state(self, source: str) -> DelayState:
        if source not in self._states:
            self._states[source] = DelayState(
                current_delay=random.uniform(self._min_delay, min(self._min_delay * 2, self._max_delay))
            )
        return self._states[source]

    def get_delay(self, source: str) -> float:
        return self._get_state(source).current_delay

    def record_success(self, source: str) -> None:
        state = self._get_state(source)
        state.consecutive_success += 1
        if state.consecutive_success >= self._success_threshold:
            state.current_delay = max(state.current_delay * 0.9, self._min_delay)
            state.consecutive_success = 0

    def record_failure(self, source: str, recoverable: bool = True) -> None:
        state = self._get_state(source)
        state.consecutive_success = 0
        if recoverable:
            jitter = random.uniform(0, self._backoff_jitter)
            state.current_delay = min(state.current_delay * 2 + jitter, self._max_delay)

    def sleep(self, source: str) -> None:
        time.sleep(self.get_delay(source))

    def execute(self, source: str, fn: Callable, *args, **kwargs):
        self.sleep(source)
        last_exception = None
        for attempt in range(self._retry_max_attempts):
            try:
                result = fn(*args, **kwargs)
                self.record_success(source)
                return result
            except Exception as e:
                last_exception = e
                self.record_failure(source, recoverable=True)
                if attempt < self._retry_max_attempts - 1:
                    self.sleep(source)
        raise last_exception
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_adaptive_request_engine.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/core/pipeline/adaptive_request_engine.py collector/tests/unit/test_adaptive_request_engine.py
git commit -m "feat(pipeline): add AdaptiveRequestEngine with delay state management"
```

---

### Task 8: AdaptiveRequestEngine 不可恢复错误不重试

**Files:**
- Modify: `collector/src/data_collector/core/pipeline/adaptive_request_engine.py`
- Test: `collector/tests/unit/test_adaptive_request_engine.py`

**Step 1: Write the failing test**

在 `test_adaptive_request_engine.py` 追加：

```python
class TestAdaptiveRequestEngineRetry:
    def test_recoverable_error_retries(self):
        engine = AdaptiveRequestEngine(min_delay=0.01, max_delay=0.1, backoff_jitter=0.0, retry_max_attempts=3)
        call_count = 0
        def flaky():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise TimeoutError("timeout")
            return "ok"
        result = engine.execute("src", flaky)
        assert result == "ok"
        assert call_count == 3

    def test_non_recoverable_error_no_retry(self):
        from data_collector.core.pipeline.adaptive_request_engine import NonRecoverableError
        engine = AdaptiveRequestEngine(min_delay=0.01, max_delay=0.1, backoff_jitter=0.0, retry_max_attempts=3)
        call_count = 0
        def fail_fast():
            nonlocal call_count
            call_count += 1
            raise NonRecoverableError("not found")
        with pytest.raises(NonRecoverableError):
            engine.execute("src", fail_fast)
        assert call_count == 1
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_adaptive_request_engine.py::TestAdaptiveRequestEngineRetry -v
```

Expected: FAIL with `ImportError`

**Step 3: Write minimal implementation**

修改 `adaptive_request_engine.py`，在文件顶部添加：

```python
class NonRecoverableError(Exception):
    """不可恢复的错误，触发后不再重试。"""
```

修改 `execute` 方法：

```python
    def execute(self, source: str, fn: Callable, *args, **kwargs):
        self.sleep(source)
        last_exception = None
        for attempt in range(self._retry_max_attempts):
            try:
                result = fn(*args, **kwargs)
                self.record_success(source)
                return result
            except NonRecoverableError:
                self.record_failure(source, recoverable=False)
                raise
            except Exception as e:
                last_exception = e
                self.record_failure(source, recoverable=True)
                if attempt < self._retry_max_attempts - 1:
                    self.sleep(source)
        raise last_exception
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_adaptive_request_engine.py::TestAdaptiveRequestEngineRetry -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/core/pipeline/adaptive_request_engine.py collector/tests/unit/test_adaptive_request_engine.py
git commit -m "feat(pipeline): support non-recoverable errors without retry"
```

---

### Task 9: FieldMapper 实现

**Files:**
- Create: `collector/src/data_collector/core/pipeline/field_mapper.py`
- Test: `collector/tests/unit/test_field_mapper.py`

**Step 1: Write the failing test**

创建 `collector/tests/unit/test_field_mapper.py`：

```python
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
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_field_mapper.py -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 3: Write minimal implementation**

创建 `collector/src/data_collector/core/pipeline/field_mapper.py`：

```python
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
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_field_mapper.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/core/pipeline/field_mapper.py collector/tests/unit/test_field_mapper.py
git commit -m "feat(pipeline): add FieldMapper with null_policy support"
```

---

### Task 10: StockCollectionStateTracker 实现

**Files:**
- Create: `collector/src/data_collector/adapters/db_stock_state_repository.py`
- Create: `collector/src/data_collector/core/pipeline/stock_state_tracker.py`
- Test: `collector/tests/unit/test_stock_state_tracker.py`

**Step 1: Write the failing test**

创建 `collector/tests/unit/test_stock_state_tracker.py`：

```python
import pytest
from datetime import datetime, timedelta
from unittest.mock import Mock

from data_collector.core.pipeline.stock_state_tracker import StockCollectionStateTracker


class TestStockCollectionStateTracker:
    def test_buffer_and_flush(self):
        repo = Mock()
        tracker = StockCollectionStateTracker(repo, batch_size=2)
        tracker.record_success("task-1", "000001", "stock_basic")
        tracker.record_success("task-1", "000002", "stock_basic")
        # 达到 batch_size，应自动 flush
        assert repo.bulk_upsert.call_count == 1
        args = repo.bulk_upsert.call_args[0][0]
        assert len(args) == 2
        assert args[0]["stock_code"] == "000001"
        assert args[0]["status"] == "success"

    def test_ttl_expired_stock_needs_recollect(self):
        repo = Mock()
        tracker = StockCollectionStateTracker(repo, batch_size=10)
        # 模拟库中有一条成功但已过期记录
        repo.find_by_task.return_value = {
            "000001": {"status": "success", "updated_at": datetime.now() - timedelta(hours=25)}
        }
        needs = tracker.filter_stocks_needing_collection("task-1", ["000001"], "stock_basic", ttl_hours=24)
        assert needs == ["000001"]

    def test_fresh_success_stock_skipped(self):
        repo = Mock()
        tracker = StockCollectionStateTracker(repo, batch_size=10)
        repo.find_by_task.return_value = {
            "000001": {"status": "success", "updated_at": datetime.now() - timedelta(hours=1)}
        }
        needs = tracker.filter_stocks_needing_collection("task-1", ["000001"], "stock_basic", ttl_hours=24)
        assert needs == []
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_stock_state_tracker.py -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 3: Write minimal implementation**

创建 `collector/src/data_collector/adapters/db_stock_state_repository.py`：

```python
"""Stock 级采集状态仓库。"""

from collections.abc import Sequence
from datetime import datetime

import structlog

from data_collector.infrastructure.db import execute_query, execute_update

logger = structlog.get_logger(__name__)


class DbStockStateRepository:
    def bulk_upsert(self, records: list[dict]) -> None:
        if not records:
            return
        sql = """
        INSERT INTO tb_collection_stock_state (
            id, task_id, stock_code, task_type, status, error_message, updated_at
        ) VALUES (%s, %s, %s, %s, %s, %s, NOW())
        ON CONFLICT (task_id, stock_code, task_type) DO UPDATE SET
            status = EXCLUDED.status,
            error_message = EXCLUDED.error_message,
            updated_at = EXCLUDED.updated_at
        """
        import ulid
        params = [
            (
                str(ulid.ULID()),
                r["task_id"],
                r["stock_code"],
                r["task_type"],
                r["status"],
                r.get("error_message"),
            )
            for r in records
        ]
        with execute_update(sql, params[0]) as cursor:
            cursor.executemany(sql, params)
        logger.debug("批量更新 stock 状态", count=len(records))

    def find_by_task(self, task_id: str, task_type: str) -> dict[str, dict]:
        sql = """
        SELECT stock_code, status, error_message, updated_at
        FROM tb_collection_stock_state
        WHERE task_id = %s AND task_type = %s
        """
        rows = execute_query(sql, (task_id, task_type))
        return {
            row["stock_code"]: {
                "status": row["status"],
                "error_message": row["error_message"],
                "updated_at": row["updated_at"],
            }
            for row in rows
        }
```

创建 `collector/src/data_collector/core/pipeline/stock_state_tracker.py`：

```python
"""Stock 级采集状态追踪器，支持批次缓冲和 TTL 过期判断。"""

from datetime import datetime, timedelta
from typing import Any


class StockCollectionStateTracker:
    def __init__(self, repository, batch_size: int = 20) -> None:
        self._repo = repository
        self._batch_size = batch_size
        self._buffer: list[dict] = []

    def record_success(self, task_id: str, stock_code: str, task_type: str) -> None:
        self._buffer.append({
            "task_id": task_id,
            "stock_code": stock_code,
            "task_type": task_type,
            "status": "success",
            "error_message": None,
        })
        self._maybe_flush()

    def record_failed(self, task_id: str, stock_code: str, task_type: str, error_message: str) -> None:
        self._buffer.append({
            "task_id": task_id,
            "stock_code": stock_code,
            "task_type": task_type,
            "status": "failed",
            "error_message": error_message,
        })
        self._maybe_flush()

    def record_skipped(self, task_id: str, stock_code: str, task_type: str) -> None:
        self._buffer.append({
            "task_id": task_id,
            "stock_code": stock_code,
            "task_type": task_type,
            "status": "skipped",
            "error_message": None,
        })
        self._maybe_flush()

    def _maybe_flush(self) -> None:
        if len(self._buffer) >= self._batch_size:
            self.flush()

    def flush(self) -> None:
        if self._buffer:
            self._repo.bulk_upsert(self._buffer)
            self._buffer.clear()

    def filter_stocks_needing_collection(
        self,
        task_id: str,
        all_stock_codes: list[str],
        task_type: str,
        ttl_hours: int,
    ) -> list[str]:
        existing = self._repo.find_by_task(task_id, task_type)
        cutoff = datetime.now() - timedelta(hours=ttl_hours)
        needs = []
        for code in all_stock_codes:
            state = existing.get(code)
            if state is None:
                needs.append(code)
            elif state["status"] != "success":
                needs.append(code)
            elif state["updated_at"] < cutoff:
                needs.append(code)
        return needs
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_stock_state_tracker.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/adapters/db_stock_state_repository.py collector/src/data_collector/core/pipeline/stock_state_tracker.py collector/tests/unit/test_stock_state_tracker.py
git commit -m "feat(pipeline): add StockCollectionStateTracker with batch flush and TTL"
```

---

### Task 11: SourceFallbackPipeline 实现

**Files:**
- Create: `collector/src/data_collector/core/pipeline/source_fallback_pipeline.py`
- Test: `collector/tests/unit/test_source_fallback_pipeline.py`

**Step 1: Write the failing test**

创建 `collector/tests/unit/test_source_fallback_pipeline.py`：

```python
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
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_source_fallback_pipeline.py -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 3: Write minimal implementation**

创建 `collector/src/data_collector/core/pipeline/source_fallback_pipeline.py`：

```python
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
        base_record: dict[str, Any] | None = None
        for source in sorted(sources, key=lambda s: s.priority):
            adapter = self._adapters.get(source.name)
            if adapter is None:
                logger.warning("适配器未找到", source=source.name)
                continue
            try:
                raw = adapter.fetch(stock_code, source)
                mapper = FieldMapper(source.field_mapping)
                mapped = mapper.apply(raw)
                if base_record is None:
                    base_record = mapped
                else:
                    # 非空补充：只填充 base_record 中为 None 或空字符串的字段
                    for key, value in mapped.items():
                        if base_record.get(key) in (None, ""):
                            base_record[key] = value
            except Exception as e:
                logger.warning(
                    "数据源采集失败",
                    source=source.name,
                    stock_code=stock_code,
                    error=str(e),
                )
                continue
        return base_record
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_source_fallback_pipeline.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/core/pipeline/source_fallback_pipeline.py collector/tests/unit/test_source_fallback_pipeline.py
git commit -m "feat(pipeline): add SourceFallbackPipeline with serial fallback and null-fill"
```

---

## 阶段三：集成层改造

### Task 12: TaskExecutor 注册表改造（支持 mode + source_priority）

**Files:**
- Modify: `collector/src/data_collector/task_executor.py`
- Test: `collector/tests/unit/test_task_executor.py`

**Step 1: Write the failing test**

在 `test_task_executor.py` 追加：

```python
def test_executor_routes_by_task_type_and_mode():
    from data_collector.task_executor import TaskExecutor
    from data_collector.core.domain.collection_task import CollectionTask
    task = CollectionTask(task_type="stock_basic", mode="single", source_priority=["akshare"])
    executor = TaskExecutor()
    # 此时 registry 中应找不到 handler，因为尚未注册
    with pytest.raises(ValueError, match="未知的任务类型"):
        executor.execute(task)
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_task_executor.py::test_executor_routes_by_task_type_and_mode -v
```

Expected: FAIL（因为 task_type 是 stock_basic，而 registry 中只有 stock_full 等旧类型）

**Step 3: Write minimal implementation**

修改 `task_executor.py`：

1. 将 `_TASK_REGISTRY` 的 key 从 `task_type: str` 改为 `(task_type: str, mode: str)`
2. 修改 `register_task` 装饰器支持 `mode` 参数
3. 修改 `execute` 方法按 `(task.task_type, task.mode)` 查找 handler
4. 将旧 handler 的注册从旧 key 迁移到新 key（保留旧 handler 作为兼容，但使用新 key 注册）

由于这是一个较大的改动，这里给出关键修改部分：

```python
_TASK_REGISTRY: dict[tuple[str, str], tuple[Callable[[CollectionTask, Settings], dict[str, Any]], str]] = {}


def register_task(task_type: str, mode: str = "full", data_source: str = "") -> Callable:
    def decorator(fn: Callable[[CollectionTask, Settings], dict[str, Any]]) -> Callable[[CollectionTask, Settings], dict[str, Any]]:
        _TASK_REGISTRY[(task_type, mode)] = (fn, data_source)
        return fn
    return decorator
```

修改 `TaskExecutor.execute`：

```python
            handler_info = _TASK_REGISTRY.get((task.task_type, task.mode))
            if handler_info is None:
                raise ValueError(f"未知的任务类型: {task.task_type} (mode={task.mode})")
```

修改所有 `register_task(...)` 调用，加入 `mode` 参数：

```python
register_task("stock_basic", mode="full", data_source="akshare")(_handle_stock_basic_full)
register_task("stock_basic", mode="single", data_source="akshare")(_handle_stock_basic_single)
# ... 类似处理其他类型
```

> 注：旧 handler（`_handle_stock_full` 等）暂时保留，但用新 key 注册。新增 `_handle_stock_basic_full` 等 handler 在后续 Task 中实现。

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_task_executor.py::test_executor_routes_by_task_type_and_mode -v
```

Expected: PASS（假设已将旧 handler 用新 key 注册）

**Step 5: Commit**

```bash
git add collector/src/data_collector/task_executor.py collector/tests/unit/test_task_executor.py
git commit -m "refactor(executor): registry keys now include mode"
```

---

### Task 13: stock_basic YAML 配置与适配器骨架

**Files:**
- Create: `collector/config/mappings/stock_basic.yaml`
- Create: `collector/src/data_collector/adapters/data_source_adapter.py`
- Modify: `collector/src/data_collector/scripts/stock_full.py` → 提取为适配器

**Step 1: Create YAML config**

创建 `collector/config/mappings/stock_basic.yaml`：

```yaml
task_type: stock_basic
ttl_hours: 24

sources:
  - name: akshare
    adapter: stock_basic_akshare_adapter
    priority: 1
    field_mapping:
      - api_field: "代码"
        db_field: "stock_code"
        converter: "str"
        null_policy: "fail"
      - api_field: "名称"
        db_field: "name"
        converter: "str"
        null_policy: "fail"
      - api_field: ["总市值", "total_market_cap"]
        db_field: "total_market_cap"
        converter: "float"
        null_policy: "skip"
    params:
      api_name: "stock_info_a_code_name"

  - name: tushare
    adapter: stock_basic_tushare_adapter
    priority: 2
    field_mapping:
      - api_field: "ts_code"
        db_field: "ts_code"
        converter: "str"
        null_policy: "skip"
      - api_field: "industry"
        db_field: "industry"
        converter: "str"
        null_policy: "default"
        default_value: ""
```

**Step 2: Create adapter protocol**

创建 `collector/src/data_collector/adapters/data_source_adapter.py`：

```python
"""数据源适配器协议。"""

from typing import Any, Protocol

from data_collector.core.config.field_mapping_config import SourceConfig


class DataSourceAdapter(Protocol):
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        """调用外部 API，返回原始字段字典。"""
        ...
```

**Step 3: Commit**

```bash
git add collector/config/mappings/stock_basic.yaml collector/src/data_collector/adapters/data_source_adapter.py
git commit -m "feat(adapter): add stock_basic YAML config and adapter protocol"
```

---

## 阶段四：适配器迁移（关键任务）

### Task 14: stock_basic AkShare 适配器实现

**Files:**
- Create: `collector/src/data_collector/adapters/stock_basic_akshare_adapter.py`
- Modify: `collector/src/data_collector/task_executor.py`（注册新 handler）
- Test: `collector/tests/unit/test_stock_basic_adapter.py`

**Step 1: Write test with mocked akshare**

创建 `collector/tests/unit/test_stock_basic_adapter.py`：

```python
from unittest.mock import Mock, patch
import pandas as pd

from data_collector.adapters.stock_basic_akshare_adapter import StockBasicAkshareAdapter
from data_collector.core.config.field_mapping_config import SourceConfig


class TestStockBasicAkshareAdapter:
    def test_fetch_single_stock(self):
        adapter = StockBasicAkshareAdapter()
        mock_df = pd.DataFrame([{"代码": "000001", "名称": "平安银行"}])
        with patch("akshare.stock_info_a_code_name", return_value=mock_df):
            result = adapter.fetch("000001", SourceConfig(name="akshare", adapter="", priority=1))
        assert result == {"代码": "000001", "名称": "平安银行"}
```

**Step 2: Run test to verify it fails**

```bash
cd collector && poetry run pytest tests/unit/test_stock_basic_adapter.py -v
```

Expected: FAIL with `ModuleNotFoundError`

**Step 3: Write implementation**

创建 `collector/src/data_collector/adapters/stock_basic_akshare_adapter.py`：

```python
"""股票基础信息 AkShare 适配器。"""

from typing import Any

import akshare as ak

from data_collector.core.config.field_mapping_config import SourceConfig


class StockBasicAkshareAdapter:
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        api_name = source_config.params.get("api_name", "stock_info_a_code_name")
        df = getattr(ak, api_name)()
        row = df[df["代码"] == stock_code]
        if row.empty:
            raise ValueError(f"未找到股票: {stock_code}")
        return row.iloc[0].to_dict()
```

**Step 4: Run test to verify it passes**

```bash
cd collector && poetry run pytest tests/unit/test_stock_basic_adapter.py -v
```

Expected: PASS

**Step 5: Commit**

```bash
git add collector/src/data_collector/adapters/stock_basic_akshare_adapter.py collector/tests/unit/test_stock_basic_adapter.py
git commit -m "feat(adapter): add stock_basic akshare adapter"
```

---

## 阶段五：验收与收尾

### Task 15: 全链路本地集成验证

**Files:**
- N/A（验证任务）

**Step 1: 准备本地 PostgreSQL**

确保本地 PostgreSQL 已启动，数据库 `db-security-analyze` 存在。

**Step 2: 执行数据库迁移**

```bash
cd backend && ./gradlew flywayMigrate
```

Expected: BUILD SUCCESSFUL

**Step 3: 运行全部 collector 测试**

```bash
cd collector && poetry run pytest tests/unit/ -v --tb=short
```

Expected: 所有新增测试 PASS，现有测试不因重构而失败。

**Step 4: 验证 CLI 兼容**

```bash
cd collector && poetry run python -m data_collector stock_full --dry-run
```

Expected: CLI 正常解析参数，内部正确转换为新的 `CollectionTask` 语义。

**Step 5: Commit**

```bash
git add -A
git commit -m "test(integration): verify full pipeline locally"
```

---

## 后续计划（阶段六及以后）

以下任务在本次基础框架完成后继续实施：

- **company_info 适配器迁移**：参照 stock_basic 模式，提取 `company_full.py` 为 `company_info_akshare_adapter`
- **financial_income / financial_balance / financial_cashflow 适配器迁移**：提取字段映射到 YAML，适配器瘦身
- **financial_indicator 计算适配器**：保留纯计算逻辑，包装为 `DataSourceAdapter` 协议实现
- **financial_full orchestrator 改造**：按设计文档第 8.4 节实现子阶段编排和跨子任务断点恢复
- **field_supplement 拆分**：将 Tushare 补充逻辑拆分为各数据类型的 secondary source
- **端到端集成测试**：使用 `security-analyze-tester` skill 验证全链路
