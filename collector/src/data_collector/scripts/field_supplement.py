"""字段补充采集脚本。

使用 Tushare 接口补充 tb_stock_basic 和 tb_company_basic 中缺失的字段，
如 area、ts_code、管理层、实控人等。

> 注意：Tushare 部分接口需要付费权限，缺失字段 gracefully skip。
"""

import random
import time
from datetime import date

import structlog
import tushare as ts

from data_collector.adapters.db_company_repository import DbCompanyRepository
from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.config import Settings
from data_collector.core.domain.company import Company
from data_collector.core.domain.stock import Stock
from data_collector.infrastructure.db import init_pool

logger = structlog.get_logger(__name__)


def _init_tushare(settings: Settings) -> object | None:
    """初始化 Tushare Pro API。"""
    token = settings.tushare_token
    if not token:
        logger.warning("TUSHARE_TOKEN 未配置，跳过字段补充")
        return None
    try:
        ts.set_token(token)
        return ts.pro_api()
    except Exception as e:
        logger.warning("Tushare 初始化失败", error=str(e))
        return None


def _parse_date(value: str | None) -> date | None:
    """解析日期字符串，支持 YYYY-MM-DD 和 YYYYMMDD 格式。"""
    if not value:
        return None
    val = str(value).strip()
    try:
        if len(val) == 8 and val.isdigit():
            # Tushare 返回格式: YYYYMMDD
            return date(int(val[:4]), int(val[4:6]), int(val[6:]))
        return date.fromisoformat(val)
    except (ValueError, TypeError):
        return None


def _parse_int(value: object | None) -> int | None:
    """解析整数（处理 Tushare 返回的浮点数字符串）。"""
    if value is None:
        return None
    try:
        return int(float(str(value).strip()))
    except (ValueError, TypeError):
        return None


def _supplement_stocks(pro: object, stocks: list[Stock], settings: Settings) -> tuple[int, int]:
    """补充股票字段。

    使用 tushare.stock_basic 和 daily_basic 接口批量补充 industry、area、
    list_date、fullname、total_shares、float_shares 等字段。

    策略：
        1. 一次性拉取 stock_basic 全量（1 次 API）
        2. 一次性拉取 daily_basic 最新交易日全量（1 次 API）
        3. 内存匹配后批量更新数据库

    Returns:
        (成功数, 失败数)
    """
    stock_repo = DbStockRepository()

    # 断点续传：跳过所有目标字段均已补全的股票
    stocks_to_update = [
        s for s in stocks
        if not s.full_name
        or not s.list_date
        or not s.industry
        or not s.area
        or not s.total_shares
        or not s.float_shares
    ]

    if not stocks_to_update:
        logger.info("所有股票字段已补全，跳过")
        return 0, 0

    logger.info("开始批量补充股票字段", count=len(stocks_to_update))

    # 1. 拉取 stock_basic 全量
    try:
        df_basic = pro.stock_basic(
            exchange="", list_status="L",
            fields="ts_code,symbol,name,area,industry,fullname,market,exchange,list_date",
        )
        if df_basic is None or df_basic.empty:
            logger.warning("tushare stock_basic 返回空数据")
            return 0, len(stocks_to_update)
    except Exception as e:
        logger.error("tushare stock_basic 查询失败", error=str(e))
        return 0, len(stocks_to_update)

    # 构建 ts_code -> 字段 的映射
    basic_map = {}
    for _, row in df_basic.iterrows():
        ts_code = str(row.get("ts_code", "")).strip()
        if ts_code:
            basic_map[ts_code] = row

    # 2. 拉取 daily_basic 最新交易日全量
    try:
        # 获取最近一个交易日
        from datetime import datetime
        today = datetime.now().strftime("%Y%m%d")
        df_trade = pro.trade_cal(exchange="SSE", start_date=today, end_date=today)
        if df_trade is not None and not df_trade.empty:
            last_trade = df_trade[df_trade["is_open"] == 1]["cal_date"].max()
        else:
            last_trade = today

        df_cap = pro.daily_basic(trade_date=last_trade, fields="ts_code,total_share,float_share")
        if df_cap is None or df_cap.empty:
            logger.warning("tushare daily_basic 返回空数据", trade_date=last_trade)
            df_cap = None
    except Exception as e:
        logger.warning("tushare daily_basic 查询失败", error=str(e))
        df_cap = None

    cap_map = {}
    if df_cap is not None:
        for _, row in df_cap.iterrows():
            ts_code = str(row.get("ts_code", "")).strip()
            if ts_code:
                cap_map[ts_code] = row

    # 3. 批量更新
    success = 0
    failed = 0

    for stock in stocks_to_update:
        ts_code = stock.ts_code
        if not ts_code:
            failed += 1
            continue

        try:
            basic_row = basic_map.get(ts_code)
            cap_row = cap_map.get(ts_code)

            if basic_row is None:
                logger.debug("tushare 未找到股票基础数据", stock_code=stock.stock_code, ts_code=ts_code)
                failed += 1
                continue

            tushare_total = _parse_int(cap_row.get("total_share")) if cap_row is not None else None
            tushare_float = _parse_int(cap_row.get("float_share")) if cap_row is not None else None

            updated = Stock(
                stock_code=stock.stock_code,
                name=stock.name,
                id=stock.id,
                ts_code=ts_code,
                full_name=str(basic_row.get("fullname", "")).strip() or stock.full_name,
                market=str(basic_row.get("market", "")).strip() or stock.market,
                exchange=str(basic_row.get("exchange", "")).strip() or stock.exchange,
                list_date=_parse_date(basic_row.get("list_date")) or stock.list_date,
                industry=str(basic_row.get("industry", "")).strip() or stock.industry,
                area=str(basic_row.get("area", "")).strip() or stock.area,
                total_shares=tushare_total * 10000 if tushare_total is not None else stock.total_shares,
                float_shares=tushare_float * 10000 if tushare_float is not None else stock.float_shares,
                company_id=stock.company_id,
            )
            stock_repo.save(updated)
            success += 1
        except Exception as e:
            logger.warning("补充股票字段失败", stock_code=stock.stock_code, error=str(e))
            failed += 1

    logger.info("股票字段补充完成", success=success, failed=failed)
    return success, failed


def _supplement_companies(
    pro: object, stocks: list[Stock], settings: Settings
) -> tuple[int, int]:
    """补充公司字段。

    使用 tushare.stock_company 接口补充 employees、chairman、manager、secretary 等。
    通过 stock.ts_code 查询，再关联到对应的公司记录。

    Returns:
        (成功数, 失败数)
    """
    from data_collector.infrastructure.db import execute_update

    success = 0
    failed = 0

    for stock in stocks:
        if not stock.company_id or not stock.ts_code:
            continue

        time.sleep(random.uniform(
            settings.source_request_delay_min,
            settings.source_request_delay_max,
        ))

        try:
            df = pro.stock_company(
                ts_code=stock.ts_code,
                fields="ts_code,exchange,chairman,manager,secretary,reg_capital,setup_date,employees,main_business",
            )
            if df is None or df.empty:
                failed += 1
                continue

            row = df.iloc[0]
            chairman = str(row.get("chairman", "")).strip() or None
            manager = str(row.get("manager", "")).strip() or None
            secretary = str(row.get("secretary", "")).strip() or None
            employees = _parse_int(row.get("employees"))

            # 直接更新公司记录
            sql = """
            UPDATE tb_company_basic
            SET chairman = COALESCE(%s, chairman),
                manager = COALESCE(%s, manager),
                secretary = COALESCE(%s, secretary),
                employees = COALESCE(%s, employees),
                updated_at = NOW()
            WHERE id = %s
            """
            execute_update(sql, (chairman, manager, secretary, employees, stock.company_id))
            logger.debug(
                "公司字段补充成功",
                stock_code=stock.stock_code,
                company_id=stock.company_id,
                chairman=chairman,
                employees=employees,
            )
            success += 1
        except Exception as e:
            logger.warning("补充公司字段失败", stock_code=stock.stock_code, error=str(e))
            failed += 1

    return success, failed


def run_field_supplement(settings: Settings | None = None) -> dict:
    """执行字段补充采集。

    Returns:
        {"stock_total": int, "stock_success": int, "stock_failed": int,
         "company_total": int, "company_success": int, "company_failed": int}
    """
    settings = settings or Settings()
    init_pool(settings)
    pro = _init_tushare(settings)
    if pro is None:
        return {
            "stock_total": 0, "stock_success": 0, "stock_failed": 0,
            "company_total": 0, "company_success": 0, "company_failed": 0,
        }

    stock_repo = DbStockRepository()

    stocks = list(stock_repo.find_all())

    logger.info("开始字段补充采集", stocks=len(stocks))

    s_success, s_failed = _supplement_stocks(pro, stocks, settings)
    c_success, c_failed = _supplement_companies(pro, stocks, settings)

    logger.info(
        "字段补充采集完成",
        stock_success=s_success,
        stock_failed=s_failed,
        company_success=c_success,
        company_failed=c_failed,
    )

    return {
        "stock_total": len(stocks),
        "stock_success": s_success,
        "stock_failed": s_failed,
        "company_total": len([s for s in stocks if s.company_id]),
        "company_success": c_success,
        "company_failed": c_failed,
    }
