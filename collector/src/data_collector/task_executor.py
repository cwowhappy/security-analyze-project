"""任务执行器：根据 task_type 路由到对应的采集脚本。

采用注册表模式管理任务处理器，新增任务类型时无需修改本文件核心逻辑。
"""

import traceback
from collections.abc import Callable
from datetime import datetime
from typing import Any

import structlog
import ulid

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask, TaskStatus

logger = structlog.get_logger(__name__)

# ---------------------------------------------------------------------------
# 全局注册表
# ---------------------------------------------------------------------------

_TASK_REGISTRY: dict[tuple[str, str], tuple[Callable[[CollectionTask, Settings], dict[str, Any]], str]] = {}


def register_task(task_type: str, mode: str = "full", data_source: str = "") -> Callable:
    """注册任务处理器装饰器。

    Args:
        task_type: 任务类型标识，如 ``"stock_full"``。
        mode: 任务模式，如 ``"full"``、``"single"``。
        data_source: 数据源标识，如 ``"akshare"``、``"tushare"``。

    Returns:
        装饰器函数。

    Example::

        @register_task("stock_full", mode="full", data_source="akshare")
        def handle_stock_full(task: CollectionTask, settings: Settings) -> dict:
            ...
    """

    def decorator(
        fn: Callable[[CollectionTask, Settings], dict[str, Any]],
    ) -> Callable[[CollectionTask, Settings], dict[str, Any]]:
        _TASK_REGISTRY[(task_type, mode)] = (fn, data_source)
        return fn

    return decorator


# ---------------------------------------------------------------------------
# 自定义异常
# ---------------------------------------------------------------------------


class BatchFailureThresholdExceeded(Exception):
    """批次失败率超过配置阈值时抛出。"""


# ---------------------------------------------------------------------------
# 任务处理器
# ---------------------------------------------------------------------------


def _handle_stock_full(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理股票全量采集。"""
    from data_collector.scripts.stock_full import run_stock_full

    return run_stock_full(settings)


def _handle_company_full(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理公司信息全量采集。"""
    from data_collector.scripts.company_full import run_company_full

    return run_company_full(settings)


def _handle_field_supplement(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理字段补充采集（Tushare）。"""
    from data_collector.scripts.field_supplement import run_field_supplement

    return run_field_supplement(settings)


def _handle_stock_single(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理单条股票采集。"""
    stock_code = task.task_params.get("stock_code")
    if not stock_code:
        raise ValueError("stock_single 任务需要提供 task_params.stock_code")

    from data_collector.scripts.stock_full import _to_exchange, _to_ts_code
    from data_collector.adapters.db_stock_repository import DbStockRepository
    import akshare as ak

    task.total_count = 1

    df = ak.stock_info_a_code_name()
    target_row = df[df["code"] == stock_code]
    if target_row.empty:
        task.fail_count = 1
        raise ValueError(f"未找到股票: {stock_code}")

    from data_collector.core.domain.stock import Stock

    row = target_row.iloc[0]
    stock = Stock(
        stock_code=stock_code,
        name=str(row["name"]).strip(),
        ts_code=_to_ts_code(stock_code, row.get("market")),
        exchange=_to_exchange(stock_code, row.get("market")),
    )
    DbStockRepository().save(stock)
    task.success_count = 1
    return {"total": task.total_count, "success": task.success_count, "failed": task.fail_count}


def _handle_company_single(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理单条公司采集。"""
    stock_code = task.task_params.get("stock_code")
    if not stock_code:
        raise ValueError("company_single 任务需要提供 task_params.stock_code")

    from data_collector.scripts.company_full import fetch_company_for_stock
    from data_collector.adapters.db_company_repository import DbCompanyRepository
    from data_collector.adapters.db_stock_repository import DbStockRepository

    task.total_count = 1

    company = fetch_company_for_stock(stock_code)
    if not company:
        task.fail_count = 1
        raise ValueError(f"未找到公司信息: {stock_code}")

    DbCompanyRepository().save(company)
    if company.unified_social_credit_code:
        DbStockRepository().update_company_id(stock_code, company.id)
    task.success_count = 1
    return {"total": task.total_count, "success": task.success_count, "failed": task.fail_count}


def _handle_financial_income(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理利润表采集。"""
    from data_collector.scripts.financial_income import run_financial_income

    stock_code = task.task_params.get("stock_code")
    return run_financial_income(stock_code, settings)


def _handle_financial_balance(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理资产负债表采集。"""
    from data_collector.scripts.financial_balance import run_financial_balance

    stock_code = task.task_params.get("stock_code")
    return run_financial_balance(stock_code, settings)


def _handle_financial_cashflow(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理现金流量表采集。"""
    from data_collector.scripts.financial_cashflow import run_financial_cashflow

    stock_code = task.task_params.get("stock_code")
    return run_financial_cashflow(stock_code, settings)


def _handle_financial_indicator(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理财务指标计算。"""
    from data_collector.scripts.financial_indicator import run_financial_indicator

    stock_code = task.task_params.get("stock_code")
    return run_financial_indicator(stock_code, settings)


def _handle_financial_full(task: CollectionTask, settings: Settings) -> dict[str, Any]:
    """处理财务全量采集（三表+指标）。"""
    from data_collector.core.pipeline.financial_full_orchestrator import (
        FinancialFullOrchestrator,
    )

    stock_code = task.task_params.get("stock_code")
    orchestrator = FinancialFullOrchestrator(TaskExecutor(settings))
    return orchestrator.run(stock_code, task, settings)


# ---------------------------------------------------------------------------
# 注册任务处理器
# ---------------------------------------------------------------------------

register_task("stock_full", mode="full", data_source="akshare")(_handle_stock_full)
register_task("company_full", mode="full", data_source="akshare")(_handle_company_full)
register_task("field_supplement", mode="full", data_source="tushare")(_handle_field_supplement)
register_task("field_full", mode="full", data_source="tushare")(_handle_field_supplement)
register_task("stock_single", mode="single", data_source="akshare")(_handle_stock_single)
register_task("company_single", mode="single", data_source="akshare")(_handle_company_single)
register_task("financial_income", mode="full", data_source="akshare")(_handle_financial_income)
register_task("financial_balance", mode="full", data_source="akshare")(_handle_financial_balance)
register_task("financial_cashflow", mode="full", data_source="akshare")(_handle_financial_cashflow)
register_task("financial_indicator", mode="full", data_source="calculated")(_handle_financial_indicator)
register_task("financial_full", mode="full", data_source="akshare")(_handle_financial_full)
register_task("financial_full", mode="single", data_source="akshare")(_handle_financial_full)


# ---------------------------------------------------------------------------
# 任务执行器
# ---------------------------------------------------------------------------


class TaskExecutor:
    """采集任务执行器。

    负责根据 task_type 从注册表查找并调用对应的采集脚本，记录执行状态。
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
            handler_info = _TASK_REGISTRY.get((task.task_type, task.mode))
            if handler_info is None:
                raise ValueError(f"未知的任务类型: {task.task_type} (mode={task.mode})")

            handler, data_source = handler_info
            task.data_source = data_source

            result = handler(task, self._settings)

            # 提取执行结果
            if task.task_type == "field_supplement":
                task.total_count = result.get("stock_total", 0) + result.get("company_total", 0)
                task.success_count = result.get("stock_success", 0) + result.get("company_success", 0)
                task.fail_count = result.get("stock_failed", 0) + result.get("company_failed", 0)
            elif task.task_type == "financial_full":
                task.total_count = (
                    result["income"]["total"]
                    + result["balance"]["total"]
                    + result["cashflow"]["total"]
                    + result["indicator"]["total"]
                )
                task.success_count = (
                    result["income"]["success"]
                    + result["balance"]["success"]
                    + result["cashflow"]["success"]
                    + result["indicator"]["success"]
                )
                task.fail_count = (
                    result["income"]["failed"]
                    + result["balance"]["failed"]
                    + result["cashflow"]["failed"]
                    + result["indicator"]["failed"]
                )
            else:
                task.total_count = result.get("total", 0)
                task.success_count = result.get("success", 0)
                task.fail_count = result.get("failed", 0)

            # 熔断检查：批次失败率超过阈值时标记为失败
            if (
                task.total_count > 0
                and task.fail_count / task.total_count > self._settings.batch_fail_threshold
            ):
                raise BatchFailureThresholdExceeded(
                    f"批次失败率 {task.fail_count / task.total_count:.1%} "
                    f"超过阈值 {self._settings.batch_fail_threshold}"
                )

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
