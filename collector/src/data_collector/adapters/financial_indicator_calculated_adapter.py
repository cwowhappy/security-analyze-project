"""财务指标计算适配器。

基于已入库的三表数据，通过 IndicatorCalculator 计算财务指标。
遵循 DataSourceAdapter 协议，但不调用外部 API。
"""

from typing import Any

import structlog

from data_collector.adapters.db_financial_balance_repository import (
    DbFinancialBalanceRepository,
)
from data_collector.adapters.db_financial_cashflow_repository import (
    DbFinancialCashflowRepository,
)
from data_collector.adapters.db_financial_income_repository import (
    DbFinancialIncomeRepository,
)
from data_collector.adapters.db_financial_indicator_repository import (
    DbFinancialIndicatorRepository,
)
from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.core.config.field_mapping_config import SourceConfig
from data_collector.services.indicator_calculator import (
    FinancialDataSnapshot,
    IndicatorCalculator,
)

logger = structlog.get_logger(__name__)


class FinancialIndicatorCalculatedAdapter:
    """基于数据库三表数据计算财务指标的适配器。"""

    def __init__(
        self,
        income_repo: DbFinancialIncomeRepository | None = None,
        balance_repo: DbFinancialBalanceRepository | None = None,
        cashflow_repo: DbFinancialCashflowRepository | None = None,
        indicator_repo: DbFinancialIndicatorRepository | None = None,
        stock_repo: DbStockRepository | None = None,
        calculator: IndicatorCalculator | None = None,
    ) -> None:
        self._income_repo = income_repo or DbFinancialIncomeRepository()
        self._balance_repo = balance_repo or DbFinancialBalanceRepository()
        self._cashflow_repo = cashflow_repo or DbFinancialCashflowRepository()
        self._indicator_repo = indicator_repo or DbFinancialIndicatorRepository()
        self._stock_repo = stock_repo or DbStockRepository()
        self._calculator = calculator or IndicatorCalculator()

    def fetch(self, stock_code: str, source_config: SourceConfig) -> list[dict[str, Any]]:
        """读取三表数据并计算财务指标，返回结果字典列表。

        Args:
            stock_code: 股票代码。
            source_config: 数据源配置（本适配器未使用外部参数）。

        Returns:
            各报告期指标字典列表。
        """
        current_incomes = self._income_repo.find_by_stock_code(stock_code, limit=2)
        current_balances = self._balance_repo.find_by_stock_code(stock_code, limit=2)
        current_cashflows = self._cashflow_repo.find_by_stock_code(stock_code, limit=2)

        if not current_incomes or not current_balances or not current_cashflows:
            logger.debug("三表数据不完整，跳过指标计算", stock_code=stock_code)
            return []

        stock = self._stock_repo.find_by_symbol(stock_code)
        is_bank = stock is not None and stock.industry == "银行"

        results: list[dict[str, Any]] = []

        for i in range(len(current_incomes)):
            inc = current_incomes[i]
            bal = next(
                (b for b in current_balances if b.report_date == inc.report_date), None
            )
            cf = next(
                (c for c in current_cashflows if c.report_date == inc.report_date), None
            )

            if not bal or not cf:
                continue

            current = FinancialDataSnapshot(
                stock_code=stock_code,
                report_date=str(inc.report_date),
                report_type=inc.report_type,
                income=inc,
                balance=bal,
                cashflow=cf,
            )

            previous = None
            if i + 1 < len(current_incomes):
                prev_inc = current_incomes[i + 1]
                prev_bal = next(
                    (b for b in current_balances if b.report_date == prev_inc.report_date),
                    None,
                )
                prev_cf = next(
                    (c for c in current_cashflows if c.report_date == prev_inc.report_date),
                    None,
                )
                if prev_bal and prev_cf:
                    previous = FinancialDataSnapshot(
                        stock_code=stock_code,
                        report_date=str(prev_inc.report_date),
                        report_type=prev_inc.report_type,
                        income=prev_inc,
                        balance=prev_bal,
                        cashflow=prev_cf,
                    )

            try:
                indicator = self._calculator.calculate(
                    current, previous, is_bank=is_bank
                )
                self._indicator_repo.save(indicator)
                results.append(indicator.to_dict())
            except Exception as e:
                logger.warning(
                    "指标计算失败",
                    stock_code=stock_code,
                    report_date=inc.report_date,
                    error=str(e),
                )

        return results
