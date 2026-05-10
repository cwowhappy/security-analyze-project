"""FastAPI API 单元测试。"""

from datetime import datetime
from unittest.mock import MagicMock, patch

from fastapi.testclient import TestClient

from data_collector.api import app


class TestHealthEndpoint:
    """健康检查接口测试。"""

    def test_should_return_health_status(self) -> None:
        source = MagicMock()
        source.name = "akshare"
        source.is_available.return_value = True
        from data_collector.core.ports.data_source import SourceHealth, SourceStatus

        source.check_health.return_value = SourceHealth(
            status=SourceStatus.HEALTHY,
            latency_ms=10.0,
            error_rate=0.0,
            last_check="2026-01-01T00:00:00Z",
        )

        with patch("data_collector.api.execute_query") as mock_query:
            mock_query.return_value = [{"1": 1}]
            with patch("data_collector.api.init_pool"), \
                 patch("data_collector.api.CollectionScheduler") as MockScheduler, \
                 patch("data_collector.api.close_pool"), \
                 patch("data_collector.api.AkshareDataSource") as MockAkshare, \
                 patch("data_collector.api.TushareDataSource") as MockTushare:

                mock_scheduler = MagicMock()
                MockScheduler.return_value = mock_scheduler
                source.priority = 1
                MockAkshare.return_value = source
                mock_tushare = MagicMock()
                mock_tushare.priority = 2
                MockTushare.return_value = mock_tushare

                with TestClient(app) as client:
                    response = client.get("/health")

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["db_connected"] is True

    def test_should_return_degraded_when_source_down(self) -> None:
        source = MagicMock()
        source.name = "akshare"
        source.is_available.return_value = False
        from data_collector.core.ports.data_source import SourceHealth, SourceStatus

        source.check_health.return_value = SourceHealth(
            status=SourceStatus.UNAVAILABLE,
            latency_ms=0.0,
            error_rate=1.0,
            last_check="2026-01-01T00:00:00Z",
        )

        with patch("data_collector.api.execute_query") as mock_query:
            mock_query.return_value = [{"1": 1}]
            with patch("data_collector.api.init_pool"), \
                 patch("data_collector.api.CollectionScheduler") as MockScheduler, \
                 patch("data_collector.api.close_pool"), \
                 patch("data_collector.api.AkshareDataSource") as MockAkshare, \
                 patch("data_collector.api.TushareDataSource") as MockTushare:

                mock_scheduler = MagicMock()
                MockScheduler.return_value = mock_scheduler
                source.priority = 1
                MockAkshare.return_value = source
                mock_tushare = MagicMock()
                mock_tushare.priority = 2
                MockTushare.return_value = mock_tushare

                with TestClient(app) as client:
                    response = client.get("/health")

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "degraded"


class TestTasksEndpoint:
    """任务接口测试。"""

    def test_should_create_task(self) -> None:
        with patch("data_collector.api.init_pool"), \
             patch("data_collector.api.CollectionScheduler") as MockScheduler, \
             patch("data_collector.api.close_pool"):

            mock_scheduler = MagicMock()
            mock_scheduler.add_instant_task.return_value = "task-123"
            MockScheduler.return_value = mock_scheduler

            with TestClient(app) as client:
                response = client.post("/tasks", json={
                    "task_type": "stock_full",
                    "task_params": {"limit": 10},
                    "data_source": "akshare",
                })

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == "task-123"
        assert data["status"] == "submitted"

    def test_should_create_task_without_data_source(self) -> None:
        with patch("data_collector.api.init_pool"), \
             patch("data_collector.api.CollectionScheduler") as MockScheduler, \
             patch("data_collector.api.close_pool"):

            mock_scheduler = MagicMock()
            mock_scheduler.add_instant_task.return_value = "task-456"
            MockScheduler.return_value = mock_scheduler

            with TestClient(app) as client:
                response = client.post("/tasks", json={
                    "task_type": "company_full",
                })

        assert response.status_code == 200
        assert response.json()["id"] == "task-456"

    def test_should_list_tasks(self) -> None:
        with patch("data_collector.api.init_pool"), \
             patch("data_collector.api.CollectionScheduler") as MockScheduler, \
             patch("data_collector.api.close_pool"):

            MockScheduler.return_value = MagicMock()

            with patch("data_collector.api.DbCollectionTaskRepository") as MockRepo:
                mock_repo = MagicMock()
                mock_repo.find_all.return_value = [
                    MagicMock(
                        id="t1",
                        task_type="stock_full",
                        status="success",
                        data_source="akshare",
                        total_count=100,
                        success_count=100,
                        fail_count=0,
                        error_message=None,
                        started_at=datetime.now(),
                        completed_at=datetime.now(),
                    ),
                ]
                MockRepo.return_value = mock_repo
                with TestClient(app) as client:
                    response = client.get("/tasks?limit=10")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["id"] == "t1"
        assert data[0]["task_type"] == "stock_full"

    def test_should_get_task_by_id(self) -> None:
        with patch("data_collector.api.init_pool"), \
             patch("data_collector.api.CollectionScheduler") as MockScheduler, \
             patch("data_collector.api.close_pool"):

            MockScheduler.return_value = MagicMock()

            with patch("data_collector.api.DbCollectionTaskRepository") as MockRepo:
                mock_repo = MagicMock()
                mock_repo.find_by_id.return_value = MagicMock(
                    id="t1",
                    task_type="stock_full",
                    status="success",
                    data_source="akshare",
                    total_count=100,
                    success_count=100,
                    fail_count=0,
                    error_message=None,
                    started_at=datetime.now(),
                    completed_at=datetime.now(),
                )
                MockRepo.return_value = mock_repo
                with TestClient(app) as client:
                    response = client.get("/tasks/t1")

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == "t1"

    def test_should_return_404_for_missing_task(self) -> None:
        with patch("data_collector.api.init_pool"), \
             patch("data_collector.api.CollectionScheduler") as MockScheduler, \
             patch("data_collector.api.close_pool"):

            MockScheduler.return_value = MagicMock()

            with patch("data_collector.api.DbCollectionTaskRepository") as MockRepo:
                mock_repo = MagicMock()
                mock_repo.find_by_id.return_value = None
                MockRepo.return_value = mock_repo
                with TestClient(app) as client:
                    response = client.get("/tasks/notexist")

        assert response.status_code == 404
