"""采集任务抽象基类，统一执行粒度与生命周期。"""
from abc import ABC, abstractmethod
from typing import Any, Optional, Set
import logging

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor

logger = logging.getLogger(__name__)


class TaskResult:
    """统一任务执行结果。"""

    def __init__(self, created: int = 0, updated: int = 0, failed: int = 0, rows: int = 0):
        self.created = created
        self.updated = updated
        self.failed = failed
        self.rows = rows

    def __repr__(self):
        return (
            f"TaskResult(created={self.created}, updated={self.updated}, "
            f"failed={self.failed}, rows={self.rows})"
        )


class BaseTask(ABC):
    """
    采集任务抽象基类。

    所有具体任务必须实现：
      - task_name: 任务标识字符串
      - data_type: 数据类型标识（用于 monitor 数据快照）
      - run_full(): 全量采集
      - run_partial(identifiers): 指定范围采集
      - run_incremental(): 增量采集
    """

    task_name: str = ""
    data_type: str = ""

    def __init__(
        self,
        db: PostgresDB,
        source: BaseDataSource,
        monitor: Optional[Monitor] = None,
    ):
        self.db = db
        self.source = source
        self.monitor = monitor

    # ------------------------------------------------------------------
    # 公共入口
    # ------------------------------------------------------------------
    def execute(self, mode: str = "full", **kwargs) -> TaskResult:
        """
        统一执行入口。

        Args:
            mode: "full" | "partial" | "incremental" | "resume"
            **kwargs: 各模式所需的额外参数
        """
        session_id = kwargs.get("session_id")
        if session_id:
            mode = "resume"

        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start(
                self.task_name, self.data_type, session_id=session_id
            )

        result = TaskResult()
        try:
            if mode == "full":
                result = self.run_full(**kwargs)
            elif mode == "partial":
                result = self.run_partial(**kwargs)
            elif mode == "incremental":
                result = self.run_incremental(**kwargs)
            elif mode == "resume":
                result = self.resume_session(**kwargs)
            else:
                raise ValueError(f"Unknown execution mode: {mode}")

            if self.monitor:
                self.monitor.log_task_end(task_id, "success", result.rows)
                self.monitor.upsert_data_status(self.data_type, result.rows, task_id)
        except Exception as e:
            logger.error(f"Task {self.task_name} failed: {e}")
            if self.monitor:
                self.monitor.log_task_end(
                    task_id, "failed", error_message=str(e)
                )
            raise

        return result

    # ------------------------------------------------------------------
    # 子类必须实现
    # ------------------------------------------------------------------
    @abstractmethod
    def run_full(self, **kwargs) -> TaskResult:
        """全量采集。"""
        ...

    @abstractmethod
    def run_partial(self, identifiers: list, **kwargs) -> TaskResult:
        """指定范围采集（如指定股票代码列表、指数代码列表等）。"""
        ...

    @abstractmethod
    def run_incremental(self, **kwargs) -> TaskResult:
        """增量采集。"""
        ...

    def resume_session(self, session_id: str, **kwargs) -> TaskResult:
        """
        从 Session 断点恢复。默认实现基于 Monitor 的 progress 表跳过已成功的记录。
        子类可覆盖以支持更复杂的恢复逻辑。
        """
        success_set: Set[str] = set()
        if self.monitor:
            success_set = self.monitor.get_session_progress(session_id)
        kwargs["exclude_set"] = success_set
        kwargs["session_id"] = session_id
        return self.run_full(**kwargs)
