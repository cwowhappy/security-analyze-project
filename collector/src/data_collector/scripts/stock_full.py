"""股票全量采集脚本。

组合使用 AKShare 的 stock_info_a_code_name（全量覆盖）与交易所分接口
（sh_name_code / sz_name_code / bj_name_code）获取更完整的字段，
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
from data_collector.infrastructure.db import init_pool

logger = structlog.get_logger(__name__)


# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

def _parse_date(value: str | None) -> date | None:
    """解析日期字符串（YYYY-MM-DD）。"""
    if not value:
        return None
    try:
        return date.fromisoformat(str(value).strip())
    except ValueError:
        return None


def _parse_shares(value: str | None) -> int | None:
    """解析股本字符串（去除逗号）。"""
    if not value:
        return None
    try:
        v = str(value).replace(",", "").strip()
        return int(float(v))
    except (ValueError, TypeError):
        return None


def _clean_name(name: str) -> str:
    """清洗股票名称中的多余空格（如 '万  科Ａ' → '万科Ａ'）。"""
    return " ".join(str(name).split())


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
    if code.startswith(("4", "8", "82", "83", "87", "88", "89", "92")):
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
    if code.startswith(("4", "8", "82", "83", "87", "88", "89", "92")):
        return "BSE"
    return None


def _infer_market(stock_code: str) -> str | None:
    """根据代码前缀推断市场标识。"""
    code = str(stock_code).strip()
    if code.startswith("6"):
        return "SH"
    if code.startswith(("0", "3")):
        return "SZ"
    if code.startswith(("4", "8", "82", "83", "87", "88", "89", "92")):
        return "BJ"
    return None


# ---------------------------------------------------------------------------
# 交易所分接口采集
# ---------------------------------------------------------------------------

def _fetch_sh_stocks() -> dict[str, dict]:
    """获取上交所股票详情。

    Returns:
        {stock_code: {name, full_name, list_date, market, exchange}}
    """
    logger.info("获取上交所股票列表")
    result: dict[str, dict] = {}
    try:
        df = ak.stock_info_sh_name_code()
        for _, row in df.iterrows():
            code = str(row.get("证券代码", "")).strip()
            if not code:
                continue
            result[code] = {
                "name": _clean_name(row.get("证券简称", "")),
                "full_name": str(row.get("证券全称", "")).strip()
                or str(row.get("公司全称", "")).strip()
                or None,
                "list_date": _parse_date(row.get("上市日期")),
                "market": "SH",
                "exchange": "SSE",
            }
    except Exception as e:
        logger.warning("获取上交所股票列表失败", error=str(e))
    logger.info("上交所股票获取完成", count=len(result))
    return result


def _fetch_sz_stocks() -> dict[str, dict]:
    """获取深交所股票详情。

    Returns:
        {stock_code: {name, list_date, industry, total_shares, float_shares, market, exchange}}
    """
    logger.info("获取深交所股票列表")
    result: dict[str, dict] = {}
    try:
        df = ak.stock_info_sz_name_code()
        for _, row in df.iterrows():
            code = str(row.get("A股代码", "")).strip()
            if not code:
                continue
            result[code] = {
                "name": _clean_name(row.get("A股简称", "")),
                "full_name": None,  # 深交所接口不返回全称
                "list_date": _parse_date(row.get("A股上市日期")),
                "industry": str(row.get("所属行业", "")).strip() or None,
                "total_shares": _parse_shares(row.get("A股总股本")),
                "float_shares": _parse_shares(row.get("A股流通股本")),
                "market": "SZ",
                "exchange": "SZSE",
            }
    except Exception as e:
        logger.warning("获取深交所股票列表失败", error=str(e))
    logger.info("深交所股票获取完成", count=len(result))
    return result


def _fetch_bj_stocks() -> dict[str, dict]:
    """获取北交所股票详情。

    Returns:
        {stock_code: {name, list_date, industry, area, total_shares, float_shares, market, exchange}}
    """
    logger.info("获取北交所股票列表")
    result: dict[str, dict] = {}
    try:
        df = ak.stock_info_bj_name_code()
        for _, row in df.iterrows():
            code = str(row.get("证券代码", "")).strip()
            if not code:
                continue
            result[code] = {
                "name": _clean_name(row.get("证券简称", "")),
                "full_name": None,
                "list_date": _parse_date(row.get("上市日期")),
                "industry": str(row.get("所属行业", "")).strip() or None,
                "area": str(row.get("地区", "")).strip() or None,
                "total_shares": _parse_shares(row.get("总股本")),
                "float_shares": _parse_shares(row.get("流通股本")),
                "market": "BJ",
                "exchange": "BSE",
            }
    except Exception as e:
        logger.warning("获取北交所股票列表失败", error=str(e))
    logger.info("北交所股票获取完成", count=len(result))
    return result


# ---------------------------------------------------------------------------
# 主入口
# ---------------------------------------------------------------------------

def run_stock_full(settings: Settings | None = None) -> dict:
    """执行股票全量采集（组合接口版）。

    1. 通过 sh/sz/bj 分接口获取详细字段
    2. 通过 stock_info_a_code_name 获取全量列表（兜底科创板等）
    3. 合并后入库

    Returns:
        {"total": int, "success": int, "failed": int}
    """
    settings = settings or Settings()
    init_pool(settings)
    repo = DbStockRepository()

    logger.info("开始股票全量采集（组合接口）")
    time.sleep(random.uniform(
        settings.source_request_delay_min,
        settings.source_request_delay_max,
    ))

    # 1. 获取三个交易所的详细数据
    sh_data = _fetch_sh_stocks()
    sz_data = _fetch_sz_stocks()
    bj_data = _fetch_bj_stocks()

    # 2. 获取全量列表（确保覆盖科创板等可能不在分接口中的股票）
    try:
        df_all = ak.stock_info_a_code_name()
    except Exception as e:
        logger.error("akshare 获取股票列表失败", error=str(e))
        raise

    if df_all is None or df_all.empty:
        logger.warning("akshare 返回空数据")
        return {"total": 0, "success": 0, "failed": 0}

    stocks: list[Stock] = []
    for _, row in df_all.iterrows():
        try:
            stock_code = str(row.get("code", "")).strip()
            name = _clean_name(row.get("name", ""))
            if not stock_code or not name:
                continue

            # 优先从分接口获取详细信息
            detail = sh_data.get(stock_code) or sz_data.get(stock_code) or bj_data.get(stock_code) or {}

            # market / exchange：优先用分接口数据，否则推断
            market = detail.get("market") or _infer_market(stock_code)
            exchange = detail.get("exchange") or _to_exchange(stock_code, market)

            stock = Stock(
                stock_code=stock_code,
                name=name,
                ts_code=_to_ts_code(stock_code, market),
                full_name=detail.get("full_name") or name,
                market=market,
                exchange=exchange,
                list_date=detail.get("list_date"),
                industry=detail.get("industry"),
                area=detail.get("area"),
                total_shares=detail.get("total_shares"),
                float_shares=detail.get("float_shares"),
            )
            stocks.append(stock)
        except (ValueError, KeyError) as e:
            logger.debug("解析股票行失败", row=dict(row), error=str(e))
            continue

    logger.info(
        "股票列表解析完成",
        count=len(stocks),
        sh=len(sh_data),
        sz=len(sz_data),
        bj=len(bj_data),
    )

    success, failed = repo.save_all(stocks)
    return {"total": len(stocks), "success": success, "failed": failed}
