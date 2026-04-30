import logging
import schedule
import time
import threading
from typing import Optional

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.company_task import CompanyTask

logger = logging.getLogger(__name__)


class Scheduler:
    def __init__(self, db: PostgresDB):
        self.db = db
        self._thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()
        self._setup_jobs()

    def _setup_jobs(self):
        # 公司信息采集任务：默认不自动调度，仅支持手动触发
        # 如需定时运行，取消下面注释并配置时间
        # schedule.every().day.at("09:00").do(self._run_company_task)
        pass

    def _run_company_task(self):
        logger.info("Running scheduled company task...")
        try:
            source = AkshareSource()
            task = CompanyTask(db=self.db, source=source)
            task.run()
        except Exception as e:
            logger.error(f"Company task failed: {e}")

    def run_company_task_now(self):
        """手动立即执行全量公司信息采集任务"""
        logger.info("Manual trigger: full company task")
        self._run_company_task()

    def run_company_task_by_name(self, query: str):
        """手动按公司名称/代码执行采集任务

        Args:
            query: 公司名称或股票代码
        """
        logger.info(f"Manual trigger: company task by name '{query}'")
        try:
            source = AkshareSource()
            task = CompanyTask(db=self.db, source=source)
            task.run_by_name(query)
        except Exception as e:
            logger.error(f"Company task by name failed: {e}")

    def start(self):
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._loop, daemon=True)
        self._thread.start()
        logger.info("Scheduler started")

    def _loop(self):
        while not self._stop_event.is_set():
            schedule.run_pending()
            time.sleep(1)

    def stop(self):
        self._stop_event.set()
        if self._thread:
            self._thread.join(timeout=5)
        logger.info("Scheduler stopped")
