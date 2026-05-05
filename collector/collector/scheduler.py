import logging
from typing import Optional

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
    "coalesce": True,      # 错过的任务只执行一次
    "max_instances": 1,    # 同一任务同时只能有一个实例在运行
    "misfire_grace_time": 3600,
}

DEFAULT_EXECUTORS = {
    "default": APThreadPoolExecutor(max_workers=2),
}


class Scheduler:
    """采集任务调度器（基于 APScheduler + PostgreSQL JobStore）"""

    def __init__(self, db: PostgresDB, db_cfg: Optional[DBConfig] = None):
        self.db = db
        self.monitor = Monitor(db)
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
    # 定时任务管理
    # ------------------------------------------------------------------
    def add_company_job(self, cron: str, job_id: str = "company_task") -> bool:
        """添加/更新公司信息采集定时任务

        Args:
            cron: Cron 表达式，例如 "0 9 * * *"（每天 09:00）
            job_id: 任务唯一标识
        """
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled company job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    self._run_company_task,
                    trigger=trigger,
                    id=job_id,
                    name="Full Company Sync",
                    replace_existing=True,
                )
                logger.info(f"Added company job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to add company job: {e}")
            return False

    def add_finance_job(self, cron: str, job_id: str = "finance_task") -> bool:
        """添加/更新财务报告采集定时任务"""
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled finance job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    self._run_finance_task,
                    trigger=trigger,
                    id=job_id,
                    name="Full Finance Sync",
                    replace_existing=True,
                )
                logger.info(f"Added finance job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to add finance job: {e}")
            return False

    def add_industry_sync_job(self, cron: str, job_id: str = "industry_sync_task") -> bool:
        """添加/更新行业分类同步定时任务

        Args:
            cron: Cron 表达式，例如 "0 3 * * 1"（每周一 03:00）
            job_id: 任务唯一标识
        """
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled industry sync job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    self._run_industry_sync_task,
                    trigger=trigger,
                    id=job_id,
                    name="Industry Classification Sync",
                    replace_existing=True,
                )
                logger.info(f"Added industry sync job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to add industry sync job: {e}")
            return False

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
    # 任务执行体
    # ------------------------------------------------------------------
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
            from collector.tasks.industry_classification_sync import run as run_industry_sync
            run_industry_sync(db=self.db)
        except Exception as e:
            logger.error(f"Scheduled industry sync task failed: {e}")

    # ------------------------------------------------------------------
    # 手动触发（保持向后兼容）
    # ------------------------------------------------------------------
    def run_company_task_now(self):
        """手动立即执行全量公司信息采集任务"""
        logger.info("Manual trigger: full company task")
        self._run_company_task()

    def run_company_task_by_name(self, query: str):
        """手动按公司名称/代码执行采集任务"""
        logger.info(f"Manual trigger: company task by name '{query}'")
        try:
            source = AkshareSource()
            task = CompanyTask(db=self.db, source=source, monitor=self.monitor)
            task.run_by_name(query)
        except Exception as e:
            logger.error(f"Company task by name failed: {e}")

    def run_industry_sync_task_now(self):
        """手动立即执行行业分类同步任务"""
        logger.info("Manual trigger: industry classification sync task")
        self._run_industry_sync_task()

    # ------------------------------------------------------------------
    # 指数模块任务
    # ------------------------------------------------------------------
    def add_index_basic_job(self, cron: str, job_id: str = "index_basic_task") -> bool:
        """添加/更新指数基本信息采集定时任务"""
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled index basic job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    self._run_index_basic_task,
                    trigger=trigger,
                    id=job_id,
                    name="Index Basic Sync",
                    replace_existing=True,
                )
                logger.info(f"Added index basic job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to add index basic job: {e}")
            return False

    def add_index_history_job(self, cron: str, job_id: str = "index_history_task") -> bool:
        """添加/更新指数历史行情采集定时任务"""
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled index history job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    self._run_index_history_task,
                    trigger=trigger,
                    id=job_id,
                    name="Index History Sync",
                    replace_existing=True,
                )
                logger.info(f"Added index history job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to add index history job: {e}")
            return False

    def add_etf_basic_job(self, cron: str, job_id: str = "etf_basic_task") -> bool:
        """添加/更新 ETF 基本信息采集定时任务"""
        try:
            trigger = CronTrigger.from_crontab(cron)
            if self._scheduler.get_job(job_id):
                self._scheduler.reschedule_job(job_id, trigger=trigger)
                logger.info(f"Rescheduled ETF basic job '{job_id}' with cron '{cron}'")
            else:
                self._scheduler.add_job(
                    self._run_etf_basic_task,
                    trigger=trigger,
                    id=job_id,
                    name="ETF Basic Sync",
                    replace_existing=True,
                )
                logger.info(f"Added ETF basic job '{job_id}' with cron '{cron}'")
            return True
        except Exception as e:
            logger.error(f"Failed to add ETF basic job: {e}")
            return False

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

    def run_index_basic_task_now(self):
        """手动立即执行指数基本信息采集任务"""
        logger.info("Manual trigger: full index basic task")
        self._run_index_basic_task()

    def run_index_history_task_now(self):
        """手动立即执行指数历史行情采集任务"""
        logger.info("Manual trigger: full index history task")
        self._run_index_history_task()

    def run_etf_basic_task_now(self):
        """手动立即执行 ETF 基本信息采集任务"""
        logger.info("Manual trigger: full ETF basic task")
        self._run_etf_basic_task()
