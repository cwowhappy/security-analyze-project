"""PostgreSQL 股票仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.stock import Stock
from data_collector.core.ports.stock_repository import StockRepository
from data_collector.infrastructure.db import execute_query, execute_update

logger = structlog.get_logger(__name__)


class DbStockRepository(StockRepository):
    """基于 PostgreSQL 的股票仓库实现。"""

    def save(self, stock: Stock) -> None:
        """保存或更新股票数据（Upsert 语义）。"""
        if stock.id is None:
            stock.id = str(ulid.ULID())

        sql = """
        INSERT INTO tb_stock_basic (
            id, stock_code, ts_code, name, full_name, market, exchange,
            list_date, industry, area, total_shares, float_shares, created_at, updated_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
        ON CONFLICT (stock_code) DO UPDATE SET
            ts_code = EXCLUDED.ts_code,
            name = EXCLUDED.name,
            full_name = EXCLUDED.full_name,
            market = EXCLUDED.market,
            exchange = EXCLUDED.exchange,
            list_date = EXCLUDED.list_date,
            industry = EXCLUDED.industry,
            area = EXCLUDED.area,
            total_shares = EXCLUDED.total_shares,
            float_shares = EXCLUDED.float_shares,
            updated_at = NOW()
        """
        params = (
            stock.id,
            stock.stock_code,
            stock.ts_code,
            stock.name,
            stock.full_name,
            stock.market,
            stock.exchange,
            stock.list_date,
            stock.industry,
            stock.area,
            stock.total_shares,
            stock.float_shares,
        )
        execute_update(sql, params)
        logger.debug("股票已保存", stock_code=stock.stock_code, name=stock.name)

    def save_all(self, stocks: Sequence[Stock]) -> tuple[int, int]:
        """批量保存股票，返回 (成功数, 失败数)。"""
        success = 0
        failed = 0
        for stock in stocks:
            try:
                self.save(stock)
                success += 1
            except Exception as e:
                logger.warning(
                    "批量保存股票失败",
                    stock_code=stock.stock_code,
                    error=str(e),
                )
                failed += 1
        logger.info("批量保存完成", total=len(stocks), success=success, failed=failed)
        return success, failed

    def find_by_symbol(self, stock_code: str) -> Stock | None:
        """根据股票代码查询。"""
        sql = """
        SELECT * FROM tb_stock_basic WHERE stock_code = %s
        """
        rows = execute_query(sql, (stock_code,))
        if not rows:
            return None
        return Stock.from_dict(rows[0])

    def find_all(self) -> Sequence[Stock]:
        """查询所有股票。"""
        sql = "SELECT * FROM tb_stock_basic ORDER BY stock_code"
        rows = execute_query(sql)
        return [Stock.from_dict(row) for row in rows]

    def count(self) -> int:
        """返回股票总数。"""
        sql = "SELECT COUNT(*) as cnt FROM tb_stock_basic"
        rows = execute_query(sql)
        return rows[0]["cnt"] if rows else 0
