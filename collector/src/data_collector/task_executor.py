"""任务执行器：根据 task_type 路由到对应的采集脚本。"""

import traceback
from datetime import datetime

import structlog
import ulid

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask, TaskStatus
from data_collector.scripts.company_full import run_company_full
from data_collector.scripts.field_supplement import run_field_supplement
from data_collector.scripts.stock_full import run_stock_full

logger = structlog.get_logger(__name__)


class TaskExecutor:
    """采集任务执行器。

    负责根据 task_type 调用对应的采集脚本，记录执行状态。
    """

    def __init__(self, settings: Settings | None = None) -> None:
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
        )

        try:
            if task.task_type == "stock_full":
                result = run_stock_full(self._settings)
                task.data_source = "akshare"
                task.total_count = result["total"]
                task.success_count = result["success"]
                task.fail_count = result["failed"]
            elif task.task_type == "company_full":
                result = run_company_full(self._settings)
                task.data_source = "akshare"
                task.total_count = result["total"]
                task.success_count = result["success"]
                task.fail_count = result["failed"]
            elif task.task_type in ("field_supplement", "field_full"):
                result = run_field_supplement(self._settings)
                task.data_source = "tushare"
                task.total_count = result.get("stock_total", 0) + result.get("company_total", 0)
                task.success_count = result.get("stock_success", 0) + result.get("company_success", 0)
                task.fail_count = result.get("stock_failed", 0) + result.get("company_failed", 0)
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

    def _execute_stock_single(self, task: CollectionTask) -> None:
        """单条采集股票信息。"""
        stock_code = task.task_params.get("stock_code")
        if not stock_code:
            raise ValueError("stock_single 任务需要提供 task_params.stock_code")

        from data_collector.scripts.stock_full import _to_exchange, _to_ts_code
        from data_collector.adapters.db_stock_repository import DbStockRepository
        import akshare as ak

        task.data_source = "akshare"
        task.total_count = 1

        df = ak.stock_info_a_code_name()
        target_row = df[df["code"] == stock_code]
        if target_row.empty:
            task.fail_count = 1
            raise ValueError(f"未找到股票: {stock_code}")

        row = target_row.iloc[0]
        from data_collector.core.domain.stock import Stock
        stock = Stock(
            stock_code=stock_code,
            name=str(row["name"]).strip(),
            ts_code=_to_ts_code(stock_code, row.get("market")),
            exchange=_to_exchange(stock_code, row.get("market")),
        )
        DbStockRepository().save(stock)
        task.success_count = 1

    def _execute_company_single(self, task: CollectionTask) -> None:
        """单条采集公司信息。"""
        stock_code = task.task_params.get("stock_code")
        if not stock_code:
            raise ValueError("company_single 任务需要提供 task_params.stock_code")

        from data_collector.scripts.company_full import fetch_company_for_stock
        from data_collector.adapters.db_company_repository import DbCompanyRepository
        from data_collector.adapters.db_stock_repository import DbStockRepository

        task.data_source = "akshare"
        task.total_count = 1

        company = fetch_company_for_stock(stock_code)
        if not company:
            task.fail_count = 1
            raise ValueError(f"未找到公司信息: {stock_code}")

        DbCompanyRepository().save(company)
        if company.unified_social_credit_code:
            DbStockRepository().update_company_id(stock_code, company.id)
        task.success_count = 1
