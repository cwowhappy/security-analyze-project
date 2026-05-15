"""现金流量表采集脚本。

使用 AKShare 的 stock_financial_report_sina 接口获取现金流量表数据。
"""

import random
import time
from datetime import date
from decimal import Decimal

import akshare as ak
import structlog

from data_collector.adapters.db_financial_cashflow_repository import DbFinancialCashflowRepository
from data_collector.config import Settings
from data_collector.core.domain.financial_cashflow import FinancialCashflow

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


def fetch_cashflow_for_stock(stock_code: str) -> list[FinancialCashflow]:
    """获取单只股票的现金流量表数据。"""
    logger.info("开始采集现金流量表", stock_code=stock_code)

    try:
        df = ak.stock_financial_report_sina(stock=stock_code, symbol="现金流量表")
    except Exception as e:
        logger.warning("akshare 现金流量表采集失败", stock_code=stock_code, error=str(e))
        return []

    if df is None or df.empty:
        logger.warning("akshare 现金流量表返回空数据", stock_code=stock_code)
        return []

    cashflows = []
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

            cf = FinancialCashflow(
                stock_code=stock_code,
                report_date=report_date,
                report_type=report_type,
                cf_operating=_to_decimal(get_col(["经营活动产生的现金流量净额", "经营活动现金流量净额"])),
                cf_investing=_to_decimal(get_col(["投资活动产生的现金流量净额", "投资活动现金流量净额"])),
                cf_financing=_to_decimal(get_col(["筹资活动产生的现金流量净额", "筹资活动现金流量净额"])),
                net_cash_flow=_to_decimal(get_col(["现金及现金等价物净增加额", "现金及现金等价物的净增加额"])),
                free_cash_flow=None,  # 需要计算：经营现金流 - 资本开支
                capex=_to_decimal(get_col(["购建固定资产、无形资产和其他长期资产支付的现金", "购建固定资产无形资产和其他长期资产支付的现金"])),
                cash_received_operating=_to_decimal(get_col(["销售商品、提供劳务收到的现金"])),
                tax_paid=_to_decimal(get_col(["支付的各项税费"])),
            )

            # 计算自由现金流
            if cf.cf_operating is not None and cf.capex is not None:
                cf.free_cash_flow = cf.cf_operating - cf.capex

            # 计算净现金流（三表合计）
            if cf.net_cash_flow is None and cf.cf_operating is not None and cf.cf_investing is not None and cf.cf_financing is not None:
                cf.net_cash_flow = cf.cf_operating + cf.cf_investing + cf.cf_financing

            cashflows.append(cf)
        except Exception as e:
            logger.debug("解析现金流量表行失败", stock_code=stock_code, error=str(e))
            continue

    logger.info("现金流量表采集完成", stock_code=stock_code, count=len(cashflows))
    return cashflows


def run_financial_cashflow(stock_code: str | None = None, settings: Settings | None = None) -> dict:
    """执行现金流量表采集。"""
    settings = settings or Settings()
    repo = DbFinancialCashflowRepository()

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
            cashflows = fetch_cashflow_for_stock(code)
            total += len(cashflows)
            if cashflows:
                s, f = repo.save_all(cashflows)
                success += s
                failed += f
        except Exception as e:
            logger.warning("现金流量表采集异常", stock_code=code, error=str(e))
            failed += 1

    return {"total": total, "success": success, "failed": failed}
