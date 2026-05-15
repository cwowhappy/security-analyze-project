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
            self._repo.bulk_upsert(list(self._buffer))
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
