"""PostgreSQL 采集任务仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.collection_task import CollectionTask
from data_collector.infrastructure.db import execute_query, execute_update

logger = structlog.get_logger(__name__)


class DbCollectionTaskRepository:
    """基于 PostgreSQL 的采集任务仓库实现。"""

    def save(self, task: CollectionTask) -> None:
        """保存任务记录。"""
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
        """更新任务记录。"""
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

    def find_by_id(self, task_id: str) -> CollectionTask | None:
        """根据 ID 查询任务。"""
        sql = "SELECT * FROM tb_collection_task WHERE id = %s"
        rows = execute_query(sql, (task_id,))
        if not rows:
            return None
        return CollectionTask.from_dict(rows[0])

    def find_all(self, limit: int = 100) -> Sequence[CollectionTask]:
        """查询最近的任务列表。"""
        sql = """
        SELECT * FROM tb_collection_task
        ORDER BY created_at DESC
        LIMIT %s
        """
        rows = execute_query(sql, (limit,))
        return [CollectionTask.from_dict(row) for row in rows]

    def find_pending(self) -> Sequence[CollectionTask]:
        """查询所有待执行的任务。"""
        sql = """
        SELECT * FROM tb_collection_task
        WHERE status = 'pending'
        ORDER BY created_at ASC
        """
        rows = execute_query(sql)
        return [CollectionTask.from_dict(row) for row in rows]
