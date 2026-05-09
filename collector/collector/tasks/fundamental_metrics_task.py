"""基本面衍生指标预计算任务

从 financial_report 表读取原始财务三表数据，计算跨期/跨表衍生指标，
物化到 stock_fundamental_metrics 表，为阶段B的同比箭头、杜邦分析、行业排名提供数据基础。

触发方式：
- 独立手动执行：python main.py fundamental-metrics
- FinanceTask 完成后自动调用（推荐，保证数据一致性）
"""

import logging
from typing import List, Dict, Optional, Any
from collections import defaultdict
from decimal import Decimal, InvalidOperation

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)

# 年报类型过滤（仅处理年报，避免中报/季报干扰年度同比）
_ANNUAL_REPORT_TYPE = "年报"

# 批次大小（每批处理的股票数）
_BATCH_SIZE = 100


class FundamentalMetricsTask(BaseTask):
    """基本面衍生指标预计算任务"""

    task_name = "fundamental_metrics"
    data_type = "fundamental_metrics"

    # 查询某只股票所有年报数据的 SQL
    _SELECT_SQL = """
        SELECT
            report_year,
            total_revenue,
            operate_income,
            operate_cost,
            parent_net_profit,
            total_assets,
            total_liabilities,
            total_equity,
            total_current_assets,
            total_noncurrent_assets,
            total_current_liabilities,
            total_noncurrent_liabilities,
            inventory,
            operating_cash_flow,
            investing_cash_flow,
            financing_cash_flow,
            sale_expense,
            manage_expense,
            research_expense,
            finance_expense
        FROM financial_report
        WHERE stock_code = %s
          AND report_type = '年报'
          AND is_deleted = FALSE
        ORDER BY report_year ASC
    """

    # upsert SQL
    _UPSERT_SQL = """
        INSERT INTO stock_fundamental_metrics (
            stock_code, report_year,
            revenue_yoy, profit_yoy, asset_growth_rate,
            roe, roa, asset_turnover, equity_multiplier,
            current_ratio, quick_ratio,
            cashflow_profit_ratio, period_expense_rate,
            is_deleted, deleted_at, created_at, updated_at
        ) VALUES (
            %s, %s,
            %s, %s, %s,
            %s, %s, %s, %s,
            %s, %s,
            %s, %s,
            FALSE, NULL, NOW(), NOW()
        )
        ON CONFLICT (stock_code, report_year) DO UPDATE SET
            revenue_yoy = EXCLUDED.revenue_yoy,
            profit_yoy = EXCLUDED.profit_yoy,
            asset_growth_rate = EXCLUDED.asset_growth_rate,
            roe = EXCLUDED.roe,
            roa = EXCLUDED.roa,
            asset_turnover = EXCLUDED.asset_turnover,
            equity_multiplier = EXCLUDED.equity_multiplier,
            current_ratio = EXCLUDED.current_ratio,
            quick_ratio = EXCLUDED.quick_ratio,
            cashflow_profit_ratio = EXCLUDED.cashflow_profit_ratio,
            period_expense_rate = EXCLUDED.period_expense_rate,
            is_deleted = FALSE,
            deleted_at = NULL,
            updated_at = NOW()
    """

    def __init__(
        self,
        db: PostgresDB,
        source: BaseDataSource,
        monitor: Optional[Monitor] = None,
        batch_size: int = _BATCH_SIZE,
    ):
        super().__init__(db=db, source=source, monitor=monitor)
        self._batch_size = batch_size

    # ------------------------------------------------------------------
    # BaseTask 接口实现
    # ------------------------------------------------------------------

    def run_full(self, **kwargs) -> TaskResult:
        """全量计算：遍历所有有年报数据的股票。"""
        stock_codes = self._get_stock_codes_with_report()
        logger.info(f"[FundamentalMetrics] 全量计算开始，共 {len(stock_codes)} 只股票")
        return self._process_codes(stock_codes)

    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """按指定股票代码列表计算。"""
        logger.info(f"[FundamentalMetrics] 局部计算开始，共 {len(identifiers)} 只股票")
        return self._process_codes(identifiers)

    def run_incremental(self, **kwargs) -> TaskResult:
        """增量计算：仅处理最近一个完整年报年度。

        例如当前为 2026-05-09，最近完整年报年度为 2025。
        只处理 report_year = 最近年度的记录（仍需前一年数据计算同比）。
        """
        latest_year = self._get_latest_report_year()
        if latest_year is None:
            logger.warning("[FundamentalMetrics] 无年报数据，跳过增量计算")
            return TaskResult()

        logger.info(f"[FundamentalMetrics] 增量计算开始，目标年度 {latest_year}")
        # 获取有该年度年报的股票
        rows = self.db.fetchall(
            """
            SELECT DISTINCT stock_code FROM financial_report
            WHERE report_year = %s AND report_type = '年报' AND is_deleted = FALSE
            """,
            (latest_year,),
        )
        stock_codes = [r[0] for r in rows if r]
        return self._process_codes(stock_codes, target_year=latest_year)

    # ------------------------------------------------------------------
    # 核心处理逻辑
    # ------------------------------------------------------------------

    def _process_codes(
        self,
        stock_codes: List[str],
        target_year: Optional[int] = None,
    ) -> TaskResult:
        """批量处理股票列表，可选只保留目标年份的结果。"""
        total_created = 0
        total_updated = 0
        total_failed = 0

        total = len(stock_codes)
        for i in range(0, total, self._batch_size):
            batch = stock_codes[i : i + self._batch_size]
            batch_num = i // self._batch_size + 1
            total_batches = (total + self._batch_size - 1) // self._batch_size

            logger.info(
                f"[FundamentalMetrics] 处理批次 {batch_num}/{total_batches} "
                f"({i + 1}-{min(i + self._batch_size, total)} / {total})"
            )

            batch_params = []
            for stock_code in batch:
                try:
                    metrics = self._compute_stock_metrics(stock_code, target_year=target_year)
                    for m in metrics:
                        batch_params.append(self._to_upsert_tuple(stock_code, m))
                except Exception as e:
                    logger.error(f"[FundamentalMetrics] 计算 {stock_code} 失败: {e}")
                    total_failed += 1

            if batch_params:
                try:
                    self.db.upsert_many(self._UPSERT_SQL, batch_params)
                    total_updated += len(batch_params)
                    logger.info(
                        f"[FundamentalMetrics] 批次 {batch_num} 完成，upsert {len(batch_params)} 条"
                    )
                except Exception as e:
                    logger.error(f"[FundamentalMetrics] 批次 {batch_num} 写入失败: {e}")
                    total_failed += len(batch)

        logger.info(
            f"[FundamentalMetrics] 计算完成。Updated: {total_updated}, Failed: {total_failed}"
        )
        return TaskResult(created=0, updated=total_updated, failed=total_failed, rows=total_updated)

    def _compute_stock_metrics(
        self,
        stock_code: str,
        target_year: Optional[int] = None,
    ) -> List[Dict[str, Any]]:
        """计算单只股票的衍生指标，返回指标字典列表。"""
        rows = self.db.fetchall(self._SELECT_SQL, (stock_code,))
        if not rows:
            return []

        # 转换为按年份索引的字典
        year_data: Dict[int, Dict[str, Optional[Decimal]]] = {}
        for row in rows:
            year = row[0]
            year_data[year] = {
                "total_revenue": _to_decimal(row[1]),
                "operate_income": _to_decimal(row[2]),
                "operate_cost": _to_decimal(row[3]),
                "parent_net_profit": _to_decimal(row[4]),
                "total_assets": _to_decimal(row[5]),
                "total_liabilities": _to_decimal(row[6]),
                "total_equity": _to_decimal(row[7]),
                "total_current_assets": _to_decimal(row[8]),
                "total_noncurrent_assets": _to_decimal(row[9]),
                "total_current_liabilities": _to_decimal(row[10]),
                "total_noncurrent_liabilities": _to_decimal(row[11]),
                "inventory": _to_decimal(row[12]),
                "operating_cash_flow": _to_decimal(row[13]),
                "investing_cash_flow": _to_decimal(row[14]),
                "financing_cash_flow": _to_decimal(row[15]),
                "sale_expense": _to_decimal(row[16]),
                "manage_expense": _to_decimal(row[17]),
                "research_expense": _to_decimal(row[18]),
                "finance_expense": _to_decimal(row[19]),
            }

        results = []
        sorted_years = sorted(year_data.keys())

        for idx, year in enumerate(sorted_years):
            current = year_data[year]
            prev = year_data.get(year - 1) if idx > 0 else None

            # 如果指定了 target_year，只保留目标年份
            if target_year is not None and year != target_year:
                continue

            metric = {
                "report_year": year,
                # 同比增长率（需要前一年数据）
                "revenue_yoy": _safe_yoy(current.get("total_revenue"), prev.get("total_revenue")) if prev else None,
                "profit_yoy": _safe_yoy(current.get("parent_net_profit"), prev.get("parent_net_profit")) if prev else None,
                "asset_growth_rate": _safe_yoy(current.get("total_assets"), prev.get("total_assets")) if prev else None,
                # 效率指标（使用平均余额，首年无前期数据时回退到期末值）
                "roe": _safe_percentage(current.get("parent_net_profit"), _avg(current.get("total_equity"), prev.get("total_equity") if prev else None)),
                "roa": _safe_percentage(current.get("parent_net_profit"), _avg(current.get("total_assets"), prev.get("total_assets") if prev else None)),
                "asset_turnover": _safe_divide(current.get("operate_income"), _avg(current.get("total_assets"), prev.get("total_assets") if prev else None)),
                "equity_multiplier": _safe_divide(current.get("total_assets"), current.get("total_equity")),
                # 偿债指标
                "current_ratio": _safe_divide(current.get("total_current_assets"), current.get("total_current_liabilities")),
                "quick_ratio": _safe_quick_ratio(current.get("total_current_assets"), current.get("inventory"), current.get("total_current_liabilities")),
                # 盈利质量
                "cashflow_profit_ratio": _safe_percentage(current.get("operating_cash_flow"), current.get("parent_net_profit")),
                "period_expense_rate": _safe_period_expense_rate(
                    current.get("sale_expense"),
                    current.get("manage_expense"),
                    current.get("research_expense"),
                    current.get("finance_expense"),
                    current.get("operate_income"),
                ),
            }
            results.append(metric)

        return results

    # ------------------------------------------------------------------
    # 工具方法
    # ------------------------------------------------------------------

    def _get_stock_codes_with_report(self) -> List[str]:
        """获取所有有年报数据的股票代码。"""
        rows = self.db.fetchall(
            """
            SELECT DISTINCT stock_code FROM financial_report
            WHERE report_type = '年报' AND is_deleted = FALSE
            ORDER BY stock_code
            """
        )
        return [r[0] for r in rows if r and r[0]]

    def _get_latest_report_year(self) -> Optional[int]:
        """获取 financial_report 中最大的年报年度。"""
        row = self.db.fetchone(
            """
            SELECT MAX(report_year) FROM financial_report
            WHERE report_type = '年报' AND is_deleted = FALSE
            """
        )
        return row[0] if row and row[0] else None

    @staticmethod
    def _to_upsert_tuple(stock_code: str, metric: Dict[str, Any]) -> tuple:
        return (
            stock_code,
            metric["report_year"],
            metric["revenue_yoy"],
            metric["profit_yoy"],
            metric["asset_growth_rate"],
            metric["roe"],
            metric["roa"],
            metric["asset_turnover"],
            metric["equity_multiplier"],
            metric["current_ratio"],
            metric["quick_ratio"],
            metric["cashflow_profit_ratio"],
            metric["period_expense_rate"],
        )


# ------------------------------------------------------------------
# 安全计算工具函数
# ------------------------------------------------------------------

def _to_decimal(value) -> Optional[Decimal]:
    """将数据库返回值安全转为 Decimal。"""
    if value is None:
        return None
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError, TypeError):
        return None


def _safe_divide(numerator: Optional[Decimal], denominator: Optional[Decimal]) -> Optional[Decimal]:
    """安全除法，保留4位小数。"""
    if numerator is None or denominator is None:
        return None
    if denominator == 0:
        return None
    try:
        return (numerator / denominator).quantize(Decimal("0.0001"))
    except Exception:
        return None


def _safe_percentage(numerator: Optional[Decimal], denominator: Optional[Decimal]) -> Optional[Decimal]:
    """安全百分比（结果乘以100），保留4位小数。"""
    result = _safe_divide(numerator, denominator)
    if result is None:
        return None
    try:
        return (result * 100).quantize(Decimal("0.0001"))
    except Exception:
        return None


def _safe_yoy(current: Optional[Decimal], previous: Optional[Decimal]) -> Optional[Decimal]:
    """安全同比增长率 = (current - previous) / previous * 100。"""
    if current is None or previous is None:
        return None
    if previous == 0:
        return None
    try:
        return ((current - previous) / previous * 100).quantize(Decimal("0.0001"))
    except Exception:
        return None


def _safe_quick_ratio(
    current_assets: Optional[Decimal],
    inventory: Optional[Decimal],
    current_liabilities: Optional[Decimal],
) -> Optional[Decimal]:
    """速动比率 = (流动资产 - 存货) / 流动负债。"""
    if current_assets is None or current_liabilities is None:
        return None
    if current_liabilities == 0:
        return None
    quick_assets = current_assets - (inventory or Decimal(0))
    try:
        return (quick_assets / current_liabilities).quantize(Decimal("0.0001"))
    except Exception:
        return None


def _safe_period_expense_rate(
    sale: Optional[Decimal],
    manage: Optional[Decimal],
    research: Optional[Decimal],
    finance: Optional[Decimal],
    operate_income: Optional[Decimal],
) -> Optional[Decimal]:
    """期间费用率 = (销售+管理+研发+财务费用) / 营业收入 * 100。"""
    if operate_income is None or operate_income == 0:
        return None
    total = (sale or Decimal(0)) + (manage or Decimal(0)) + (research or Decimal(0)) + (finance or Decimal(0))
    try:
        return (total / operate_income * 100).quantize(Decimal("0.0001"))
    except Exception:
        return None


def _avg(current: Optional[Decimal], previous: Optional[Decimal]) -> Optional[Decimal]:
    """计算平均值：(当期 + 上期) / 2。若上期缺失则返回当期。"""
    if current is None:
        return None
    if previous is None:
        return current
    try:
        return ((current + previous) / Decimal(2)).quantize(Decimal("0.0001"))
    except Exception:
        return current
