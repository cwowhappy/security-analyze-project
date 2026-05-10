"""APScheduler 调度引擎：定时任务加载、执行、状态记录。"""

from datetime import datetime

import structlog
from apscheduler.events import EVENT_JOB_ERROR, EVENT_JOB_EXECUTED, EVENT_JOB_SUBMITTED
from apscheduler.executors.pool import ThreadPoolExecutor
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

from data_collector.adapters.db_collection_task_repository import DbCollectionTaskRepository
from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.task_executor import TaskExecutor

logger = structlog.get_logger(__name__)


class CollectionScheduler:
    """采集任务调度器。

    封装 APScheduler BackgroundScheduler，负责：
    - 从数据库加载定时规则
    - 注册 CronTrigger Job
    - 监听事件并记录任务状态
    """

    def __init__(
        self,
        executor: TaskExecutor,
        task_repo: DbCollectionTaskRepository,
        settings: Settings | None = None,
    ) -> None:
        self._executor = executor
        self._task_repo = task_repo
        self._settings = settings or Settings()
        self._scheduler = self._create_scheduler()
        self._setup_listeners()

    def _create_scheduler(self) -> BackgroundScheduler:
        """创建并配置 APScheduler。"""
        executors = {
            "default": ThreadPoolExecutor(max_workers=self._settings.collector_max_workers),
        }
        job_defaults = {
            "coalesce": True,
            "max_instances": 1,
            "misfire_grace_time": self._settings.collector_misfire_grace_time,
        }
        return BackgroundScheduler(
            executors=executors,
            job_defaults=job_defaults,
        )

    def _setup_listeners(self) -> None:
        """注册 APScheduler 事件监听器。"""
        self._scheduler.add_listener(self._on_job_submitted, EVENT_JOB_SUBMITTED)
        self._scheduler.add_listener(self._on_job_executed, EVENT_JOB_EXECUTED)
        self._scheduler.add_listener(self._on_job_error, EVENT_JOB_ERROR)

    def start(self) -> None:
        """启动调度器并加载定时规则。"""
        self._scheduler.start()
        self.load_schedules()
        logger.info("APScheduler 已启动")

    def shutdown(self, wait: bool = True) -> None:
        """关闭调度器。"""
        self._scheduler.shutdown(wait=wait)
        logger.info("APScheduler 已关闭")

    def load_schedules(self) -> int:
        """从数据库加载启用的定时规则并注册为 Job。

        Returns:
            成功加载的规则数量。
        """
        schedules = self._task_repo.find_schedules()
        count = 0
        for schedule in schedules:
            try:
                self._add_cron_job(schedule)
                count += 1
            except Exception as e:
                logger.warning(
                    "加载定时规则失败",
                    schedule_id=schedule.get("id"),
                    cron=schedule.get("cron_expression"),
                    error=str(e),
                )
        logger.info("定时规则加载完成", loaded=count, total=len(schedules))
        return count

    def _add_cron_job(self, schedule: dict) -> None:
        """将单条定时规则注册为 Cron Job。"""
        job_id = f"schedule_{schedule['id']}"
        # 避免重复添加
        if self._scheduler.get_job(job_id):
            self._scheduler.remove_job(job_id)

        self._scheduler.add_job(
            func=self._run_scheduled_task,
            trigger=CronTrigger.from_crontab(schedule["cron_expression"]),
            id=job_id,
            replace_existing=True,
            args=[
                schedule["task_type"],
                schedule.get("task_params") or {},
                schedule.get("data_source"),
            ],
        )
        logger.debug(
            "定时规则已注册",
            job_id=job_id,
            task_type=schedule["task_type"],
            cron=schedule["cron_expression"],
        )

    def add_instant_task(
        self,
        task_type: str,
        task_params: dict | None = None,
        data_source: str | None = None,
    ) -> str:
        """添加即时执行的任务。

        Returns:
            任务 ID。
        """
        import ulid

        task_id = str(ulid.ULID())
        self._scheduler.add_job(
            func=self._run_scheduled_task,
            trigger="date",
            run_date=datetime.now(),
            id=f"instant_{task_id}",
            args=[task_type, task_params or {}, data_source],
        )
        logger.info("即时任务已添加", task_id=task_id, task_type=task_type)
        return task_id

    def _run_scheduled_task(
        self,
        task_type: str,
        task_params: dict,
        data_source: str | None,
    ) -> None:
        """Job 执行函数：创建任务记录并调用执行器。"""
        import ulid

        task = CollectionTask(
            id=str(ulid.ULID()),
            task_type=task_type,
            task_params=task_params,
            data_source=data_source,
            scheduled_at=datetime.now(),
        )
        # 先保存 running 状态
        self._task_repo.save(task)

        # 执行
        result = self._executor.execute(task)

        # 更新结果
        self._task_repo.update(result)

    def _on_job_submitted(self, event) -> None:
        """任务提交事件。"""
        logger.debug("Job 已提交", job_id=event.job_id)

    def _on_job_executed(self, event) -> None:
        """任务成功执行事件。"""
        logger.debug("Job 执行成功", job_id=event.job_id, retval=event.retval)

    def _on_job_error(self, event) -> None:
        """任务执行失败事件。"""
        logger.error(
            "Job 执行异常",
            job_id=event.job_id,
            exception=event.exception,
        )

    @property
    def scheduler(self) -> BackgroundScheduler:
        """返回底层 APScheduler 实例（用于健康检查等）。"""
        return self._scheduler
