"""任务运行器：负责根据配置组装依赖（db + source + monitor），并执行任务。"""
from typing import Optional, Type

from collector.config import CollectorConfig
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor
from collector.tasks.base import BaseTask, TaskResult


class TaskRunner:
    """统一组装依赖并执行任意 BaseTask 子类。"""

    def __init__(self, cfg: Optional[CollectorConfig] = None):
        self.cfg = cfg or CollectorConfig.from_env()
        self.db = self._create_db()
        self.source = self._create_source()
        self.monitor = Monitor(self.db)

    def _create_db(self) -> PostgresDB:
        db = self.cfg.db
        return PostgresDB(
            host=db.host,
            port=db.port,
            database=db.database,
            user=db.user,
            password=db.password,
            pool_min_size=self.cfg.db_pool_min_size,
            pool_max_size=self.cfg.db_pool_max_size,
            pool_max_idle=self.cfg.db_pool_max_idle,
            pool_max_lifetime=self.cfg.db_pool_max_lifetime,
        )

    def _create_source(self) -> AkshareSource:
        return AkshareSource(
            max_retries=self.cfg.source_max_retries,
            retry_delay=self.cfg.source_retry_delay,
            retry_backoff=self.cfg.source_retry_backoff,
        )

    def run(self, task_cls: Type[BaseTask], mode: str = "full", **kwargs) -> TaskResult:
        """实例化并执行指定任务类。"""
        task = task_cls(
            db=self.db, source=self.source, monitor=self.monitor
        )
        return task.execute(mode=mode, **kwargs)

    def close(self):
        """关闭数据库连接池。"""
        self.db.close()
