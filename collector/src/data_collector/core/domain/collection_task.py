"""采集任务领域模型，与 tb_collection_task 表结构对应。"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum


class TaskStatus(str, Enum):
    """任务状态枚举。"""

    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"


class TaskType(str, Enum):
    """任务类型枚举。

    以数据类型为维度，组合操作模式：
    - stock_full / stock_single
    - company_full / company_single
    """

    STOCK_FULL = "stock_full"
    STOCK_SINGLE = "stock_single"
    COMPANY_FULL = "company_full"
    COMPANY_SINGLE = "company_single"


@dataclass
class CollectionTask:
    """采集任务执行记录领域实体。"""

    id: str | None = None
    task_type: str = ""
    task_params: dict = field(default_factory=dict)
    status: str = TaskStatus.PENDING.value
    data_source: str | None = None
    total_count: int = 0
    success_count: int = 0
    fail_count: int = 0
    scheduled_at: datetime | None = None
    error_message: str | None = None
    started_at: datetime | None = None
    completed_at: datetime | None = None
    created_at: datetime | None = None

    def __post_init__(self) -> None:
        if not self.task_type:
            raise ValueError("任务类型 task_type 不能为空")

    def to_dict(self) -> dict:
        """转换为字典（用于入库）。"""
        import json

        return {
            "id": self.id,
            "task_type": self.task_type,
            "task_params": json.dumps(self.task_params) if self.task_params else None,
            "status": self.status,
            "data_source": self.data_source,
            "total_count": self.total_count,
            "success_count": self.success_count,
            "fail_count": self.fail_count,
            "scheduled_at": self.scheduled_at,
            "error_message": self.error_message,
            "started_at": self.started_at,
            "completed_at": self.completed_at,
            "created_at": self.created_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "CollectionTask":
        """从字典创建实例（从数据库读取）。"""
        import json

        task_params = data.get("task_params")
        if isinstance(task_params, str):
            task_params = json.loads(task_params)
        elif task_params is None:
            task_params = {}

        return cls(
            id=data.get("id"),
            task_type=data.get("task_type", ""),
            task_params=task_params,
            status=data.get("status", TaskStatus.PENDING.value),
            data_source=data.get("data_source"),
            total_count=data.get("total_count", 0),
            success_count=data.get("success_count", 0),
            fail_count=data.get("fail_count", 0),
            scheduled_at=data.get("scheduled_at"),
            error_message=data.get("error_message"),
            started_at=data.get("started_at"),
            completed_at=data.get("completed_at"),
            created_at=data.get("created_at"),
        )
