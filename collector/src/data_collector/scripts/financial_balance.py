"""资产负债表采集脚本。

使用 AKShare 的 stock_financial_report_sina 接口获取资产负债表数据。
"""

import random
import time
from datetime import date
from decimal import Decimal

import akshare as ak
import structlog

from data_collector.adapters.db_financial_balance_repository import DbFinancialBalanceRepository
from data_collector.config import Settings
from data_collector.core.domain.financial_balance import FinancialBalance

logger = structlog.get_logger(__name__)


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
        if v in ("--", "-", "", "NaN", "nan"):
            return None
        d = Decimal(v)
        if d != d:  # NaN 判断: NaN != NaN
            return None
        return d
    except (ValueError, TypeError):
        return None


def fetch_balance_for_stock(stock_code: str) -> list[FinancialBalance]:
    """获取单只股票的资产负债表数据。"""
    logger.info("开始采集资产负债表", stock_code=stock_code)

    try:
        df = ak.stock_financial_report_sina(stock=stock_code, symbol="资产负债表")
    except Exception as e:
        logger.warning("akshare 资产负债表采集失败", stock_code=stock_code, error=str(e))
        return []

    if df is None or df.empty:
        logger.warning("akshare 资产负债表返回空数据", stock_code=stock_code)
        return []

    balances = []
    for _, row in df.iterrows():
        try:
            report_date = _parse_date(row.get("报告日") or row.get("报告期") or row.get("报告日期"))
            if not report_date:
                continue

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

            def get_col(candidates):
                for c in candidates:
                    if c in row:
                        return row[c]
                return None

            total_assets = _to_decimal(get_col(["资产总计", "总资产", "资产合计"]))
            total_liabilities = _to_decimal(get_col(["负债合计", "负债总计", "总负债"]))
            total_equity = _to_decimal(get_col([
                "所有者权益合计", "股东权益合计",
                "所有者权益(或股东权益)合计", "股东权益"
            ]))
            equity_parent_company = _to_decimal(get_col([
                "归属于母公司所有者权益合计", "归属于母公司股东的权益合计",
                "归母权益", "归属于母公司股东的权益"
            ]))

            balance = FinancialBalance(
                stock_code=stock_code,
                report_date=report_date,
                report_type=report_type,
                total_assets=total_assets,
                total_liabilities=total_liabilities,
                total_equity=total_equity,
                equity_parent_company=equity_parent_company,
                current_assets=_to_decimal(get_col(["流动资产合计", "流动资产"])),
                non_current_assets=_to_decimal(get_col(["非流动资产合计", "非流动资产"])),
                cash_equivalents=_to_decimal(get_col(["货币资金", "现金及存放中央银行款项"])),
                accounts_receivable=_to_decimal(get_col(["应收账款", "应收票据及应收账款"])),
                inventories=_to_decimal(get_col(["存货"])),
                current_liabilities=_to_decimal(get_col(["流动负债合计", "流动负债"])),
                non_current_liabilities=_to_decimal(get_col(["非流动负债合计", "非流动负债"])),
                accounts_payable=_to_decimal(get_col(["应付账款", "应付票据及应付账款"])),
                short_term_borrowings=_to_decimal(get_col(["短期借款"])),
                long_term_borrowings=_to_decimal(get_col(["长期借款"])),
                goodwill=_to_decimal(get_col(["商誉"])),
            )

            # 兜底计算 total_equity（资产 = 负债 + 权益）
            if balance.total_equity is None and total_assets is not None and total_liabilities is not None:
                balance.total_equity = total_assets - total_liabilities
            balances.append(balance)
        except Exception as e:
            logger.debug("解析资产负债表行失败", stock_code=stock_code, error=str(e))
            continue

    logger.info("资产负债表采集完成", stock_code=stock_code, count=len(balances))
    return balances


def run_financial_balance(stock_code: str | None = None, settings: Settings | None = None) -> dict:
    """执行资产负债表采集。"""
    settings = settings or Settings()
    repo = DbFinancialBalanceRepository()

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
            balances = fetch_balance_for_stock(code)
            total += len(balances)
            if balances:
                s, f = repo.save_all(balances)
                success += s
                failed += f
        except Exception as e:
            logger.warning("资产负债表采集异常", stock_code=code, error=str(e))
            failed += 1

    return {"total": total, "success": success, "failed": failed}
