"""APScheduler 调度器单元测试。"""

from datetime import datetime

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.scheduler import CollectionScheduler


class MockExecutor:
    """测试用任务执行器。"""

    def __init__(self) -> None:
        self.executed_tasks: list[CollectionTask] = []

    def execute(self, task: CollectionTask) -> CollectionTask:
        self.executed_tasks.append(task)
        task.status = "success"
        task.success_count = 1
        task.completed_at = datetime.now()
        return task


class MockTaskRepo:
    """测试用任务仓库。"""

    def __init__(self) -> None:
        self.saved: list[CollectionTask] = []
        self.updated: list[CollectionTask] = []

    def save(self, task: CollectionTask) -> None:
        self.saved.append(task)

    def update(self, task: CollectionTask) -> None:
        self.updated.append(task)

    def find_by_id(self, task_id: str) -> CollectionTask | None:
        return None

    def find_all(self, limit: int = 100) -> list[CollectionTask]:
        return []

    def find_schedules(self) -> list[dict]:
        return []


class TestCollectionScheduler:
    """CollectionScheduler 测试。"""

    def setup_method(self) -> None:
        self.executor = MockExecutor()
        self.task_repo = MockTaskRepo()
        self.scheduler = CollectionScheduler(
            executor=self.executor,
            task_repo=self.task_repo,
            settings=Settings(),
        )

    def test_should_start_and_shutdown(self) -> None:
        self.scheduler.start()
        assert self.scheduler.scheduler.running
        self.scheduler.shutdown(wait=False)
        assert not self.scheduler.scheduler.running

    def test_should_load_schedules(self) -> None:
        self.task_repo.find_schedules = lambda: [
            {
                "id": "s1",
                "name": "每日股票",
                "task_type": "stock_full",
                "task_params": None,
                "data_source": "akshare",
                "cron_expression": "0 9 * * *",
            },
        ]
        count = self.scheduler.load_schedules()
        assert count == 1
        job = self.scheduler.scheduler.get_job("schedule_s1")
        assert job is not None

    def test_should_add_instant_task(self) -> None:
        self.scheduler.start()
        task_id = self.scheduler.add_instant_task(
            task_type="stock_full",
            task_params={"limit": 10},
            data_source="akshare",
        )
        assert task_id is not None
        # 等待任务执行完成
        import time

        time.sleep(0.5)
        self.scheduler.shutdown(wait=False)

    def test_should_run_scheduled_task(self) -> None:
        self.scheduler.start()
        self.scheduler._run_scheduled_task(
            task_type="stock_full",
            task_params={},
            data_source="akshare",
        )
        assert len(self.task_repo.saved) == 1
        assert len(self.task_repo.updated) == 1
        # save 时的状态被 executor 修改为 success（同一对象引用）
        assert self.task_repo.updated[0].status == "success"

    def test_should_handle_load_schedule_error(self) -> None:
        self.task_repo.find_schedules = lambda: [
            {
                "id": "bad",
                "name": "错误规则",
                "task_type": "stock_full",
                "cron_expression": "invalid_cron",
            },
        ]
        count = self.scheduler.load_schedules()
        assert count == 0

    def test_should_replace_existing_job(self) -> None:
        self.task_repo.find_schedules = lambda: [
            {
                "id": "s1",
                "name": "每日股票",
                "task_type": "stock_full",
                "cron_expression": "0 9 * * *",
            },
        ]
        self.scheduler.load_schedules()
        # 再次加载应替换现有 job 不报错
        self.scheduler.load_schedules()
        job = self.scheduler.scheduler.get_job("schedule_s1")
        assert job is not None
