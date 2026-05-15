"""PostgreSQL 利润表仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.financial_income import FinancialIncome
from data_collector.infrastructure.db import execute_query, execute_update, transaction

logger = structlog.get_logger(__name__)


class DbFinancialIncomeRepository:
    """基于 PostgreSQL 的利润表仓库实现。"""

    def save(self, income: FinancialIncome, conn=None) -> None:
        """保存或更新利润表数据（Upsert 语义）。

        Args:
            income: 利润表领域对象。
            conn: 可选的数据库连接，用于在显式事务中批量执行。
        """
        if income.id is None:
            income.id = str(ulid.ULID())

        sql = """
        INSERT INTO tb_financial_income (
            id, stock_code, report_date, report_type, basic_eps, diluted_eps,
            total_revenue, revenue, operating_cost, gross_profit,
            selling_expense, admin_expense, rd_expense, financial_expense,
            operating_profit, total_profit, net_profit, np_parent_company,
            np_excl_nonrecurring, created_at, updated_at
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
            NOW(), NOW()
        )
        ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
            basic_eps = EXCLUDED.basic_eps,
            diluted_eps = EXCLUDED.diluted_eps,
            total_revenue = EXCLUDED.total_revenue,
            revenue = EXCLUDED.revenue,
            operating_cost = EXCLUDED.operating_cost,
            gross_profit = EXCLUDED.gross_profit,
            selling_expense = EXCLUDED.selling_expense,
            admin_expense = EXCLUDED.admin_expense,
            rd_expense = EXCLUDED.rd_expense,
            financial_expense = EXCLUDED.financial_expense,
            operating_profit = EXCLUDED.operating_profit,
            total_profit = EXCLUDED.total_profit,
            net_profit = EXCLUDED.net_profit,
            np_parent_company = EXCLUDED.np_parent_company,
            np_excl_nonrecurring = EXCLUDED.np_excl_nonrecurring,
            updated_at = NOW()
        """
        params = (
            income.id, income.stock_code, income.report_date, income.report_type,
            income.basic_eps, income.diluted_eps, income.total_revenue, income.revenue,
            income.operating_cost, income.gross_profit, income.selling_expense,
            income.admin_expense, income.rd_expense, income.financial_expense,
            income.operating_profit, income.total_profit, income.net_profit,
            income.np_parent_company, income.np_excl_nonrecurring,
        )
        if conn is not None:
            cursor = conn.cursor()
            cursor.execute(sql, params)
            cursor.close()
        else:
            execute_update(sql, params)
        logger.debug("利润表已保存", stock_code=income.stock_code, report_date=income.report_date)

    def save_all(self, incomes: Sequence[FinancialIncome]) -> tuple[int, int]:
        """批量保存利润表，返回 (成功数, 失败数)。

        采用显式事务批量提交，减少数据库往返开销；
        单条失败仅跳过当前记录，不回滚整个批次。
        """
        success = 0
        failed = 0
        with transaction() as conn:
            for income in incomes:
                try:
                    self.save(income, conn=conn)
                    success += 1
                except Exception as e:
                    logger.warning(
                        "批量保存利润表失败",
                        stock_code=income.stock_code,
                        report_date=income.report_date,
                        error=str(e),
                    )
                    failed += 1
        logger.info("利润表批量保存完成", total=len(incomes), success=success, failed=failed)
        return success, failed

    def find_by_stock_code(
        self, stock_code: str, report_type: str | None = None, limit: int = 20
    ) -> Sequence[FinancialIncome]:
        """根据股票代码查询利润表数据。"""
        sql = """
        SELECT * FROM tb_financial_income
        WHERE stock_code = %s
        """
        params: list = [stock_code]
        if report_type:
            sql += " AND report_type = %s"
            params.append(report_type)
        sql += " ORDER BY report_date DESC LIMIT %s"
        params.append(limit)
        rows = execute_query(sql, tuple(params))
        return [FinancialIncome.from_dict(row) for row in rows]

    def find_latest(self, stock_code: str, report_type: str = "Y") -> FinancialIncome | None:
        """查询最新一期利润表。"""
        sql = """
        SELECT * FROM tb_financial_income
        WHERE stock_code = %s AND report_type = %s
        ORDER BY report_date DESC LIMIT 1
        """
        rows = execute_query(sql, (stock_code, report_type))
        if not rows:
            return None
        return FinancialIncome.from_dict(rows[0])
