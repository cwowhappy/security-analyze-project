import logging
from typing import Optional
from datetime import datetime

from collector.db.postgres import PostgresDB

logger = logging.getLogger(__name__)


class Monitor:
    """采集任务监控模块，负责记录任务执行日志和数据状态快照"""

    def __init__(self, db: PostgresDB):
        self.db = db

    def log_task_start(self, task_name: str, task_type: str) -> Optional[int]:
        """记录任务开始，返回 task_id"""
        try:
            result = self.db.execute_returning(
                """
                INSERT INTO collector_task_log (task_name, task_type, started_at, status)
                VALUES (%s, %s, NOW(), 'running')
                RETURNING id
                """,
                (task_name, task_type),
            )
            if result:
                logger.info(f"Task '{task_name}' started, log_id={result[0]}")
                return result[0]
        except Exception as e:
            logger.error(f"Failed to log task start for '{task_name}': {e}")
        return None

    def log_task_end(
        self,
        task_id: Optional[int],
        status: str,
        rows_affected: int = 0,
        error_message: Optional[str] = None,
    ):
        """记录任务结束并更新数据状态快照"""
        if task_id is None:
            logger.warning("Task id is None, skipping log_task_end")
            return

        try:
            self.db.execute(
                """
                UPDATE collector_task_log
                SET ended_at = NOW(), status = %s, rows_affected = %s, error_message = %s
                WHERE id = %s
                """,
                (status, rows_affected, error_message, task_id),
            )
            logger.info(
                f"Task log_id={task_id} ended with status='{status}', rows={rows_affected}"
            )
        except Exception as e:
            logger.error(f"Failed to log task end for log_id={task_id}: {e}")

    def upsert_data_status(self, data_type: str, total_rows: int, task_id: Optional[int]):
        """更新数据类型快照"""
        try:
            self.db.execute(
                """
                INSERT INTO collector_data_status (data_type, total_rows, last_updated_at, last_task_id)
                VALUES (%s, %s, NOW(), %s)
                ON CONFLICT (data_type) DO UPDATE SET
                    total_rows = EXCLUDED.total_rows,
                    last_updated_at = EXCLUDED.last_updated_at,
                    last_task_id = EXCLUDED.last_task_id,
                    updated_at = NOW()
                """,
                (data_type, total_rows, task_id),
            )
            logger.info(
                f"Data status updated: data_type='{data_type}', rows={total_rows}"
            )
        except Exception as e:
            logger.error(f"Failed to upsert data status for '{data_type}': {e}")


def _get_task_type(task_name: str) -> str:
    """根据任务名推断数据类型"""
    if "company" in task_name.lower():
        return "company"
    if "finance" in task_name.lower():
        return "finance_report"
    return "unknown"
