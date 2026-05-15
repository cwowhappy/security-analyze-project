"""PostgreSQL 资产负债表仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.financial_balance import FinancialBalance
from data_collector.infrastructure.db import execute_query, execute_update, transaction

logger = structlog.get_logger(__name__)


class DbFinancialBalanceRepository:
    """基于 PostgreSQL 的资产负债表仓库实现。"""

    def save(self, balance: FinancialBalance, conn=None) -> None:
        """保存或更新资产负债表数据（Upsert 语义）。

        Args:
            balance: 资产负债表领域对象。
            conn: 可选的数据库连接，用于在显式事务中批量执行。
        """
        if balance.id is None:
            balance.id = str(ulid.ULID())

        sql = """
        INSERT INTO tb_financial_balance (
            id, stock_code, report_date, report_type, total_assets, total_liabilities,
            total_equity, equity_parent_company, current_assets, non_current_assets,
            cash_equivalents, accounts_receivable, inventories, current_liabilities,
            non_current_liabilities, accounts_payable, short_term_borrowings,
            long_term_borrowings, goodwill, created_at, updated_at
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
            NOW(), NOW()
        )
        ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
            total_assets = EXCLUDED.total_assets,
            total_liabilities = EXCLUDED.total_liabilities,
            total_equity = EXCLUDED.total_equity,
            equity_parent_company = EXCLUDED.equity_parent_company,
            current_assets = EXCLUDED.current_assets,
            non_current_assets = EXCLUDED.non_current_assets,
            cash_equivalents = EXCLUDED.cash_equivalents,
            accounts_receivable = EXCLUDED.accounts_receivable,
            inventories = EXCLUDED.inventories,
            current_liabilities = EXCLUDED.current_liabilities,
            non_current_liabilities = EXCLUDED.non_current_liabilities,
            accounts_payable = EXCLUDED.accounts_payable,
            short_term_borrowings = EXCLUDED.short_term_borrowings,
            long_term_borrowings = EXCLUDED.long_term_borrowings,
            goodwill = EXCLUDED.goodwill,
            updated_at = NOW()
        """
        params = (
            balance.id, balance.stock_code, balance.report_date, balance.report_type,
            balance.total_assets, balance.total_liabilities, balance.total_equity,
            balance.equity_parent_company, balance.current_assets, balance.non_current_assets,
            balance.cash_equivalents, balance.accounts_receivable, balance.inventories,
            balance.current_liabilities, balance.non_current_liabilities,
            balance.accounts_payable, balance.short_term_borrowings,
            balance.long_term_borrowings, balance.goodwill,
        )
        if conn is not None:
            cursor = conn.cursor()
            cursor.execute(sql, params)
            cursor.close()
        else:
            execute_update(sql, params)
        logger.debug("资产负债表已保存", stock_code=balance.stock_code, report_date=balance.report_date)

    def save_all(self, balances: Sequence[FinancialBalance]) -> tuple[int, int]:
        """批量保存资产负债表，返回 (成功数, 失败数)。

        采用显式事务批量提交，减少数据库往返开销；
        单条失败仅跳过当前记录，不回滚整个批次。
        """
        success = 0
        failed = 0
        with transaction() as conn:
            for balance in balances:
                try:
                    self.save(balance, conn=conn)
                    success += 1
                except Exception as e:
                    logger.warning(
                        "批量保存资产负债表失败",
                        stock_code=balance.stock_code,
                        report_date=balance.report_date,
                        error=str(e),
                    )
                    failed += 1
        logger.info("资产负债表批量保存完成", total=len(balances), success=success, failed=failed)
        return success, failed

    def find_by_stock_code(
        self, stock_code: str, report_type: str | None = None, limit: int = 20
    ) -> Sequence[FinancialBalance]:
        """根据股票代码查询资产负债表数据。"""
        sql = """
        SELECT * FROM tb_financial_balance
        WHERE stock_code = %s
        """
        params: list = [stock_code]
        if report_type:
            sql += " AND report_type = %s"
            params.append(report_type)
        sql += " ORDER BY report_date DESC LIMIT %s"
        params.append(limit)
        rows = execute_query(sql, tuple(params))
        return [FinancialBalance.from_dict(row) for row in rows]

    def find_latest(self, stock_code: str, report_type: str = "Y") -> FinancialBalance | None:
        """查询最新一期资产负债表。"""
        sql = """
        SELECT * FROM tb_financial_balance
        WHERE stock_code = %s AND report_type = %s
        ORDER BY report_date DESC LIMIT 1
        """
        rows = execute_query(sql, (stock_code, report_type))
        if not rows:
            return None
        return FinancialBalance.from_dict(rows[0])
