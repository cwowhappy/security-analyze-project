"""Stock 级采集状态仓库。"""

from collections.abc import Sequence
from datetime import datetime

import structlog
import ulid

from data_collector.infrastructure.db import execute_query, transaction

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
        with transaction() as conn:
            cursor = conn.cursor()
            cursor.executemany(sql, params)
            cursor.close()
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
