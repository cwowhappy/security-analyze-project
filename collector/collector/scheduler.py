import logging
import schedule
import time
import threading
from typing import Optional

from collector.db.postgres import PostgresDB

logger = logging.getLogger(__name__)


class Scheduler:
    def __init__(self, db: PostgresDB):
        self.db = db
        self._thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()
        self._setup_jobs()

    def _setup_jobs(self):
        # TODO: 注册具体采集任务
        # schedule.every().day.at("09:00").do(self._run_job, task_name="daily_company")
        pass

    def _run_job(self, task_name: str):
        logger.info(f"Running scheduled job: {task_name}")
        # TODO: 调用具体任务

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
