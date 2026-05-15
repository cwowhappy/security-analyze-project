"""PostgreSQL 现金流量表仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.financial_cashflow import FinancialCashflow
from data_collector.infrastructure.db import execute_query, execute_update, transaction

logger = structlog.get_logger(__name__)


class DbFinancialCashflowRepository:
    """基于 PostgreSQL 的现金流量表仓库实现。"""

    def save(self, cashflow: FinancialCashflow, conn=None) -> None:
        """保存或更新现金流量表数据（Upsert 语义）。

        Args:
            cashflow: 现金流量表领域对象。
            conn: 可选的数据库连接，用于在显式事务中批量执行。
        """
        if cashflow.id is None:
            cashflow.id = str(ulid.ULID())

        sql = """
        INSERT INTO tb_financial_cashflow (
            id, stock_code, report_date, report_type, cf_operating, cf_investing,
            cf_financing, net_cash_flow, free_cash_flow, capex,
            cash_received_operating, tax_paid, created_at, updated_at
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW()
        )
        ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
            cf_operating = EXCLUDED.cf_operating,
            cf_investing = EXCLUDED.cf_investing,
            cf_financing = EXCLUDED.cf_financing,
            net_cash_flow = EXCLUDED.net_cash_flow,
            free_cash_flow = EXCLUDED.free_cash_flow,
            capex = EXCLUDED.capex,
            cash_received_operating = EXCLUDED.cash_received_operating,
            tax_paid = EXCLUDED.tax_paid,
            updated_at = NOW()
        """
        params = (
            cashflow.id, cashflow.stock_code, cashflow.report_date, cashflow.report_type,
            cashflow.cf_operating, cashflow.cf_investing, cashflow.cf_financing,
            cashflow.net_cash_flow, cashflow.free_cash_flow, cashflow.capex,
            cashflow.cash_received_operating, cashflow.tax_paid,
        )
        if conn is not None:
            cursor = conn.cursor()
            cursor.execute(sql, params)
            cursor.close()
        else:
            execute_update(sql, params)
        logger.debug("现金流量表已保存", stock_code=cashflow.stock_code, report_date=cashflow.report_date)

    def save_all(self, cashflows: Sequence[FinancialCashflow]) -> tuple[int, int]:
        """批量保存现金流量表，返回 (成功数, 失败数)。

        采用显式事务批量提交，减少数据库往返开销；
        单条失败仅跳过当前记录，不回滚整个批次。
        """
        success = 0
        failed = 0
        with transaction() as conn:
            for cashflow in cashflows:
                try:
                    self.save(cashflow, conn=conn)
                    success += 1
                except Exception as e:
                    logger.warning(
                        "批量保存现金流量表失败",
                        stock_code=cashflow.stock_code,
                        report_date=cashflow.report_date,
                        error=str(e),
                    )
                    failed += 1
        logger.info("现金流量表批量保存完成", total=len(cashflows), success=success, failed=failed)
        return success, failed

    def find_by_stock_code(
        self, stock_code: str, report_type: str | None = None, limit: int = 20
    ) -> Sequence[FinancialCashflow]:
        """根据股票代码查询现金流量表数据。"""
        sql = """
        SELECT * FROM tb_financial_cashflow
        WHERE stock_code = %s
        """
        params: list = [stock_code]
        if report_type:
            sql += " AND report_type = %s"
            params.append(report_type)
        sql += " ORDER BY report_date DESC LIMIT %s"
        params.append(limit)
        rows = execute_query(sql, tuple(params))
        return [FinancialCashflow.from_dict(row) for row in rows]

    def find_latest(self, stock_code: str, report_type: str = "Y") -> FinancialCashflow | None:
        """查询最新一期现金流量表。"""
        sql = """
        SELECT * FROM tb_financial_cashflow
        WHERE stock_code = %s AND report_type = %s
        ORDER BY report_date DESC LIMIT 1
        """
        rows = execute_query(sql, (stock_code, report_type))
        if not rows:
            return None
        return FinancialCashflow.from_dict(rows[0])
