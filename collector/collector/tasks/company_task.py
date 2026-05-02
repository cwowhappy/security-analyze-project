import logging
from typing import List, Dict, Any, Optional
from datetime import datetime

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor

logger = logging.getLogger(__name__)


class CompanyTask:
    """采集公司基本信息任务（支持公司-证券分离模型）"""

    def __init__(self, db: PostgresDB, source: AkshareSource, monitor: Monitor = None):
        self.db = db
        self.source = source
        self.monitor = monitor

    def run(self):
        """全量采集所有 A 股公司信息"""
        logger.info("Starting full company task...")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start("sync_company", "company")

        try:
            stock_list = self.source.get_stock_list()
            logger.info(f"Fetched {len(stock_list)} stocks from source")

            created, updated, failed = self._process_stocks(stock_list)
            rows = created + updated

            if self.monitor:
                status = "success" if failed == 0 else "failed"
                self.monitor.log_task_end(task_id, status, rows)
                self.monitor.upsert_data_status("company", rows, task_id)
        except Exception as e:
            logger.error(f"Company task failed: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", 0, str(e))
            raise

    def run_by_name(self, query: str):
        """按公司名称或股票代码采集指定公司信息"""
        logger.info(f"Starting company task by query: {query}")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start("sync_company_by_name", "company")

        try:
            matches = self.source.search_by_name(query)

            if not matches:
                logger.warning(f"No company found matching '{query}'")
                if self.monitor:
                    self.monitor.log_task_end(task_id, "success", 0)
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

            created, updated, failed = self._process_stocks(matches)
            rows = created + updated

            if self.monitor:
                status = "success" if failed == 0 else "failed"
                self.monitor.log_task_end(task_id, status, rows)
        except Exception as e:
            logger.error(f"Company task by name failed: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", 0, str(e))
            raise

    def _process_stocks(self, stock_list: List[Dict[str, Any]]) -> tuple[int, int, int]:
        """处理公司列表：采集详情并写入数据库，返回 (created, updated, failed)"""
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

                em_detail = None
                if not detail:
                    logger.warning(
                        f"Primary source failed for {stock_code}, trying fallback"
                    )
                    em_detail = self.source.get_company_info_em(stock_code)

                if not stock_name:
                    if detail:
                        stock_name = detail.get("A股简称", "") or detail.get(
                            "公司名称", ""
                        )
                    elif em_detail:
                        stock_name = em_detail.get("股票简称", "")

                company_entity = self._parse_company_entity(stock_name, detail, em_detail)
                securities = self._parse_securities(stock_code, stock_name, detail, em_detail)

                company_id = self._upsert_company(company_entity)
                if company_id:
                    result = self._upsert_securities(company_id, securities)
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
        return created, updated, failed

    def _parse_company_entity(
        self,
        stock_name: str,
        detail: Optional[Dict[str, Any]],
        em_detail: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """解析公司法人实体信息（公司级属性）"""
        if detail:
            company_name = detail.get("公司名称", "") or stock_name
            short_name = detail.get("A股简称", "") or stock_name
            industry = detail.get("所属行业", "")
            region = self._extract_region(detail.get("注册地址", ""))
            establish_date = self._parse_date(detail.get("成立日期", ""))
            registered_capital = self._parse_capital(detail.get("注册资金", ""))
        elif em_detail:
            company_name = em_detail.get("股票简称", "") or stock_name
            short_name = em_detail.get("股票简称", "") or stock_name
            industry = em_detail.get("行业", "")
            region = None
            establish_date = None
            registered_capital = None
        else:
            company_name = stock_name
            short_name = stock_name
            industry = ""
            region = None
            establish_date = None
            registered_capital = None

        return {
            "company_name": company_name or stock_name,
            "short_name": short_name or stock_name,
            "industry": industry or None,
            "region": region,
            "establish_date": establish_date,
            "registered_capital": registered_capital,
        }

    def _parse_securities(
        self,
        stock_code: str,
        stock_name: str,
        detail: Optional[Dict[str, Any]],
        em_detail: Optional[Dict[str, Any]] = None,
    ) -> List[Dict[str, Any]]:
        """解析证券信息列表（支持多市场证券）

        从 akshare stock_profile_cninfo 返回的多代码字段中提取：
        A股代码、A股简称、B股代码、B股简称、H股代码、H股简称
        """
        securities = []

        if detail:
            # 主数据源：stock_profile_cninfo
            # 解析 A 股（主传入代码）
            a_code = detail.get("A股代码", "") or stock_code
            a_name = detail.get("A股简称", "") or stock_name
            a_listing = self._parse_date(detail.get("上市日期", ""))
            a_market = self._infer_market(a_code)
            securities.append({
                "stock_code": a_code,
                "stock_name": a_name,
                "market": a_market,
                "security_type": "A股",
                "listing_date": a_listing,
                "listing_status": "listed",
            })

            # 解析 B 股
            b_code = detail.get("B股代码", "")
            b_name = detail.get("B股简称", "")
            if b_code and str(b_code).strip():
                securities.append({
                    "stock_code": str(b_code).strip(),
                    "stock_name": b_name or f"{stock_name}B",
                    "market": self._infer_market(str(b_code).strip()),
                    "security_type": "B股",
                    "listing_date": a_listing,  # B股通常与A股同日上市
                    "listing_status": "listed",
                })

            # 解析 H 股
            h_code = detail.get("H股代码", "")
            h_name = detail.get("H股简称", "")
            if h_code and str(h_code).strip():
                securities.append({
                    "stock_code": str(h_code).strip(),
                    "stock_name": h_name or f"{stock_name}H",
                    "market": "HK",
                    "security_type": "H股",
                    "listing_date": None,  # H股上市日期可能不同
                    "listing_status": "listed",
                })

        elif em_detail:
            # 备用数据源：stock_individual_info_em
            listing_date = self._parse_date(em_detail.get("上市时间", ""))
            market = self._infer_market(stock_code)
            securities.append({
                "stock_code": stock_code,
                "stock_name": stock_name or em_detail.get("股票简称", ""),
                "market": market,
                "security_type": "A股",
                "listing_date": listing_date,
                "listing_status": "listed",
            })
        else:
            # 无任何详情数据
            market = self._infer_market(stock_code)
            securities.append({
                "stock_code": stock_code,
                "stock_name": stock_name or stock_code,
                "market": market,
                "security_type": "A股",
                "listing_date": None,
                "listing_status": "listed",
            })

        return securities

    def _upsert_company(self, company: Dict[str, Any]) -> Optional[int]:
        """UPSERT 公司数据，返回 company_id"""
        # 先按 company_name 精确匹配查找
        existing = self.db.fetchone(
            "SELECT id FROM company WHERE company_name = %s",
            (company["company_name"],),
        )

        if existing:
            company_id = existing[0]
            # UPDATE
            sql = """
                UPDATE company
                SET short_name = %s, industry = %s, region = %s,
                    establish_date = %s, registered_capital = %s, updated_at = NOW()
                WHERE id = %s
            """
            self.db.execute(
                sql,
                (
                    company["short_name"],
                    company["industry"],
                    company["region"],
                    company["establish_date"],
                    company["registered_capital"],
                    company_id,
                ),
            )
            return company_id
        else:
            # INSERT
            sql = """
                INSERT INTO company
                (company_name, short_name, industry, region, establish_date,
                 registered_capital, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, NOW(), NOW())
                RETURNING id
            """
            result = self.db.execute_returning(
                sql,
                (
                    company["company_name"],
                    company["short_name"],
                    company["industry"],
                    company["region"],
                    company["establish_date"],
                    company["registered_capital"],
                ),
            )
            return result[0] if result else None

    def _upsert_securities(self, company_id: int, securities: List[Dict[str, Any]]) -> str:
        """UPSERT 证券数据，返回 insert / update / skip"""
        if not securities:
            return "skip"

        total_result = "skip"
        for sec in securities:
            existing = self.db.fetchone(
                "SELECT id FROM company_security WHERE stock_code = %s",
                (sec["stock_code"],),
            )

            if existing:
                # UPDATE
                sql = """
                    UPDATE company_security
                    SET company_id = %s, stock_name = %s, market = %s,
                        security_type = %s, listing_date = %s, listing_status = %s,
                        updated_at = NOW()
                    WHERE stock_code = %s
                """
                self.db.execute(
                    sql,
                    (
                        company_id,
                        sec["stock_name"],
                        sec["market"],
                        sec["security_type"],
                        sec["listing_date"],
                        sec["listing_status"],
                        sec["stock_code"],
                    ),
                )
                total_result = "update"
            else:
                # INSERT
                sql = """
                    INSERT INTO company_security
                    (company_id, stock_code, stock_name, market, security_type,
                     listing_date, listing_status, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
                """
                self.db.execute(
                    sql,
                    (
                        company_id,
                        sec["stock_code"],
                        sec["stock_name"],
                        sec["market"],
                        sec["security_type"],
                        sec["listing_date"],
                        sec["listing_status"],
                    ),
                )
                total_result = "insert"

        return total_result

    @staticmethod
    def _extract_region(address: str) -> Optional[str]:
        """从注册地址提取省份/城市"""
        if not address:
            return None
        import re

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
        if not stock_code:
            return None
        code = str(stock_code).strip()
        if len(code) != 6:
            return "HK" if len(code) == 5 else None
        first = code[0]
        if first in ("6", "9"):
            return "SH"
        elif first in ("0", "2", "3"):
            return "SZ"
        elif first in ("4", "8"):
            return "BJ"
        return None
