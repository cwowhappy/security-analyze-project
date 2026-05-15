"""财务全量采集编排器。

依次执行利润表、资产负债表、现金流量表采集，最后计算指标。
"""

import structlog

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.task_executor import TaskExecutor

logger = structlog.get_logger(__name__)


class FinancialFullOrchestrator:
    """财务三表 + 指标计算编排器。"""

    def __init__(self, executor: TaskExecutor | None = None) -> None:
        self._executor = executor or TaskExecutor()

    def run(
        self, stock_code: str | None, task: CollectionTask, settings: Settings
    ) -> dict[str, dict[str, int]]:
        """执行财务全量采集与指标计算。

        Args:
            stock_code: 单只股票代码，None 则采集全市场。
            task: 父任务对象，用于传递 mode 等上下文。
            settings: 配置。

        Returns:
            各阶段统计汇总。
        """
        sub_params = {"stock_code": stock_code} if stock_code else {}

        # 1. 利润表
        logger.info("【1/4】采集利润表...", stock_code=stock_code)
        income_task = CollectionTask(
            task_type="financial_income",
            mode=task.mode,
            task_params=sub_params,
        )
        income_executed = self._executor.execute(income_task)
        income_result = {
            "total": income_executed.total_count,
            "success": income_executed.success_count,
            "failed": income_executed.fail_count,
        }

        # 2. 资产负债表
        logger.info("【2/4】采集资产负债表...", stock_code=stock_code)
        balance_task = CollectionTask(
            task_type="financial_balance",
            mode=task.mode,
            task_params=sub_params,
        )
        balance_executed = self._executor.execute(balance_task)
        balance_result = {
            "total": balance_executed.total_count,
            "success": balance_executed.success_count,
            "failed": balance_executed.fail_count,
        }

        # 3. 现金流量表
        logger.info("【3/4】采集现金流量表...", stock_code=stock_code)
        cashflow_task = CollectionTask(
            task_type="financial_cashflow",
            mode=task.mode,
            task_params=sub_params,
        )
        cashflow_executed = self._executor.execute(cashflow_task)
        cashflow_result = {
            "total": cashflow_executed.total_count,
            "success": cashflow_executed.success_count,
            "failed": cashflow_executed.fail_count,
        }

        # 4. 指标计算
        # single 模式下，若前三表任一失败则跳过指标计算
        should_skip_indicator = (
            task.mode == "single"
            and (
                income_executed.status != "success"
                or balance_executed.status != "success"
                or cashflow_executed.status != "success"
            )
        )

        if should_skip_indicator:
            logger.info(
                "【4/4】跳过指标计算（前置采集失败）",
                stock_code=stock_code,
            )
            indicator_result = {"total": 0, "success": 0, "failed": 0}
        else:
            logger.info("【4/4】计算财务指标...", stock_code=stock_code)
            indicator_task = CollectionTask(
                task_type="financial_indicator",
                mode=task.mode,
                task_params=sub_params,
            )
            indicator_executed = self._executor.execute(indicator_task)
            indicator_result = {
                "total": indicator_executed.total_count,
                "success": indicator_executed.success_count,
                "failed": indicator_executed.fail_count,
            }

        return {
            "income": income_result,
            "balance": balance_result,
            "cashflow": cashflow_result,
            "indicator": indicator_result,
        }
