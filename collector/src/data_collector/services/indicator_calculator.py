"""财务指标计算服务。

基于利润表、资产负债表、现金流量表数据，计算 30+ 财务指标。
参考 PRD《基本面分析框架 · 指标体系完整版》。
"""

from dataclasses import dataclass
from decimal import Decimal

import structlog

from data_collector.core.domain.financial_balance import FinancialBalance
from data_collector.core.domain.financial_cashflow import FinancialCashflow
from data_collector.core.domain.financial_income import FinancialIncome
from data_collector.core.domain.financial_indicator import FinancialIndicator

logger = structlog.get_logger(__name__)


@dataclass
class FinancialDataSnapshot:
    """某一报告期的财务数据快照（三表合一）。"""

    stock_code: str
    report_date: str
    report_type: str
    income: FinancialIncome
    balance: FinancialBalance
    cashflow: FinancialCashflow


def _safe_div(numerator, denominator):
    """安全除法，处理 None 和零值。"""
    if numerator is None or denominator is None:
        return None
    if denominator == 0:
        return None
    return Decimal(numerator) / Decimal(denominator)


def _safe_avg(current, previous):
    """计算平均值，若上期缺失则用本期代替。"""
    if current is None:
        return None
    if previous is None:
        return current
    return (Decimal(current) + Decimal(previous)) / 2


def _calc_growth(current, previous):
    """计算同比增长率。"""
    if current is None or previous is None:
        return None
    if previous == 0:
        return None
    return (Decimal(current) - Decimal(previous)) / Decimal(previous) * 100


class IndicatorCalculator:
    """财务指标计算器。"""

    def calculate(self, current, previous=None, is_bank=False):
        """计算指定报告期的完整财务指标。

        Args:
            current: 当前期财务数据快照
            previous: 上一期财务数据快照（用于计算增长率和平均值）
            is_bank: 是否为银行业股票，银行业部分指标不适用
        """
        indicator = FinancialIndicator(
            stock_code=current.stock_code,
            report_date=current.income.report_date,
            report_type=current.report_type,
            data_source="CALCULATED",
        )

        inc = current.income
        bal = current.balance
        cf = current.cashflow

        prev_inc = previous.income if previous else None
        prev_bal = previous.balance if previous else None
        prev_cf = previous.cashflow if previous else None

        # 盈利能力
        avg_equity = _safe_avg(bal.equity_parent_company, prev_bal.equity_parent_company if prev_bal else None)
        indicator.roe = _safe_div(inc.np_parent_company, avg_equity)
        if indicator.roe:
            indicator.roe = indicator.roe * 100

        avg_assets = _safe_avg(bal.total_assets, prev_bal.total_assets if prev_bal else None)
        indicator.roa = _safe_div(inc.net_profit, avg_assets)
        if indicator.roa:
            indicator.roa = indicator.roa * 100

        # 毛利率：银行业无此概念（无营业成本科目）
        if not is_bank:
            if inc.revenue is not None and inc.operating_cost is not None and inc.revenue != 0:
                indicator.gross_margin = ((inc.revenue - inc.operating_cost) / inc.revenue) * 100

        indicator.net_margin = _safe_div(inc.net_profit, inc.revenue)
        if indicator.net_margin:
            indicator.net_margin = indicator.net_margin * 100

        indicator.net_margin_excl = _safe_div(inc.np_excl_nonrecurring, inc.revenue)
        if indicator.net_margin_excl:
            indicator.net_margin_excl = indicator.net_margin_excl * 100

        # 偿债能力
        indicator.debt_ratio = _safe_div(bal.total_liabilities, bal.total_assets)
        if indicator.debt_ratio:
            indicator.debt_ratio = indicator.debt_ratio * 100

        # 流动/速动比率：银行业资产负债表无流动/非流动分类，不适用
        if not is_bank:
            indicator.current_ratio = _safe_div(bal.current_assets, bal.current_liabilities)

            if bal.current_assets is not None and bal.inventories is not None and bal.current_liabilities:
                quick_assets = bal.current_assets - bal.inventories
                indicator.quick_ratio = _safe_div(quick_assets, bal.current_liabilities)

        # 净负债率：银行业不适用（银行本身就是负债经营主体）
        if not is_bank and bal.total_equity and bal.total_equity != 0:
            interest_bearing_debt = (bal.short_term_borrowings or Decimal(0)) + (bal.long_term_borrowings or Decimal(0))
            net_debt = interest_bearing_debt - (bal.cash_equivalents or Decimal(0))
            indicator.net_debt_ratio = (net_debt / bal.total_equity) * 100

        indicator.equity_ratio = _safe_div(bal.total_liabilities, bal.total_equity)
        if indicator.equity_ratio:
            indicator.equity_ratio = indicator.equity_ratio * 100

        # 运营效率：应收账款/存货/应付账款周转天数，银行不适用
        days = Decimal(360)

        if not is_bank:
            avg_ar = _safe_avg(bal.accounts_receivable, prev_bal.accounts_receivable if prev_bal else None)
            indicator.dso = _safe_div(avg_ar, inc.revenue)
            if indicator.dso:
                indicator.dso = indicator.dso * days

            avg_inv = _safe_avg(bal.inventories, prev_bal.inventories if prev_bal else None)
            indicator.dio = _safe_div(avg_inv, inc.operating_cost)
            if indicator.dio:
                indicator.dio = indicator.dio * days

            avg_ap = _safe_avg(bal.accounts_payable, prev_bal.accounts_payable if prev_bal else None)
            indicator.dpo = _safe_div(avg_ap, inc.operating_cost)
            if indicator.dpo:
                indicator.dpo = indicator.dpo * days

            if indicator.dso is not None and indicator.dio is not None and indicator.dpo is not None:
                indicator.ccc = indicator.dso + indicator.dio - indicator.dpo

        indicator.asset_turnover = _safe_div(inc.revenue, avg_assets)

        # 成长性
        if prev_inc:
            indicator.revenue_growth = _calc_growth(inc.revenue, prev_inc.revenue)
            indicator.np_parent_growth = _calc_growth(inc.np_parent_company, prev_inc.np_parent_company)
            indicator.np_excl_growth = _calc_growth(inc.np_excl_nonrecurring, prev_inc.np_excl_nonrecurring)

        if prev_cf:
            indicator.cfo_growth = _calc_growth(cf.cf_operating, prev_cf.cf_operating)

        if prev_bal:
            indicator.equity_growth = _calc_growth(bal.total_equity, prev_bal.total_equity)
            indicator.asset_growth = _calc_growth(bal.total_assets, prev_bal.total_assets)

        # 现金流质量
        indicator.cfo_to_np = _safe_div(cf.cf_operating, inc.net_profit)
        if indicator.cfo_to_np:
            indicator.cfo_to_np = indicator.cfo_to_np * 100

        return indicator
