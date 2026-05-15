"""PostgreSQL 财务指标仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.financial_indicator import FinancialIndicator
from data_collector.infrastructure.db import execute_query, execute_update, transaction

logger = structlog.get_logger(__name__)


class DbFinancialIndicatorRepository:
    """基于 PostgreSQL 的财务指标仓库实现。"""

    def save(self, indicator: FinancialIndicator, conn=None) -> None:
        """保存或更新财务指标数据（Upsert 语义）。

        Args:
            indicator: 财务指标领域对象。
            conn: 可选的数据库连接，用于在显式事务中批量执行。
        """
        if indicator.id is None:
            indicator.id = str(ulid.ULID())

        sql = """
        INSERT INTO tb_financial_indicator (
            id, stock_code, report_date, report_type,
            roe, roa, roic, gross_margin, net_margin, net_margin_excl,
            debt_ratio, current_ratio, quick_ratio, net_debt_ratio, equity_ratio,
            dso, dio, dpo, ccc, asset_turnover, fixed_asset_turnover,
            revenue_growth, np_parent_growth, np_excl_growth, cfo_growth,
            equity_growth, asset_growth,
            pe, pb, ps, peg, ev_ebitda, dividend_yield, market_cap,
            cfo_to_np, data_source, created_at, updated_at
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW()
        )
        ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
            roe = EXCLUDED.roe, roa = EXCLUDED.roa, roic = EXCLUDED.roic,
            gross_margin = EXCLUDED.gross_margin, net_margin = EXCLUDED.net_margin,
            net_margin_excl = EXCLUDED.net_margin_excl,
            debt_ratio = EXCLUDED.debt_ratio, current_ratio = EXCLUDED.current_ratio,
            quick_ratio = EXCLUDED.quick_ratio, net_debt_ratio = EXCLUDED.net_debt_ratio,
            equity_ratio = EXCLUDED.equity_ratio,
            dso = EXCLUDED.dso, dio = EXCLUDED.dio, dpo = EXCLUDED.dpo,
            ccc = EXCLUDED.ccc, asset_turnover = EXCLUDED.asset_turnover,
            fixed_asset_turnover = EXCLUDED.fixed_asset_turnover,
            revenue_growth = EXCLUDED.revenue_growth,
            np_parent_growth = EXCLUDED.np_parent_growth,
            np_excl_growth = EXCLUDED.np_excl_growth,
            cfo_growth = EXCLUDED.cfo_growth,
            equity_growth = EXCLUDED.equity_growth,
            asset_growth = EXCLUDED.asset_growth,
            pe = EXCLUDED.pe, pb = EXCLUDED.pb, ps = EXCLUDED.ps,
            peg = EXCLUDED.peg, ev_ebitda = EXCLUDED.ev_ebitda,
            dividend_yield = EXCLUDED.dividend_yield, market_cap = EXCLUDED.market_cap,
            cfo_to_np = EXCLUDED.cfo_to_np,
            data_source = EXCLUDED.data_source,
            updated_at = NOW()
        """
        params = (
            indicator.id, indicator.stock_code, indicator.report_date, indicator.report_type,
            indicator.roe, indicator.roa, indicator.roic, indicator.gross_margin,
            indicator.net_margin, indicator.net_margin_excl,
            indicator.debt_ratio, indicator.current_ratio, indicator.quick_ratio,
            indicator.net_debt_ratio, indicator.equity_ratio,
            indicator.dso, indicator.dio, indicator.dpo, indicator.ccc,
            indicator.asset_turnover, indicator.fixed_asset_turnover,
            indicator.revenue_growth, indicator.np_parent_growth,
            indicator.np_excl_growth, indicator.cfo_growth,
            indicator.equity_growth, indicator.asset_growth,
            indicator.pe, indicator.pb, indicator.ps, indicator.peg,
            indicator.ev_ebitda, indicator.dividend_yield, indicator.market_cap,
            indicator.cfo_to_np, indicator.data_source,
        )
        if conn is not None:
            cursor = conn.cursor()
            cursor.execute(sql, params)
            cursor.close()
        else:
            execute_update(sql, params)
        logger.debug("财务指标已保存", stock_code=indicator.stock_code, report_date=indicator.report_date)

    def save_all(self, indicators: Sequence[FinancialIndicator]) -> tuple[int, int]:
        """批量保存财务指标，返回 (成功数, 失败数)。

        采用显式事务批量提交，减少数据库往返开销；
        单条失败仅跳过当前记录，不回滚整个批次。
        """
        success = 0
        failed = 0
        with transaction() as conn:
            for indicator in indicators:
                try:
                    self.save(indicator, conn=conn)
                    success += 1
                except Exception as e:
                    logger.warning(
                        "批量保存财务指标失败",
                        stock_code=indicator.stock_code,
                        report_date=indicator.report_date,
                        error=str(e),
                    )
                    failed += 1
        logger.info("财务指标批量保存完成", total=len(indicators), success=success, failed=failed)
        return success, failed

    def find_by_stock_code(
        self, stock_code: str, report_type: str | None = None, limit: int = 20
    ) -> Sequence[FinancialIndicator]:
        """根据股票代码查询财务指标数据。"""
        sql = """
        SELECT * FROM tb_financial_indicator
        WHERE stock_code = %s
        """
        params: list = [stock_code]
        if report_type:
            sql += " AND report_type = %s"
            params.append(report_type)
        sql += " ORDER BY report_date DESC LIMIT %s"
        params.append(limit)
        rows = execute_query(sql, tuple(params))
        return [FinancialIndicator.from_dict(row) for row in rows]

    def find_latest(self, stock_code: str, report_type: str = "Y") -> FinancialIndicator | None:
        """查询最新一期财务指标。"""
        sql = """
        SELECT * FROM tb_financial_indicator
        WHERE stock_code = %s AND report_type = %s
        ORDER BY report_date DESC LIMIT 1
        """
        rows = execute_query(sql, (stock_code, report_type))
        if not rows:
            return None
        return FinancialIndicator.from_dict(rows[0])
