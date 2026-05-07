import logging
from datetime import datetime
from typing import List, Optional

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource

logger = logging.getLogger(__name__)


class QuoteTask:
    """采集 A 股日行情任务（仅采集当前 portfolio 有持仓的股票）"""

    def __init__(self, db: PostgresDB, source: AkshareSource):
        self.db = db
        self.source = source

    def run(self, trade_date: Optional[str] = None):
        """执行行情采集

        Args:
            trade_date: 交易日期，格式 YYYY-MM-DD，默认为当天
        """
        if trade_date is None:
            trade_date = datetime.now().strftime("%Y-%m-%d")

        logger.info(f"Starting quote task for trade_date={trade_date}")

        stock_codes = self._get_holding_stock_codes()
        if not stock_codes:
            logger.info("No holding stocks found in portfolio, skip quote task")
            return

        logger.info(f"Found {len(stock_codes)} holding stocks to collect")

        # akshare 使用 YYYYMMDD 格式
        start_date = trade_date.replace("-", "")
        end_date = start_date

        quotes = []
        failed = 0

        for idx, stock_code in enumerate(stock_codes):
            try:
                df = self.source.get_stock_daily_quote(
                    stock_code, start_date=start_date, end_date=end_date
                )
                if df is None or df.empty:
                    logger.warning(f"No quote data for {stock_code} on {trade_date}")
                    continue

                row = df.iloc[0]
                quotes.append(self._parse_quote_row(stock_code, trade_date, row))

                if (idx + 1) % 100 == 0:
                    logger.info(f"Quote progress: {idx + 1}/{len(stock_codes)}")
            except Exception as e:
                logger.error(f"Failed to get quote for {stock_code}: {e}")
                failed += 1

        if quotes:
            self._upsert_quotes(quotes)

        logger.info(
            f"Quote task finished. Trade date: {trade_date}, "
            f"Total: {len(stock_codes)}, Collected: {len(quotes)}, Failed: {failed}"
        )

    def _get_holding_stock_codes(self) -> List[str]:
        """从 position 表查询当前有持仓的股票代码"""
        rows = self.db.fetchall(
            "SELECT DISTINCT stock_code FROM position WHERE current_quantity > 0"
        )
        return [row[0] for row in rows if row[0]]

    def _parse_quote_row(self, stock_code: str, trade_date: str, row) -> tuple:
        """解析 DataFrame 单行数据为插入元组"""
        # akshare stock_zh_a_hist 列名：日期 开盘 收盘 最高 最低 成交量 成交额 ...
        return (
            stock_code,
            trade_date,
            float(row.get("开盘", 0) or 0),
            float(row.get("最高", 0) or 0),
            float(row.get("最低", 0) or 0),
            float(row.get("收盘", 0) or 0),
            int(row.get("成交量", 0) or 0),
            float(row.get("成交额", 0) or 0),
        )

    def _upsert_quotes(self, quotes: List[tuple]):
        """批量 upsert 行情数据到 daily_quote 表"""
        sql = """
            INSERT INTO daily_quote
            (stock_code, trade_date, open_price, high_price, low_price, close_price, volume, amount)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (stock_code, trade_date) DO UPDATE SET
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                volume = EXCLUDED.volume,
                amount = EXCLUDED.amount
        """
        count = self.db.upsert_many(sql, quotes)
        logger.info(f"Upserted {count} quote rows into daily_quote")
