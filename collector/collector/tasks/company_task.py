import logging
from typing import List, Dict, Any, Optional
from datetime import datetime

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource

logger = logging.getLogger(__name__)


class CompanyTask:
    """采集公司基本信息任务"""

    def __init__(self, db: PostgresDB, source: AkshareSource):
        self.db = db
        self.source = source

    def run(self):
        """全量采集所有 A 股公司信息"""
        logger.info("Starting full company task...")

        # 1. 获取 A 股列表
        stock_list = self.source.get_stock_list()
        logger.info(f"Fetched {len(stock_list)} stocks from source")

        self._process_stocks(stock_list)

    def run_by_name(self, query: str):
        """按公司名称或股票代码采集指定公司信息

        Args:
            query: 用户输入的公司名称或股票代码
        """
        logger.info(f"Starting company task by query: {query}")

        # 1. 搜索匹配的公司
        matches = self.source.search_by_name(query)

        if not matches:
            logger.warning(f"No company found matching '{query}'")
            return

        if len(matches) == 1:
            match = matches[0]
            logger.info(f"Found exact match: {match.get('name', '')} ({match['code']})")
        else:
            logger.info(f"Found {len(matches)} matches for '{query}':")
            for i, m in enumerate(matches[:10], 1):
                logger.info(f"  {i}. {m.get('name', '')} ({m['code']})")
            if len(matches) > 10:
                logger.info(f"  ... and {len(matches) - 10} more")
            logger.info("Collecting all matched companies...")

        self._process_stocks(matches)

    def _process_stocks(self, stock_list: List[Dict[str, Any]]):
        """处理公司列表：采集详情并写入数据库"""
        total = len(stock_list)
        created = 0
        updated = 0
        failed = 0

        for item in stock_list:
            stock_code = item.get("code", "")
            stock_name = item.get("name", "")

            if not stock_code:
                logger.warning("Skip empty stock code")
                failed += 1
                continue

            try:
                detail = self.source.get_company_detail(stock_code)

                # 如果主数据源失败，尝试备用数据源
                em_detail = None
                if not detail:
                    logger.warning(
                        f"Primary source failed for {stock_code}, trying fallback"
                    )
                    em_detail = self.source.get_company_info_em(stock_code)

                # 如果 stock_name 为空（如直接传入代码场景），尝试从详情补全
                if not stock_name:
                    if detail:
                        stock_name = detail.get("A股简称", "") or detail.get(
                            "公司名称", ""
                        )
                    elif em_detail:
                        stock_name = em_detail.get("股票简称", "")

                company = self._parse_company(stock_code, stock_name, detail, em_detail)
                result = self._upsert_company(company)

                if result == "insert":
                    created += 1
                elif result == "update":
                    updated += 1

            except Exception as e:
                logger.error(f"Failed to process {stock_code} {stock_name}: {e}")
                failed += 1

        logger.info(
            f"Company task finished. Total: {total}, Created: {created}, Updated: {updated}, Failed: {failed}"
        )

    def _parse_company(
        self,
        stock_code: str,
        stock_name: str,
        detail: Optional[Dict[str, Any]],
        em_detail: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """解析 akshare 数据为统一格式

        Args:
            detail: stock_profile_cninfo 返回的数据（主数据源）
            em_detail: stock_individual_info_em 返回的数据（备用数据源）
        """
        if detail:
            # 主数据源：stock_profile_cninfo
            # 字段：公司名称, 英文名称, 曾用简称, A股代码, A股简称, B股代码, B股简称,
            #       H股代码, H股简称, 入选指数, 所属市场, 所属行业, 法人代表, 注册资金,
            #       成立日期, 上市日期, 官方网站, 电子邮箱, 联系电话, 传真,
            #       注册地址, 办公地址, 邮政编码, 主营业务, 经营范围, 机构简介
            industry = detail.get("所属行业", "")
            region = self._extract_region(detail.get("注册地址", ""))
            establish_date = self._parse_date(detail.get("成立日期", ""))
            registered_capital = self._parse_capital(detail.get("注册资金", ""))
            listing_date = self._parse_date(detail.get("上市日期", ""))
            stock_name = stock_name or detail.get("A股简称", "") or detail.get("公司名称", "")
        elif em_detail:
            # 备用数据源：stock_individual_info_em
            # 字段：股票代码, 股票简称, 总股本, 流通股, 行业, 总市值, 流通市值, 上市时间, 最新
            industry = em_detail.get("行业", "")
            region = None
            establish_date = None
            registered_capital = None
            listing_date = self._parse_date(em_detail.get("上市时间", ""))
            stock_name = stock_name or em_detail.get("股票简称", "")
        else:
            industry = ""
            region = None
            establish_date = None
            registered_capital = None
            listing_date = None

        market = self._infer_market(stock_code)

        return {
            "stock_code": stock_code,
            "stock_name": stock_name or stock_code,
            "industry": industry or None,
            "region": region,
            "establish_date": establish_date,
            "registered_capital": registered_capital,
            "listing_date": listing_date,
            "market": market,
        }

    def _upsert_company(self, company: Dict[str, Any]) -> str:
        """UPSERT 公司数据，返回 insert / update"""
        # 先查询是否存在
        existing = self.db.fetchall(
            "SELECT id FROM company WHERE stock_code = %s",
            (company["stock_code"],),
        )

        if existing:
            # UPDATE
            sql = """
                UPDATE company
                SET stock_name = %s, industry = %s, region = %s,
                    establish_date = %s, registered_capital = %s,
                    listing_date = %s, market = %s, updated_at = NOW()
                WHERE stock_code = %s
            """
            self.db.execute(
                sql,
                (
                    company["stock_name"],
                    company["industry"],
                    company["region"],
                    company["establish_date"],
                    company["registered_capital"],
                    company["listing_date"],
                    company["market"],
                    company["stock_code"],
                ),
            )
            return "update"
        else:
            # INSERT
            sql = """
                INSERT INTO company
                (stock_code, stock_name, industry, region, establish_date,
                 registered_capital, listing_date, market, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            """
            self.db.execute(
                sql,
                (
                    company["stock_code"],
                    company["stock_name"],
                    company["industry"],
                    company["region"],
                    company["establish_date"],
                    company["registered_capital"],
                    company["listing_date"],
                    company["market"],
                ),
            )
            return "insert"

    @staticmethod
    def _extract_region(address: str) -> Optional[str]:
        """从注册地址提取省份/城市"""
        if not address:
            return None
        import re

        # 匹配省级：省、自治区、直辖市
        match = re.search(r"^(.*?省|.*?自治区|北京|天津|上海|重庆)", address)
        if match:
            return match.group(1)
        return None

    @staticmethod
    def _parse_date(date_str) -> Optional[str]:
        """解析日期字符串为 YYYY-MM-DD"""
        if not date_str:
            return None
        try:
            dt = datetime.strptime(str(date_str), "%Y-%m-%d")
            return dt.strftime("%Y-%m-%d")
        except (ValueError, TypeError):
            return None

    @staticmethod
    def _parse_capital(capital) -> Optional[float]:
        """解析注册资本/注册资金为数字（万元）"""
        if capital is None:
            return None
        import re

        # 提取数字部分
        match = re.search(r"([\d,.]+)", str(capital))
        if match:
            try:
                return float(match.group(1).replace(",", ""))
            except (ValueError, TypeError):
                return None
        return None

    @staticmethod
    def _infer_market(stock_code: str) -> Optional[str]:
        """根据股票代码推断市场板块"""
        if not stock_code or len(stock_code) != 6:
            return None
        first = stock_code[0]
        if first in ("6",):
            return "SH"
        elif first in ("0", "3"):
            return "SZ"
        elif first in ("4", "8", "9"):
            return "BJ"
        return None
