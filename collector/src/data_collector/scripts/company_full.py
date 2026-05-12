"""公司信息全量采集脚本。

遍历 tb_stock_basic 中所有股票，逐条调用 akshare.stock_profile_cninfo
获取公司详情，写入 tb_company_basic，并更新 tb_stock_basic.company_id。
"""

import random
import time
from datetime import date, datetime
from decimal import Decimal

import akshare as ak
import structlog
import ulid

from data_collector.adapters.db_company_repository import DbCompanyRepository
from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.config import Settings
from data_collector.core.domain.company import Company

logger = structlog.get_logger(__name__)


def _parse_date(value: str | None) -> date | None:
    """解析日期字符串。"""
    if not value:
        return None
    try:
        return datetime.strptime(str(value).strip(), "%Y-%m-%d").date()
    except ValueError:
        return None


def _parse_capital(value: str | None) -> Decimal | None:
    """解析注册资本，去除单位保留数字。"""
    if not value:
        return None
    try:
        val = str(value).replace(",", "").replace("万", "").replace("亿", "")
        return Decimal(str(float(val)))
    except ValueError:
        return None


def fetch_company_for_stock(stock_code: str) -> Company | None:
    """为单只股票获取公司详情。

    Returns:
        Company 实例，未找到或失败时返回 None。
    """
    try:
        df = ak.stock_profile_cninfo(symbol=stock_code)
    except Exception as e:
        logger.warning("akshare 获取公司详情失败", stock_code=stock_code, error=str(e))
        return None

    if df is None or df.empty:
        logger.debug("akshare 未找到公司详情", stock_code=stock_code)
        return None

    row = df.iloc[0]
    try:
        company = Company(
            id=str(ulid.ULID()),
            unified_social_credit_code=str(row.get("统一社会信用代码", "")).strip() or None,
            name=str(row.get("公司名称", stock_code)).strip(),
            short_name=str(row.get("证券简称", "")).strip() or None,
            english_name=str(row.get("英文名称", "")).strip() or None,
            former_name=str(row.get("曾用名", "")).strip() or None,
            legal_representative=str(row.get("法人代表", "")).strip() or None,
            chairman=str(row.get("董事长", "")).strip() or None,
            manager=str(row.get("总经理", "")).strip() or None,
            secretary=str(row.get("董秘", "")).strip() or None,
            reg_capital=_parse_capital(row.get("注册资本")),
            setup_date=_parse_date(row.get("成立日期")),
            province=str(row.get("省份", "")).strip() or None,
            city=str(row.get("城市", "")).strip() or None,
            reg_address=str(row.get("注册地址", "")).strip() or None,
            office_address=str(row.get("办公地址", "")).strip() or None,
            website=str(row.get("公司网站", "")).strip() or None,
            industry=str(row.get("行业分类", "")).strip() or None,
            main_business=str(row.get("主营业务", "")).strip() or None,
            business_scope=str(row.get("经营范围", "")).strip() or None,
            introduction=str(row.get("公司简介", "")).strip() or None,
        )
        logger.info("公司详情获取成功", stock_code=stock_code, name=company.name)
        return company
    except Exception as e:
        logger.warning("解析公司详情失败", stock_code=stock_code, error=str(e))
        return None


def run_company_full(settings: Settings | None = None) -> dict:
    """执行公司信息全量采集。

    遍历股票列表，逐条采集公司信息并建立关联。

    Returns:
        {"total": int, "success": int, "failed": int}
    """
    settings = settings or Settings()
    stock_repo = DbStockRepository()
    company_repo = DbCompanyRepository()

    stocks = stock_repo.find_all()
    # 断点续传：跳过已建立公司关联的股票
    stocks = [s for s in stocks if not s.company_id]
    total = len(stocks)
    success = 0
    failed = 0

    logger.info("开始公司信息全量采集", total=total, skipped=len(stock_repo.find_all()) - total)

    for stock in stocks:
        time.sleep(random.uniform(
            settings.source_request_delay_min,
            settings.source_request_delay_max,
        ))

        company = fetch_company_for_stock(stock.stock_code)
        if company:
            try:
                company_repo.save(company)
                # 建立股票-公司关联
                stock_repo.update_company_id(
                    stock.stock_code,
                    company.id,
                )
                success += 1
            except Exception as e:
                logger.warning(
                    "保存公司信息失败",
                    stock_code=stock.stock_code,
                    error=str(e),
                )
                failed += 1
        else:
            failed += 1

    logger.info("公司信息全量采集完成", total=total, success=success, failed=failed)
    return {"total": total, "success": success, "failed": failed}
