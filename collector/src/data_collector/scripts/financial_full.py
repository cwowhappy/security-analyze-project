"""财务三表批量采集编排脚本。

依次执行利润表、资产负债表、现金流量表采集，最后计算指标。
"""

import structlog

from data_collector.config import Settings
from data_collector.scripts.financial_income import run_financial_income
from data_collector.scripts.financial_balance import run_financial_balance
from data_collector.scripts.financial_cashflow import run_financial_cashflow
from data_collector.scripts.financial_indicator import run_financial_indicator

logger = structlog.get_logger(__name__)


def run_financial_full(stock_code: str | None = None, settings: Settings | None = None) -> dict:
    """执行财务三表全量采集与指标计算。

    Args:
        stock_code: 单只股票代码，None 则采集全市场
        settings: 配置

    Returns:
        各阶段统计汇总
    """
    settings = settings or Settings()
    logger.info("开始财务三表全量采集", stock_code=stock_code)

    # 1. 利润表
    logger.info("【1/4】采集利润表...")
    income_result = run_financial_income(stock_code, settings)

    # 2. 资产负债表
    logger.info("【2/4】采集资产负债表...")
    balance_result = run_financial_balance(stock_code, settings)

    # 3. 现金流量表
    logger.info("【3/4】采集现金流量表...")
    cashflow_result = run_financial_cashflow(stock_code, settings)

    # 4. 指标计算
    logger.info("【4/4】计算财务指标...")
    indicator_result = run_financial_indicator(stock_code, settings)

    result = {
        "income": income_result,
        "balance": balance_result,
        "cashflow": cashflow_result,
        "indicator": indicator_result,
    }

    logger.info(
        "财务三表全量采集完成",
        stock_code=stock_code,
        income_total=income_result["total"],
        balance_total=balance_result["total"],
        cashflow_total=cashflow_result["total"],
        indicator_total=indicator_result["total"],
    )
    return result
