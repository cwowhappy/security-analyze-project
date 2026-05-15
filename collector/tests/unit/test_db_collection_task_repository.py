"""PostgreSQL 采集任务仓库单元测试。"""

import pytest
from unittest.mock import patch

from data_collector.adapters.db_collection_task_repository import DbCollectionTaskRepository
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.config import Settings
from data_collector.infrastructure.db import init_pool, close_pool


@pytest.fixture(autouse=True)
def db_pool():
    """初始化数据库连接池（集成测试需要）。"""
    settings = Settings()
    init_pool(settings)
    yield
    close_pool()


@pytest.fixture
def repo():
    return DbCollectionTaskRepository()


class TestDbCollectionTaskRepository:
    """DbCollectionTaskRepository 测试。"""

    def setup_method(self) -> None:
        self.repo = DbCollectionTaskRepository()

    def test_should_save_task(self) -> None:
        with patch("data_collector.adapters.db_collection_task_repository.execute_update") as mock_update:
            mock_update.return_value = 1
            task = CollectionTask(task_type="stock_full", task_params={"limit": 100})
            self.repo.save(task)
            assert task.id is not None
            mock_update.assert_called_once()
            sql = mock_update.call_args[0][0]
            assert "INSERT INTO tb_collection_task" in sql

    def test_should_update_task(self) -> None:
        with patch("data_collector.adapters.db_collection_task_repository.execute_update") as mock_update:
            mock_update.return_value = 1
            task = CollectionTask(id="test-id", task_type="stock_full", status="success")
            self.repo.update(task)
            mock_update.assert_called_once()
            sql = mock_update.call_args[0][0]
            assert "UPDATE tb_collection_task" in sql

    def test_should_find_by_id(self) -> None:
        with patch("data_collector.adapters.db_collection_task_repository.execute_query") as mock_query:
            mock_query.return_value = [{
                "id": "test-id",
                "task_type": "stock_full",
                "status": "success",
                "task_params": '{"limit": 100}',
            }]
            result = self.repo.find_by_id("test-id")
            assert result is not None
            assert result.id == "test-id"
            assert result.task_params == {"limit": 100}

    def test_should_return_none_when_id_not_found(self) -> None:
        with patch("data_collector.adapters.db_collection_task_repository.execute_query") as mock_query:
            mock_query.return_value = []
            result = self.repo.find_by_id("notexist")
            assert result is None

    def test_should_find_all(self) -> None:
        with patch("data_collector.adapters.db_collection_task_repository.execute_query") as mock_query:
            mock_query.return_value = [
                {"id": "1", "task_type": "stock_full", "status": "success", "task_params": None},
                {"id": "2", "task_type": "company_full", "status": "failed", "task_params": None},
            ]
            results = self.repo.find_all(limit=10)
            assert len(results) == 2

    def test_should_find_pending(self) -> None:
        with patch("data_collector.adapters.db_collection_task_repository.execute_query") as mock_query:
            mock_query.return_value = [
                {"id": "1", "task_type": "stock_full", "status": "pending", "task_params": None},
                {"id": "2", "task_type": "company_full", "status": "pending", "task_params": None},
            ]
            results = self.repo.find_pending()
            assert len(results) == 2
            assert results[0].status == "pending"


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
