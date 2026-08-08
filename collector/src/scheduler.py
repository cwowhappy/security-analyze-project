from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger
from loguru import logger

from src.config import settings
from src.tasks.cve_collector import CveCollectorTask


class TaskScheduler:
    def __init__(self):
        self.scheduler = BackgroundScheduler(timezone=settings.scheduler_timezone)
        self._register_jobs()

    def _register_jobs(self):
        self.scheduler.add_job(
            CveCollectorTask().run,
            trigger=IntervalTrigger(minutes=5),
            id="cve_collector_every_5m",
            replace_existing=True,
        )

    def start(self):
        logger.info("Starting scheduler")
        self.scheduler.start()

    def shutdown(self, wait: bool = True):
        logger.info("Shutting down scheduler")
        self.scheduler.shutdown(wait=wait)
