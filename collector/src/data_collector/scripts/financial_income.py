"""利润表采集脚本。

使用 AKShare 的 stock_financial_report_sina 接口获取利润表数据。
"""

import random
import time
from datetime import date
from decimal import Decimal

import akshare as ak
import structlog

from data_collector.adapters.db_financial_income_repository import DbFinancialIncomeRepository
from data_collector.config import Settings
from data_collector.core.domain.financial_income import FinancialIncome

logger = structlog.get_logger(__name__)


REPORT_TYPE_MAP = {
    "年报": "Y",
    "一季报": "Q1",
    "半年报": "Q2",
    "三季报": "Q3",
}


def _parse_date(value):
    """解析日期字符串。"""
    if not value:
        return None
    try:
        if isinstance(value, date):
            return value
        return date.fromisoformat(str(value).strip())
    except ValueError:
        return None


def _to_decimal(value):
    """安全转换为 Decimal。"""
    if value is None:
        return None
    try:
        v = str(value).replace(",", "").strip()
        if v == "--" or v == "-" or v == "" or v.lower() == "nan":
            return None
        d = Decimal(v)
        if d != d:  # NaN 判断: NaN != NaN
            return None
        return d
    except (ValueError, TypeError):
        return None


def fetch_income_for_stock(stock_code: str) -> list[FinancialIncome]:
    """获取单只股票的利润表数据。

    Args:
        stock_code: 股票代码，如 "000001"

    Returns:
        FinancialIncome 列表
    """
    logger.info("开始采集利润表", stock_code=stock_code)

    try:
        # AKShare 新浪财经财务报告接口
        df = ak.stock_financial_report_sina(stock=stock_code, symbol="利润表")
    except Exception as e:
        logger.warning("akshare 利润表采集失败", stock_code=stock_code, error=str(e))
        return []

    if df is None or df.empty:
        logger.warning("akshare 利润表返回空数据", stock_code=stock_code)
        return []

    incomes = []
    for _, row in df.iterrows():
        try:
            report_date = _parse_date(row.get("报告日") or row.get("报告期") or row.get("报告日期"))
            if not report_date:
                continue

            # 推断报告类型（根据月份）
            month = report_date.month
            if month == 12:
                report_type = "Y"
            elif month == 3:
                report_type = "Q1"
            elif month == 6:
                report_type = "Q2"
            elif month == 9:
                report_type = "Q3"
            else:
                report_type = "Y"

            # AKShare 新浪接口列名映射（常见列名）
            def get_col(candidates):
                for c in candidates:
                    if c in row:
                        return row[c]
                return None

            income = FinancialIncome(
                stock_code=stock_code,
                report_date=report_date,
                report_type=report_type,
                basic_eps=_to_decimal(get_col(["基本每股收益", "每股收益"])),
                diluted_eps=_to_decimal(get_col(["稀释每股收益"])),
                total_revenue=_to_decimal(get_col(["营业总收入", "营业收入", "营业总收"])),
                revenue=_to_decimal(get_col(["营业收入"])),
                operating_cost=_to_decimal(get_col(["营业成本", "营业总成本", "营业支出"])),
                gross_profit=None,  # 需要计算：营收 - 营业成本
                selling_expense=_to_decimal(get_col(["销售费用"])),
                admin_expense=_to_decimal(get_col(["管理费用", "业务及管理费用"])),
                rd_expense=_to_decimal(get_col(["研发费用", "研究费用"])),
                financial_expense=_to_decimal(get_col(["财务费用"])),
                operating_profit=_to_decimal(get_col(["营业利润"])),
                total_profit=_to_decimal(get_col(["利润总额"])),
                net_profit=_to_decimal(get_col(["净利润"])),
                np_parent_company=_to_decimal(get_col([
                    "归属于母公司股东的净利润", "归母净利润",
                    "归属于母公司所有者的净利润", "归属于母公司的净利润"
                ])),
                np_excl_nonrecurring=_to_decimal(get_col(["扣除非经常性损益后的净利润", "扣非净利润"])),
            )

            # 计算毛利
            if income.revenue is not None and income.operating_cost is not None:
                income.gross_profit = income.revenue - income.operating_cost
            elif income.total_revenue is not None and income.operating_cost is not None:
                income.gross_profit = income.total_revenue - income.operating_cost

            incomes.append(income)
        except Exception as e:
            logger.debug("解析利润表行失败", stock_code=stock_code, row=dict(row), error=str(e))
            continue

    logger.info("利润表采集完成", stock_code=stock_code, count=len(incomes))
    return incomes


def run_financial_income(stock_code: str | None = None, settings: Settings | None = None) -> dict:
    """执行利润表采集。

    Args:
        stock_code: 单只股票代码，None 则采集全市场
        settings: 配置

    Returns:
        {"total": int, "success": int, "failed": int}
    """
    settings = settings or Settings()
    repo = DbFinancialIncomeRepository()

    if stock_code:
        stock_codes = [stock_code]
    else:
        from data_collector.adapters.db_stock_repository import DbStockRepository
        stocks = DbStockRepository().find_all()
        stock_codes = [s.stock_code for s in stocks]

    total = 0
    success = 0
    failed = 0

    for code in stock_codes:
        time.sleep(random.uniform(
            settings.source_request_delay_min,
            settings.source_request_delay_max,
        ))
        try:
            incomes = fetch_income_for_stock(code)
            total += len(incomes)
            if incomes:
                s, f = repo.save_all(incomes)
                success += s
                failed += f
        except Exception as e:
            logger.warning("利润表采集异常", stock_code=code, error=str(e))
            failed += 1

    return {"total": total, "success": success, "failed": failed}
