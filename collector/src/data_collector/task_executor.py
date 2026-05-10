"""任务执行器：根据 task_type 路由到对应的采集逻辑。"""

import time
import traceback
from datetime import datetime

import structlog
import ulid

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask, TaskStatus
from data_collector.core.domain.data_source_error import DataSourceError
from data_collector.core.ports.company_repository import CompanyRepository
from data_collector.core.ports.data_source import DataSource
from data_collector.core.ports.stock_repository import StockRepository
from data_collector.core.services.multi_source_stock_collector import MultiSourceStockCollector

logger = structlog.get_logger(__name__)


class TaskExecutor:
    """采集任务执行器。

    负责根据 task_type 选择数据源、执行采集、入库、记录状态。
    """

    def __init__(
        self,
        sources: list[DataSource],
        stock_repo: StockRepository,
        company_repo: CompanyRepository,
        settings: Settings | None = None,
    ) -> None:
        self._sources = sorted(sources, key=lambda s: s.priority)
        self._stock_repo = stock_repo
        self._company_repo = company_repo
        self._settings = settings or Settings()

    def execute(self, task: CollectionTask) -> CollectionTask:
        """执行采集任务并更新状态。

        Args:
            task: 待执行的任务。

        Returns:
            执行后的任务（状态已更新）。
        """
        task.status = TaskStatus.RUNNING.value
        task.started_at = datetime.now()
        task.id = task.id or str(ulid.ULID())

        logger.info(
            "开始执行任务",
            task_id=task.id,
            task_type=task.task_type,
            data_source=task.data_source,
        )

        try:
            if task.task_type == "stock_full":
                self._execute_stock_full(task)
            elif task.task_type == "company_full":
                self._execute_company_full(task)
            elif task.task_type == "stock_single":
                self._execute_stock_single(task)
            elif task.task_type == "company_single":
                self._execute_company_single(task)
            else:
                raise ValueError(f"未知的任务类型: {task.task_type}")

            task.status = TaskStatus.SUCCESS.value
            task.completed_at = datetime.now()
            logger.info(
                "任务执行成功",
                task_id=task.id,
                task_type=task.task_type,
                total=task.total_count,
                success=task.success_count,
                failed=task.fail_count,
            )

        except Exception as e:
            task.status = TaskStatus.FAILED.value
            task.error_message = f"{e}\n{traceback.format_exc()}"
            task.completed_at = datetime.now()
            logger.error(
                "任务执行失败",
                task_id=task.id,
                task_type=task.task_type,
                error=str(e),
            )

        return task

    def _execute_stock_full(self, task: CollectionTask) -> None:
        """全量采集股票列表（双源合并模式）。"""
        ak_source = self._find_source_by_name("akshare")
        ts_source = self._find_source_by_name("tushare")

        # 当 akshare 与 tushare 均可用时，启用双源合并采集
        if ak_source and ak_source.is_available() and ts_source and ts_source.is_available():
            logger.info("双源均可用，启用合并采集模式")
            collector = MultiSourceStockCollector(ak_source, ts_source)
            stocks = collector.fetch_merged_stock_list()
            task.data_source = f"{ak_source.name}+{ts_source.name}"
        else:
            logger.info("双源不可用时回退到单源模式")
            source = self._pick_source(task.data_source)
            task.data_source = source.name
            stocks = source.fetch_stock_list()

        task.total_count = len(stocks)
        success, failed = self._stock_repo.save_all(stocks)
        task.success_count = success
        task.fail_count = failed

    def _execute_company_full(self, task: CollectionTask) -> None:
        """全量采集公司信息。

        遍历已有股票列表，逐条获取公司信息。
        """
        source = self._pick_source(task.data_source)
        task.data_source = source.name

        stocks = self._stock_repo.find_all()
        task.total_count = len(stocks)

        success = 0
        failed = 0
        for stock in stocks:
            try:
                company = source.fetch_company_info(stock.stock_code)
                if company:
                    self._company_repo.save(company)
                    success += 1
                else:
                    failed += 1
            except Exception as e:
                logger.warning(
                    "采集公司信息失败",
                    stock_code=stock.stock_code,
                    error=str(e),
                )
                failed += 1

            # 请求间延迟
            time.sleep(
                (self._settings.source_request_delay_min + self._settings.source_request_delay_max) / 2
            )

        task.success_count = success
        task.fail_count = failed

    def _execute_stock_single(self, task: CollectionTask) -> None:
        """单条采集股票信息。"""
        stock_code = task.task_params.get("stock_code")
        if not stock_code:
            raise ValueError("stock_single 任务需要提供 task_params.stock_code")

        source = self._pick_source(task.data_source)
        task.data_source = source.name
        task.total_count = 1

        # 单条采集：从全量列表中过滤
        stocks = source.fetch_stock_list()
        target = [s for s in stocks if s.stock_code == stock_code]
        if not target:
            task.fail_count = 1
            raise DataSourceError(source.name, f"未找到股票: {stock_code}")

        self._stock_repo.save(target[0])
        task.success_count = 1

    def _execute_company_single(self, task: CollectionTask) -> None:
        """单条采集公司信息。"""
        stock_code = task.task_params.get("stock_code")
        if not stock_code:
            raise ValueError("company_single 任务需要提供 task_params.stock_code")

        source = self._pick_source(task.data_source)
        task.data_source = source.name
        task.total_count = 1

        company = source.fetch_company_info(stock_code)
        if not company:
            task.fail_count = 1
            raise DataSourceError(source.name, f"未找到公司: {stock_code}")

        self._company_repo.save(company)
        task.success_count = 1

    def _find_source_by_name(self, name: str) -> DataSource | None:
        """根据名称查找已注册的数据源。"""
        for s in self._sources:
            if s.name == name:
                return s
        return None

    def _pick_source(self, preferred: str | None) -> DataSource:
        """选择可用的数据源。

        优先使用指定数据源，不可用时按优先级降级。
        """
        if preferred:
            for s in self._sources:
                if s.name == preferred and s.is_available():
                    return s
            logger.warning("指定数据源不可用，尝试降级", preferred=preferred)

        for s in self._sources:
            if s.is_available():
                return s

        raise RuntimeError("所有数据源均不可用")



