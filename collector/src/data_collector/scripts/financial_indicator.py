"""财务指标计算与入库脚本。

读取 tb_financial_income / balance / cashflow，计算指标后写入 tb_financial_indicator。
"""

import structlog

from data_collector.adapters.db_financial_balance_repository import DbFinancialBalanceRepository
from data_collector.adapters.db_financial_cashflow_repository import DbFinancialCashflowRepository
from data_collector.adapters.db_financial_income_repository import DbFinancialIncomeRepository
from data_collector.adapters.db_financial_indicator_repository import DbFinancialIndicatorRepository
from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.config import Settings
from data_collector.services.indicator_calculator import FinancialDataSnapshot, IndicatorCalculator

logger = structlog.get_logger(__name__)


def run_financial_indicator(stock_code: str | None = None, settings: Settings | None = None) -> dict:
    """执行财务指标计算与入库。

    Args:
        stock_code: 单只股票代码，None 则计算全市场
        settings: 配置

    Returns:
        {"total": int, "success": int, "failed": int}
    """
    settings = settings or Settings()
    calculator = IndicatorCalculator()
    indicator_repo = DbFinancialIndicatorRepository()
    income_repo = DbFinancialIncomeRepository()
    balance_repo = DbFinancialBalanceRepository()
    cashflow_repo = DbFinancialCashflowRepository()

    if stock_code:
        stock_codes = [stock_code]
    else:
        stocks = DbStockRepository().find_all()
        stock_codes = [s.stock_code for s in stocks]

    total = 0
    success = 0
    failed = 0

    stock_repo = DbStockRepository()

    for code in stock_codes:
        try:
            # 获取当前期数据（最近一期）
            current_incomes = income_repo.find_by_stock_code(code, limit=2)
            current_balances = balance_repo.find_by_stock_code(code, limit=2)
            current_cashflows = cashflow_repo.find_by_stock_code(code, limit=2)

            if not current_incomes or not current_balances or not current_cashflows:
                logger.debug("三表数据不完整，跳过指标计算", stock_code=code)
                continue

            # 判断是否为银行业（部分指标不适用）
            stock = stock_repo.find_by_symbol(code)
            is_bank = stock is not None and stock.industry == "银行"

            # 逐期计算指标
            for i in range(len(current_incomes)):
                inc = current_incomes[i]
                bal = next((b for b in current_balances if b.report_date == inc.report_date), None)
                cf = next((c for c in current_cashflows if c.report_date == inc.report_date), None)

                if not bal or not cf:
                    continue

                current = FinancialDataSnapshot(
                    stock_code=code,
                    report_date=str(inc.report_date),
                    report_type=inc.report_type,
                    income=inc,
                    balance=bal,
                    cashflow=cf,
                )

                # 查找上一期数据（用于计算平均值和增长率）
                previous = None
                if i + 1 < len(current_incomes):
                    prev_inc = current_incomes[i + 1]
                    prev_bal = next((b for b in current_balances if b.report_date == prev_inc.report_date), None)
                    prev_cf = next((c for c in current_cashflows if c.report_date == prev_inc.report_date), None)
                    if prev_bal and prev_cf:
                        previous = FinancialDataSnapshot(
                            stock_code=code,
                            report_date=str(prev_inc.report_date),
                            report_type=prev_inc.report_type,
                            income=prev_inc,
                            balance=prev_bal,
                            cashflow=prev_cf,
                        )

                try:
                    indicator = calculator.calculate(current, previous, is_bank=is_bank)
                    indicator_repo.save(indicator)
                    success += 1
                except Exception as e:
                    logger.warning("指标计算失败", stock_code=code, report_date=inc.report_date, error=str(e))
                    failed += 1

                total += 1

        except Exception as e:
            logger.warning("财务指标计算异常", stock_code=code, error=str(e))
            failed += 1

    logger.info("财务指标计算完成", total=total, success=success, failed=failed)
    return {"total": total, "success": success, "failed": failed}
