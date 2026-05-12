"""股票全量采集脚本。

使用 AKShare 的 stock_info_a_code_name 接口获取 A 股全量股票列表，
写入 tb_stock_basic。
"""

import random
import time
from datetime import date

import akshare as ak
import structlog

from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.config import Settings
from data_collector.core.domain.stock import Stock

logger = structlog.get_logger(__name__)


def _to_ts_code(stock_code: str, market: str | None) -> str | None:
    """将股票代码转换为 ts_code 格式（如 000001.SZ）。"""
    code = str(stock_code).strip()
    if not code:
        return None
    if market:
        m = str(market).upper()
        if m in ("SH", "SSE"):
            return f"{code}.SH"
        if m in ("SZ", "SZSE"):
            return f"{code}.SZ"
        if m in ("BJ", "BSE"):
            return f"{code}.BJ"
    # 根据代码前缀推断交易所
    if code.startswith("6"):
        return f"{code}.SH"
    if code.startswith(("0", "3")):
        return f"{code}.SZ"
    if code.startswith(("4", "8")):
        return f"{code}.BJ"
    return None


def _to_exchange(stock_code: str, market: str | None) -> str | None:
    """根据 market 或代码前缀推断交易所代码（SSE / SZSE / BSE）。"""
    if market:
        m = str(market).upper()
        if m in ("SH", "SSE"):
            return "SSE"
        if m in ("SZ", "SZSE"):
            return "SZSE"
        if m in ("BJ", "BSE"):
            return "BSE"
    code = str(stock_code).strip()
    if code.startswith("6"):
        return "SSE"
    if code.startswith(("0", "3")):
        return "SZSE"
    if code.startswith(("4", "8")):
        return "BSE"
    return None


def _parse_date(value: str | None) -> date | None:
    """解析日期字符串。"""
    if not value:
        return None
    try:
        return date.fromisoformat(str(value).strip())
    except ValueError:
        return None


def run_stock_full(settings: Settings | None = None) -> dict:
    """执行股票全量采集。

    Returns:
        {"total": int, "success": int, "failed": int}
    """
    settings = settings or Settings()
    repo = DbStockRepository()

    logger.info("开始从 akshare 获取全量股票列表")
    time.sleep(random.uniform(
        settings.source_request_delay_min,
        settings.source_request_delay_max,
    ))

    try:
        df = ak.stock_info_a_code_name()
    except Exception as e:
        logger.error("akshare 获取股票列表失败", error=str(e))
        raise

    if df is None or df.empty:
        logger.warning("akshare 返回空数据")
        return {"total": 0, "success": 0, "failed": 0}

    stocks: list[Stock] = []
    for _, row in df.iterrows():
        try:
            stock_code = str(row.get("code", "")).strip()
            name = str(row.get("name", "")).strip()
            market = str(row.get("market", "")).strip() or None
            if not stock_code or not name:
                continue

            # AKShare 部分股票不返回 market，根据代码前缀推断
            if not market:
                if stock_code.startswith("6"):
                    market = "SH"
                elif stock_code.startswith(("0", "3")):
                    market = "SZ"
                elif stock_code.startswith(("4", "8")):
                    market = "BJ"

            stock = Stock(
                stock_code=stock_code,
                name=name,
                ts_code=_to_ts_code(stock_code, market),
                full_name=name,
                market=market,
                exchange=_to_exchange(stock_code, market),
                list_date=_parse_date(row.get("list_date")),
            )
            stocks.append(stock)
        except (ValueError, KeyError) as e:
            logger.debug("解析股票行失败", row=dict(row), error=str(e))
            continue

    logger.info("akshare 股票列表获取完成", count=len(stocks))

    success, failed = repo.save_all(stocks)
    return {"total": len(stocks), "success": success, "failed": failed}
