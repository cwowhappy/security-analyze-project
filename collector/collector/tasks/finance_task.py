import logging
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource

logger = logging.getLogger(__name__)


class FinanceTask:
    """采集财务报告数据任务"""

    def __init__(self, db: PostgresDB, source: AkshareSource):
        self.db = db
        self.source = source

    def run(self):
        logger.info("Starting finance task...")
        # TODO: 采集逻辑
        pass
