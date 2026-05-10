"""股票估值指标预计算任务（阶段C）

从 daily_quote、company_security、financial_report 三表联查，
计算每日 PE/PB/PS 及历史分位数，物化到 stock_valuation_metrics 表。

触发方式：
- 独立手动执行：python main.py valuation-metrics
- 推荐在 quote_task 完成后自动调用（保证收盘价最新）

计算逻辑：
- 总市值 = close_price × total_shares
- PE_LYR = 总市值 / 最近年报归母净利润
- PE_TTM = 总市值 / 最近4个季度归母净利润之和
- PB = 总市值 / 最近年报总权益
- PS_TTM = 总市值 / 最近4个季度营业收入之和
- 历史分位数 = 该指标近5年所有交易日值的累积分布位置
"""

import logging
from typing import List, Dict, Optional, Any, Tuple
from decimal import Decimal, InvalidOperation
from datetime import datetime, timedelta

import numpy as np

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)

_BATCH_SIZE = 100


def _to_decimal(val: Any) -> Optional[Decimal]:
    """安全转换为 Decimal"""
    if val is None:
        return None
    try:
        d = Decimal(str(val))
        if d.is_nan():
            return None
        return d
    except (InvalidOperation, ValueError, TypeError):
        return None


def _safe_div(numerator: Optional[Decimal], denominator: Optional[Decimal]) -> Optional[Decimal]:
    """安全除法，除数为零或None时返回None"""
    if numerator is None or denominator is None:
        return None
    if denominator == 0:
        return None
    return numerator / denominator


class ValuationMetricsTask(BaseTask):
    """估值指标预计算任务"""

    task_name = "valuation_metrics"
    data_type = "valuation_metrics"

    # 查询某只股票的所有日行情（按日期升序）
    _SELECT_QUOTES_SQL = """
        SELECT trade_date, close_price
        FROM daily_quote
        WHERE stock_code = %s
        ORDER BY trade_date ASC
    """

    # 查询总股本
    _SELECT_SHARES_SQL = """
        SELECT total_shares FROM company_security WHERE stock_code = %s
    """

    # 查询最近年报数据（用于 PE_LYR / PB）
    _SELECT_LATEST_ANNUAL_SQL = """
        SELECT report_year, total_revenue, parent_net_profit, total_equity
        FROM financial_report
        WHERE stock_code = %s
          AND report_type = '年报'
          AND is_deleted = FALSE
        ORDER BY report_date DESC
        LIMIT 1
    """

    # 查询最近4个季度数据（用于 TTM 计算）
    _SELECT_LATEST_4Q_SQL = """
        SELECT report_date, report_year, report_type,
               total_revenue, parent_net_profit
        FROM financial_report
        WHERE stock_code = %s
          AND report_type IN ('一季报', '半年报', '三季报', '年报')
          AND is_deleted = FALSE
        ORDER BY report_date DESC
        LIMIT 4
    """

    # 查询最近年报经营现金流（用于 DCF）
    _SELECT_LATEST_OCF_SQL = """
        SELECT operating_cash_flow
        FROM financial_report
        WHERE stock_code = %s
          AND report_type = '年报'
          AND is_deleted = FALSE
        ORDER BY report_date DESC
        LIMIT 1
    """

    # upsert SQL
    _UPSERT_SQL = """
        INSERT INTO stock_valuation_metrics (
            stock_code, trade_date, close_price,
            pe_ttm, pe_lyr, pb, ps_ttm,
            pe_ttm_percentile, pb_percentile, ps_ttm_percentile,
            dcf_fair_price,
            created_at, updated_at
        ) VALUES (
            %s, %s, %s,
            %s, %s, %s, %s,
            %s, %s, %s,
            %s,
            NOW(), NOW()
        )
        ON CONFLICT (stock_code, trade_date) DO UPDATE SET
            close_price = EXCLUDED.close_price,
            pe_ttm = EXCLUDED.pe_ttm,
            pe_lyr = EXCLUDED.pe_lyr,
            pb = EXCLUDED.pb,
            ps_ttm = EXCLUDED.ps_ttm,
            pe_ttm_percentile = EXCLUDED.pe_ttm_percentile,
            pb_percentile = EXCLUDED.pb_percentile,
            ps_ttm_percentile = EXCLUDED.ps_ttm_percentile,
            dcf_fair_price = EXCLUDED.dcf_fair_price,
            updated_at = NOW()
    """

    # 默认 DCF 参数
    _DEFAULT_DCF_GROWTH = Decimal("0.10")       # 10% 增长率
    _DEFAULT_DCF_DISCOUNT = Decimal("0.08")     # 8% 折现率
    _DEFAULT_DCF_TERMINAL_GROWTH = Decimal("0.03")  # 3% 永续增长率
    _DEFAULT_DCF_YEARS = 10

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
        """全量计算：遍历 daily_quote 中所有有行情的股票。"""
        stock_codes = self._get_stock_codes_with_quotes()
        logger.info(f"[ValuationMetrics] 全量计算开始，共 {len(stock_codes)} 只股票")
        return self._process_codes(stock_codes)

    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """按指定股票代码列表计算。"""
        logger.info(f"[ValuationMetrics] 局部计算开始，共 {len(identifiers)} 只股票")
        return self._process_codes(identifiers)

    def run_incremental(self, **kwargs) -> TaskResult:
        """增量计算：仅处理 daily_quote 中最新日期之后的数据。

        实际实现为：找出每只股票在 stock_valuation_metrics 中已有的最大日期，
        只计算该日期之后的 daily_quote 记录。
        """
        logger.info("[ValuationMetrics] 增量计算开始")
        stock_codes = self._get_stock_codes_with_quotes()
        incremental_codes = []
        for code in stock_codes:
            latest_val_date = self._get_latest_valuation_date(code)
            latest_quote_date = self._get_latest_quote_date(code)
            if latest_val_date is None or (latest_quote_date and latest_quote_date > latest_val_date):
                incremental_codes.append(code)

        logger.info(f"[ValuationMetrics] 增量计算，{len(incremental_codes)} 只股票需要更新")
        return self._process_codes(incremental_codes)

    # ------------------------------------------------------------------
    # 数据获取
    # ------------------------------------------------------------------

    def _get_stock_codes_with_quotes(self) -> List[str]:
        """获取所有有日行情数据的股票代码列表。"""
        rows = self.db.fetchall(
            "SELECT DISTINCT stock_code FROM daily_quote ORDER BY stock_code"
        )
        return [r[0] for r in rows if r]

    def _get_latest_valuation_date(self, stock_code: str) -> Optional[datetime.date]:
        """获取某股票已有估值数据的最大日期。"""
        row = self.db.fetchone(
            "SELECT MAX(trade_date) FROM stock_valuation_metrics WHERE stock_code = %s",
            (stock_code,),
        )
        return row[0] if row and row[0] else None

    def _get_latest_quote_date(self, stock_code: str) -> Optional[datetime.date]:
        """获取某股票日行情的最大日期。"""
        row = self.db.fetchone(
            "SELECT MAX(trade_date) FROM daily_quote WHERE stock_code = %s",
            (stock_code,),
        )
        return row[0] if row and row[0] else None

    # ------------------------------------------------------------------
    # 核心处理逻辑
    # ------------------------------------------------------------------

    def _process_codes(self, stock_codes: List[str]) -> TaskResult:
        """批量处理股票列表。"""
        total_updated = 0
        total_failed = 0

        total = len(stock_codes)
        for i in range(0, total, self._batch_size):
            batch = stock_codes[i : i + self._batch_size]
            batch_num = i // self._batch_size + 1
            total_batches = (total + self._batch_size - 1) // self._batch_size

            logger.info(
                f"[ValuationMetrics] 处理批次 {batch_num}/{total_batches} "
                f"({i + 1}-{min(i + self._batch_size, total)} / {total})"
            )

            batch_params = []
            for stock_code in batch:
                try:
                    params_list = self._compute_stock_valuations(stock_code)
                    batch_params.extend(params_list)
                except Exception as e:
                    logger.error(f"[ValuationMetrics] 计算 {stock_code} 失败: {e}")
                    total_failed += 1

            if batch_params:
                try:
                    self.db.upsert_many(self._UPSERT_SQL, batch_params)
                    total_updated += len(batch_params)
                    logger.info(
                        f"[ValuationMetrics] 批次 {batch_num} 完成，upsert {len(batch_params)} 条"
                    )
                except Exception as e:
                    logger.error(f"[ValuationMetrics] 批次 {batch_num} 写入失败: {e}")
                    total_failed += len(batch)

        logger.info(
            f"[ValuationMetrics] 计算完成。Updated: {total_updated}, Failed: {total_failed}"
        )
        return TaskResult(created=0, updated=total_updated, failed=total_failed, rows=total_updated)

    def _compute_stock_valuations(self, stock_code: str) -> List[Tuple]:
        """计算单只股票所有交易日的估值指标，返回 upsert 参数列表。"""
        # 1. 获取日行情
        quote_rows = self.db.fetchall(self._SELECT_QUOTES_SQL, (stock_code,))
        if not quote_rows:
            return []

        # 2. 获取总股本
        shares_row = self.db.fetchone(self._SELECT_SHARES_SQL, (stock_code,))
        total_shares = _to_decimal(shares_row[0]) if shares_row and shares_row[0] else None
        if total_shares is None or total_shares <= 0:
            logger.warning(f"[ValuationMetrics] {stock_code} 总股本缺失或无效，跳过")
            return []

        # 3. 获取最近年报财务数据
        annual_row = self.db.fetchone(self._SELECT_LATEST_ANNUAL_SQL, (stock_code,))
        annual = {
            "total_revenue": _to_decimal(annual_row[1]) if annual_row else None,
            "parent_net_profit": _to_decimal(annual_row[2]) if annual_row else None,
            "total_equity": _to_decimal(annual_row[3]) if annual_row else None,
        }

        # 4. 获取最近4个季度数据（TTM）
        q_rows = self.db.fetchall(self._SELECT_LATEST_4Q_SQL, (stock_code,))
        ttm_revenue = Decimal("0")
        ttm_profit = Decimal("0")
        q_count = 0
        for q_row in q_rows:
            rev = _to_decimal(q_row[3])
            prof = _to_decimal(q_row[4])
            if rev is not None:
                ttm_revenue += rev
            if prof is not None:
                ttm_profit += prof
            q_count += 1

        if q_count < 4:
            # 季度数据不足时回退到年报
            ttm_revenue = annual["total_revenue"] or Decimal("0")
            ttm_profit = annual["parent_net_profit"] or Decimal("0")

        # 5. 获取经营现金流（DCF）
        ocf_row = self.db.fetchone(self._SELECT_LATEST_OCF_SQL, (stock_code,))
        operating_cash_flow = _to_decimal(ocf_row[0]) if ocf_row else None

        # 6. 逐日计算估值指标
        daily_metrics: List[Dict[str, Any]] = []
        for trade_date, close_price in quote_rows:
            price = _to_decimal(close_price)
            if price is None or price <= 0:
                continue

            market_cap = price * total_shares

            pe_lyr = _safe_div(market_cap, annual["parent_net_profit"])
            pe_ttm = _safe_div(market_cap, ttm_profit if ttm_profit > 0 else None)
            pb = _safe_div(market_cap, annual["total_equity"])
            ps_ttm = _safe_div(market_cap, ttm_revenue if ttm_revenue > 0 else None)

            dcf_price = self._compute_dcf_fair_price(
                operating_cash_flow, price, total_shares
            )

            daily_metrics.append({
                "trade_date": trade_date,
                "close_price": price,
                "pe_ttm": pe_ttm,
                "pe_lyr": pe_lyr,
                "pb": pb,
                "ps_ttm": ps_ttm,
                "dcf_fair_price": dcf_price,
            })

        # 7. 计算历史分位数（基于本批次数据）
        self._compute_percentiles(daily_metrics)

        # 8. 转换为 upsert 参数元组
        result = []
        for m in daily_metrics:
            result.append((
                stock_code,
                m["trade_date"],
                m["close_price"],
                m["pe_ttm"],
                m["pe_lyr"],
                m["pb"],
                m["ps_ttm"],
                m.get("pe_ttm_percentile"),
                m.get("pb_percentile"),
                m.get("ps_ttm_percentile"),
                m["dcf_fair_price"],
            ))
        return result

    def _compute_percentiles(self, daily_metrics: List[Dict[str, Any]]) -> None:
        """为每日指标计算历史分位数（基于近5年数据）。

        由于 daily_metrics 已包含该股票所有历史交易日的计算值，
        直接在这些数据上计算累积分布。
        """
        if not daily_metrics:
            return

        # 提取有效值序列
        pe_ttm_vals = [m["pe_ttm"] for m in daily_metrics if m["pe_ttm"] is not None]
        pb_vals = [m["pb"] for m in daily_metrics if m["pb"] is not None]
        ps_ttm_vals = [m["ps_ttm"] for m in daily_metrics if m["ps_ttm"] is not None]

        pe_arr = np.array([float(v) for v in pe_ttm_vals]) if pe_ttm_vals else np.array([])
        pb_arr = np.array([float(v) for v in pb_vals]) if pb_vals else np.array([])
        ps_arr = np.array([float(v) for v in ps_ttm_vals]) if ps_ttm_vals else np.array([])

        for m in daily_metrics:
            if m["pe_ttm"] is not None and len(pe_arr) > 0:
                m["pe_ttm_percentile"] = Decimal(str(np.mean(pe_arr <= float(m["pe_ttm"]))))
            if m["pb"] is not None and len(pb_arr) > 0:
                m["pb_percentile"] = Decimal(str(np.mean(pb_arr <= float(m["pb"]))))
            if m["ps_ttm"] is not None and len(ps_arr) > 0:
                m["ps_ttm_percentile"] = Decimal(str(np.mean(ps_arr <= float(m["ps_ttm"]))))

    def _compute_dcf_fair_price(
        self,
        operating_cash_flow: Optional[Decimal],
        current_price: Decimal,
        total_shares: Decimal,
    ) -> Optional[Decimal]:
        """简易 DCF 计算：使用经营现金流近似自由现金流。

        公式：
        FV = Σ(CF_t / (1+r)^t) + TV / (1+r)^n
        TV = CF_n × (1+g) / (r - g)
        每股公允价 = FV / total_shares
        """
        if operating_cash_flow is None or operating_cash_flow <= 0:
            return None

        cf = float(operating_cash_flow)
        r = float(self._DEFAULT_DCF_DISCOUNT)
        g = float(self._DEFAULT_DCF_GROWTH)
        tg = float(self._DEFAULT_DCF_TERMINAL_GROWTH)
        n = self._DEFAULT_DCF_YEARS
        shares = float(total_shares)

        # 预测期现值
        pv = 0.0
        for t in range(1, n + 1):
            cf_t = cf * ((1 + g) ** t)
            pv += cf_t / ((1 + r) ** t)

        # 永续价值
        cf_n = cf * ((1 + g) ** n)
        terminal_value = cf_n * (1 + tg) / (r - tg)
        pv += terminal_value / ((1 + r) ** n)

        fair_price = pv / shares
        return Decimal(str(fair_price))
