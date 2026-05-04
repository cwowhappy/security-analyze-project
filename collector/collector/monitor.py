import json
import logging
from typing import Optional, Set, Dict, Any
from datetime import datetime

from collector.db.postgres import PostgresDB

logger = logging.getLogger(__name__)


class Monitor:
    """采集任务监控模块，负责记录任务执行日志和数据状态快照"""

    def __init__(self, db: PostgresDB):
        self.db = db

    def log_task_start(self, task_name: str, task_type: str, session_id: Optional[str] = None, params: Optional[Dict[str, Any]] = None) -> Optional[int]:
        """记录任务开始，返回 task_id

        Args:
            task_name: 任务名称
            task_type: 任务类型
            session_id: 可选的 Session UUID（用于故障恢复）
            params: 可选的任务参数快照（JSONB），恢复时无需重新输入
        """
        try:
            if session_id:
                result = self.db.execute_returning(
                    """
                    INSERT INTO collector_task_log (task_name, task_type, started_at, status, session_id, params)
                    VALUES (%s, %s, NOW(), 'running', %s, %s)
                    RETURNING id
                    """,
                    (task_name, task_type, session_id, json.dumps(params, ensure_ascii=False, default=str) if params else None),
                )
            else:
                result = self.db.execute_returning(
                    """
                    INSERT INTO collector_task_log (task_name, task_type, started_at, status)
                    VALUES (%s, %s, NOW(), 'running')
                    RETURNING id
                    """,
                    (task_name, task_type),
                )
            if result:
                logger.info(f"Task '{task_name}' started, log_id={result[0]}, session_id={session_id}")
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

    def get_session_progress(self, session_id: str) -> Set[str]:
        """获取指定 Session 下已处理成功的股票代码集合"""
        try:
            rows = self.db.fetchall(
                "SELECT stock_code FROM collector_task_progress WHERE session_id = %s AND status = 'success'",
                (session_id,),
            )
            return {row[0] for row in rows if row and row[0]}
        except Exception as e:
            logger.error(f"Failed to get session progress for {session_id}: {e}")
            return set()

    def log_task_progress(self, session_id: str, stock_code: str, status: str, rows_created: int = 0, rows_updated: int = 0, error_message: Optional[str] = None):
        """记录单只股票的处理进度（Upsert）"""
        try:
            self.db.execute(
                """
                INSERT INTO collector_task_progress
                    (session_id, stock_code, status, rows_created, rows_updated, error_message, started_at, ended_at)
                VALUES (%s, %s, %s, %s, %s, %s, NOW(), NOW())
                ON CONFLICT (session_id, stock_code) DO UPDATE SET
                    status = EXCLUDED.status,
                    rows_created = EXCLUDED.rows_created,
                    rows_updated = EXCLUDED.rows_updated,
                    error_message = EXCLUDED.error_message,
                    ended_at = EXCLUDED.ended_at
                """,
                (session_id, stock_code, status, rows_created, rows_updated, error_message),
            )
        except Exception as e:
            logger.error(f"Failed to log task progress for {session_id}/{stock_code}: {e}")

    def get_session_params(self, session_id: str) -> Optional[Dict[str, Any]]:
        """获取指定 Session 的任务参数"""
        try:
            row = self.db.fetchone(
                "SELECT params FROM collector_task_log WHERE session_id = %s",
                (session_id,),
            )
            if row and row[0]:
                if isinstance(row[0], dict):
                    return row[0]
                if isinstance(row[0], str):
                    return json.loads(row[0])
            return None
        except Exception as e:
            logger.error(f"Failed to get session params for {session_id}: {e}")
            return None

    def get_task_id_by_session(self, session_id: str) -> Optional[int]:
        """根据 Session ID 查找对应的 Task Log ID"""
        try:
            row = self.db.fetchone(
                "SELECT id FROM collector_task_log WHERE session_id = %s",
                (session_id,),
            )
            return row[0] if row else None
        except Exception as e:
            logger.error(f"Failed to get task_id by session {session_id}: {e}")
            return None

    def update_task_status(self, task_id: int, status: str):
        """更新任务状态（用于恢复时重置为 running）"""
        try:
            self.db.execute(
                """
                UPDATE collector_task_log
                SET status = %s, ended_at = NULL, error_message = NULL, rows_affected = 0
                WHERE id = %s
                """,
                (status, task_id),
            )
        except Exception as e:
            logger.error(f"Failed to update task status for log_id={task_id}: {e}")



