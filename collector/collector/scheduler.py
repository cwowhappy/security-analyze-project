import logging
from typing import Optional, Callable

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.jobstores.sqlalchemy import SQLAlchemyJobStore
from apscheduler.executors.pool import ThreadPoolExecutor as APThreadPoolExecutor
from apscheduler.triggers.cron import CronTrigger

from collector.config import DBConfig
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.company_task import CompanyTask
from collector.monitor import Monitor

logger = logging.getLogger(__name__)

# 默认 Job 配置
DEFAULT_JOB_DEFAULTS = {
    "coalesce": True,
    "max_instances": 1,
    "misfire_grace_time": 3600,
}

DEFAULT_EXECUTORS = {
    "default": APThreadPoolExecutor(max_workers=2),
}


class Scheduler:
    """采集任务调度器（基于 APScheduler + PostgreSQL JobStore）"""

    def __init__(self, db: PostgresDB = None, db_cfg: Optional[DBConfig] = None):
        self.db = db
        self.monitor = Monitor(db) if db else None
        self._scheduler: Optional[BackgroundScheduler] = None

        jobstores = {}
        if db_cfg:
            db_url = (
                f"postgresql+psycopg://{db_cfg.user}:{db_cfg.password}"
                f"@{db_cfg.host}:{db_cfg.port}/{db_cfg.database}"
            )
            jobstores["default"] = SQLAlchemyJobStore(url=db_url)
            logger.info("APScheduler SQLAlchemyJobStore configured")

        self._scheduler = BackgroundScheduler(
            jobstores=jobstores,
            executors=DEFAULT_EXECUTORS,
            job_defaults=DEFAULT_JOB_DEFAULTS,
        )

    # ------------------------------------------------------------------
    # 生命周期
    # ------------------------------------------------------------------
    def start(self):
        self._scheduler.start()
        logger.info("Scheduler started")

    def stop(self):
        if self._scheduler:
            self._scheduler.shutdown(wait=True)
            logger.info("Scheduler stopped")

    # ------------------------------------------------------------------
    # 通用 Job 注册（新增）
    # ------------------------------------------------------------------
    def register(
        self, job_id: str, name: str, cron: str, func: Callable, **kwargs
    ) -> bool:
        """通用 Job 注册方法。

        Args:
            job_id: 任务唯一标识
            name: 任务名称
            cron: Cron 表达式
            func: 执行函数
            **kwargs: 传给 func 的额外关键字参数
        """
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    func,
                    trigger=trigger,
                    id=job_id,
                    name=name,
                    replace_existing=True,
                    kwargs=kwargs,
                )
                logger.info(f"Added job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to register job {job_id}: {e}")
            return False

    # ------------------------------------------------------------------
    # 定时任务管理（向后兼容）
    # ------------------------------------------------------------------
    def add_company_job(self, cron: str, job_id: str = "company_task") -> bool:
        def _run():
            try:
                source = AkshareSource()
                task = CompanyTask(db=self.db, source=source, monitor=self.monitor)
                task.run()
            except Exception as e:
                logger.error(f"Scheduled company task failed: {e}")
        return self.register(job_id, "Full Company Sync", cron, _run)

    def add_finance_job(self, cron: str, job_id: str = "finance_task") -> bool:
        def _run():
            try:
                from collector.tasks.finance_task import FinanceTask
                source = AkshareSource()
                task = FinanceTask(db=self.db, source=source, monitor=self.monitor)
                task.run()
            except Exception as e:
                logger.error(f"Scheduled finance task failed: {e}")
        return self.register(job_id, "Full Finance Sync", cron, _run)

    def add_industry_sync_job(
        self, cron: str, job_id: str = "industry_sync_task"
    ) -> bool:
        def _run():
            try:
                from collector.tasks.industry_classification_sync import (
                    run as run_industry_sync,
                )
                run_industry_sync(db=self.db)
            except Exception as e:
                logger.error(f"Scheduled industry sync task failed: {e}")
        return self.register(job_id, "Industry Classification Sync", cron, _run)

    def add_quote_job(self, cron: str, job_id: str = "quote_task") -> bool:
        def _run():
            try:
                from collector.tasks.quote_task import QuoteTask
                source = AkshareSource()
                task = QuoteTask(db=self.db, source=source)
                task.run()
            except Exception as e:
                logger.error(f"Scheduled quote task failed: {e}")
        return self.register(job_id, "Daily Quote Sync", cron, _run)

    def add_index_basic_job(
        self, cron: str, job_id: str = "index_basic_task"
    ) -> bool:
        def _run():
            try:
                from collector.tasks.index_basic_task import IndexBasicTask
                source = AkshareSource()
                task = IndexBasicTask(db=self.db, source=source, monitor=self.monitor)
                task.run()
            except Exception as e:
                logger.error(f"Scheduled index basic task failed: {e}")
        return self.register(job_id, "Index Basic Sync", cron, _run)

    def add_index_history_job(
        self, cron: str, job_id: str = "index_history_task"
    ) -> bool:
        def _run():
            try:
                from collector.tasks.index_history_task import IndexHistoryTask
                source = AkshareSource()
                task = IndexHistoryTask(db=self.db, source=source, monitor=self.monitor)
                task.run()
            except Exception as e:
                logger.error(f"Scheduled index history task failed: {e}")
        return self.register(job_id, "Index History Sync", cron, _run)

    def add_etf_basic_job(
        self, cron: str, job_id: str = "etf_basic_task"
    ) -> bool:
        def _run():
            try:
                from collector.tasks.etf_basic_task import EtfBasicTask
                source = AkshareSource()
                task = EtfBasicTask(db=self.db, source=source, monitor=self.monitor)
                task.run()
            except Exception as e:
                logger.error(f"Scheduled ETF basic task failed: {e}")
        return self.register(job_id, "ETF Basic Sync", cron, _run)

    def remove_job(self, job_id: str) -> bool:
        try:
            self._scheduler.remove_job(job_id)
            logger.info(f"Removed job '{job_id}'")
            return True
        except Exception as e:
            logger.error(f"Failed to remove job '{job_id}': {e}")
            return False

    def pause_job(self, job_id: str) -> bool:
        try:
            self._scheduler.pause_job(job_id)
            logger.info(f"Paused job '{job_id}'")
            return True
        except Exception as e:
            logger.error(f"Failed to pause job '{job_id}': {e}")
            return False

    def resume_job(self, job_id: str) -> bool:
        try:
            self._scheduler.resume_job(job_id)
            logger.info(f"Resumed job '{job_id}'")
            return True
        except Exception as e:
            logger.error(f"Failed to resume job '{job_id}': {e}")
            return False

    def list_jobs(self):
        """返回当前所有 job 的简要信息"""
        jobs = self._scheduler.get_jobs()
        return [
            {
                "id": job.id,
                "name": job.name,
                "next_run_time": str(job.next_run_time) if job.next_run_time else None,
            }
            for job in jobs
        ]

    # ------------------------------------------------------------------
    # 手动触发（向后兼容）
    # ------------------------------------------------------------------
    def run_company_task_now(self):
        logger.info("Manual trigger: full company task")
        self._run_company_task()

    def run_company_task_by_name(self, query: str):
        logger.info(f"Manual trigger: company task by name '{query}'")
        try:
            source = AkshareSource()
            task = CompanyTask(db=self.db, source=source, monitor=self.monitor)
            task.run_by_name(query)
        except Exception as e:
            logger.error(f"Company task by name failed: {e}")

    def run_industry_sync_task_now(self):
        logger.info("Manual trigger: industry classification sync task")
        self._run_industry_sync_task()

    def run_quote_task_now(self, trade_date: Optional[str] = None):
        logger.info(f"Manual trigger: quote task, trade_date={trade_date or 'today'}")
        try:
            from collector.tasks.quote_task import QuoteTask
            source = AkshareSource()
            task = QuoteTask(db=self.db, source=source)
            task.run(trade_date=trade_date)
        except Exception as e:
            logger.error(f"Quote task failed: {e}")

    def run_index_basic_task_now(self):
        logger.info("Manual trigger: full index basic task")
        self._run_index_basic_task()

    def run_index_history_task_now(self):
        logger.info("Manual trigger: full index history task")
        self._run_index_history_task()

    def run_etf_basic_task_now(self):
        logger.info("Manual trigger: full ETF basic task")
        self._run_etf_basic_task()

    def _run_company_task(self):
        logger.info("Running scheduled company task...")
        try:
            source = AkshareSource()
            task = CompanyTask(db=self.db, source=source, monitor=self.monitor)
            task.run()
        except Exception as e:
            logger.error(f"Scheduled company task failed: {e}")

    def _run_finance_task(self):
        logger.info("Running scheduled finance task...")
        try:
            from collector.tasks.finance_task import FinanceTask
            source = AkshareSource()
            task = FinanceTask(db=self.db, source=source, monitor=self.monitor)
            task.run()
        except Exception as e:
            logger.error(f"Scheduled finance task failed: {e}")

    def _run_industry_sync_task(self):
        logger.info("Running scheduled industry classification sync task...")
        try:
            from collector.tasks.industry_classification_sync import (
                run as run_industry_sync,
            )
            run_industry_sync(db=self.db)
        except Exception as e:
            logger.error(f"Scheduled industry sync task failed: {e}")

    def _run_quote_task(self):
        logger.info("Running scheduled quote task...")
        try:
            from collector.tasks.quote_task import QuoteTask
            source = AkshareSource()
            task = QuoteTask(db=self.db, source=source)
            task.run()
        except Exception as e:
            logger.error(f"Scheduled quote task failed: {e}")

    def _run_index_basic_task(self):
        logger.info("Running scheduled index basic task...")
        try:
            from collector.tasks.index_basic_task import IndexBasicTask
            source = AkshareSource()
            task = IndexBasicTask(db=self.db, source=source, monitor=self.monitor)
            task.run()
        except Exception as e:
            logger.error(f"Scheduled index basic task failed: {e}")

    def _run_index_history_task(self):
        logger.info("Running scheduled index history task...")
        try:
            from collector.tasks.index_history_task import IndexHistoryTask
            source = AkshareSource()
            task = IndexHistoryTask(db=self.db, source=source, monitor=self.monitor)
            task.run()
        except Exception as e:
            logger.error(f"Scheduled index history task failed: {e}")

    def _run_etf_basic_task(self):
        logger.info("Running scheduled ETF basic task...")
        try:
            from collector.tasks.etf_basic_task import EtfBasicTask
            source = AkshareSource()
            task = EtfBasicTask(db=self.db, source=source, monitor=self.monitor)
            task.run()
        except Exception as e:
            logger.error(f"Scheduled ETF basic task failed: {e}")
