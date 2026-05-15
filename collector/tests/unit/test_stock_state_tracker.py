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
