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


def _parse_province_city(address: str | None) -> tuple[str | None, str | None]:
    """从注册地址解析省份和城市。

    中国地址通常以"省/市/自治区"开头， followed by 城市名。
    """
    if not address:
        return None, None
    addr = str(address).strip()
    # 直辖市
    for city in ("北京市", "上海市", "天津市", "重庆市"):
        if addr.startswith(city):
            return city, city
    # 省份（含自治区）
    province_keywords = [
        "黑龙江省", "内蒙古自治区", "新疆维吾尔自治区", "广西壮族自治区", "宁夏回族自治区", "西藏自治区",
        "河北省", "山西省", "辽宁省", "吉林省", "江苏省", "浙江省", "安徽省", "福建省", "江西省",
        "山东省", "河南省", "湖北省", "湖南省", "广东省", "海南省", "四川省", "贵州省", "云南省",
        "陕西省", "甘肃省", "青海省", "台湾省"
    ]
    for pk in province_keywords:
        if addr.startswith(pk):
            # 尝试提取城市：省份后的下一个行政区划单位
            rest = addr[len(pk):].lstrip("省自治区维吾尔回族")
            # 通常城市名在 2-4 个字符之间，以"市"结尾
            if len(rest) >= 2:
                # 简单取前几个字符，若包含"市"则截取到"市"
                if "市" in rest:
                    city_end = rest.index("市") + 1
                    city = rest[:city_end]
                    return pk, city
                else:
                    return pk, rest[:4] if len(rest) >= 4 else rest
    #  fallback：尝试简单模式
    if len(addr) >= 2:
        return addr[:2] + "省" if not addr[:2].endswith("省") else addr[:2], None
    return None, None


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
        reg_address = str(row.get("注册地址", "")).strip() or None
        province, city = _parse_province_city(reg_address)

        company = Company(
            id=str(ulid.ULID()),
            unified_social_credit_code=None,  # stock_profile_cninfo 不返回统一社会信用代码，需通过其他数据源补充
            name=str(row.get("公司名称", stock_code)).strip(),
            short_name=str(row.get("A股简称", "")).strip() or None,
            english_name=str(row.get("英文名称", "")).strip() or None,
            former_name=str(row.get("曾用简称", "")).strip() or None,
            legal_representative=str(row.get("法人代表", "")).strip() or None,
            chairman=None,  # 当前数据源不返回董事长
            manager=None,   # 当前数据源不返回总经理
            secretary=None, # 当前数据源不返回董秘
            reg_capital=_parse_capital(row.get("注册资金")),
            setup_date=_parse_date(row.get("成立日期")),
            province=province,
            city=city,
            reg_address=reg_address,
            office_address=str(row.get("办公地址", "")).strip() or None,
            website=str(row.get("官方网站", "")).strip() or None,
            industry=str(row.get("所属行业", "")).strip() or None,
            main_business=str(row.get("主营业务", "")).strip() or None,
            business_scope=str(row.get("经营范围", "")).strip() or None,
            introduction=str(row.get("机构简介", "")).strip() or None,
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
