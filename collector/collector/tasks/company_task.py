import logging
from typing import List, Dict, Any, Optional

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor
from collector.utils import parse_date, parse_capital, extract_region, infer_market
from collector.models import CompanyEntity, SecurityEntity, CompanyIndustryMapping

logger = logging.getLogger(__name__)

# 申万行业编码映射（从 stock_industry_clf_hist_sw 的 6 位编码映射到 801xxx）
# 一级：前 2 位 -> 8010xx
L1_TO_801_MAPPING = {
    "11": "801030", "22": "801030", "23": "801040", "24": "801050",
    "27": "801750", "28": "801880", "33": "801110", "34": "801760",
    "35": "801130", "36": "801140", "37": "801150", "41": "801160",
    "42": "801170", "43": "801180", "45": "801200", "46": "801210",
    "48": "801780", "49": "801790", "51": "801880", "61": "801710",
    "62": "801720", "63": "801730", "64": "801890", "65": "801740",
    "71": "801750", "72": "801760", "73": "801230", "74": "801950",
    "75": "801960", "76": "801970", "77": "801980",
}

# 二级：前 4 位 -> 801xxx
L2_TO_801_MAPPING = {
    "1101": "801038", "1102": "801015", "1103": "801011", "1104": "801014",
    "1105": "801012", "1107": "801181", "1108": "801018", "1109": "801019",
    "2202": "801033", "2203": "801034", "2204": "801032", "2205": "801036",
    "2206": "801037", "2208": "801033", "2209": "801039", "2303": "801043",
    "2304": "801044", "2305": "801045", "2402": "801051", "2403": "801055",
    "2404": "801181", "2405": "801054", "2406": "801038", "2701": "801101",
    "2702": "801083", "2703": "801084", "2704": "801082", "2705": "801085",
    "2706": "801086", "2802": "801093", "2803": "801082", "2804": "801881",
    "2805": "801095", "2806": "801096", "3301": "801111", "3302": "801112",
    "3303": "801113", "3304": "801114", "3305": "801115", "3306": "801116",
    "3307": "801117", "3404": "801769", "3405": "801125", "3406": "801126",
    "3407": "801127", "3408": "801128", "3409": "801129", "3501": "801131",
    "3502": "801132", "3503": "801881", "3601": "801143", "3602": "801141",
    "3603": "801142", "3605": "801765", "3701": "801151", "3702": "801151",
    "3703": "801152", "3704": "801154", "3705": "801153", "3706": "801156",
    "4101": "801161", "4103": "801163", "4208": "801178", "4209": "801179",
    "4210": "801991", "4211": "801992", "4301": "801181", "4303": "801183",
    "4502": "801202", "4503": "801203", "4504": "801204", "4506": "801206",
    "4507": "801181", "4606": "801216", "4608": "801982", "4609": "801219",
    "4610": "801993", "4611": "801994", "4802": "801782", "4803": "801783",
    "4804": "801784", "4805": "801785", "4901": "801193", "4902": "801194",
    "4903": "801191", "5101": "801092", "6101": "801711", "6102": "801712",
    "6103": "801713", "6201": "801721", "6202": "801722", "6203": "801723",
    "6204": "801102", "6206": "801726", "6301": "801731", "6303": "801733",
    "6305": "801963", "6306": "801736", "6307": "801737", "6308": "801738",
    "6401": "801072", "6402": "801074", "6405": "801076", "6406": "801077",
    "6407": "801078", "6501": "801741", "6502": "801742", "6503": "801743",
    "6504": "801744", "6505": "801745", "7101": "801101", "7103": "801103",
    "7104": "801104", "7204": "801764", "7205": "801765", "7206": "801766",
    "7207": "801767", "7209": "801769", "7210": "801995", "7301": "801231",
    "7302": "801102", "7401": "801951", "7402": "801952", "7501": "801961",
    "7502": "801962", "7503": "801963", "7601": "801971", "7602": "801972",
    "7701": "801981", "7702": "801982", "7703": "801983",
}


class CompanyTask:
    """采集公司基本信息任务（支持公司-证券分离模型）"""

    def __init__(self, db: PostgresDB, source: AkshareSource, monitor: Monitor = None):
        self.db = db
        self.source = source
        self.monitor = monitor
        self._sw_mapping: Dict[str, List[tuple]] = {}  # stock_code -> [(l1_code, l2_code)]
        self._em_name_to_code: Dict[str, str] = {}      # EM industry name -> code
        self._mappings_loaded = False

    def _preload_mappings(self):
        """预加载行业分类映射（申万 + 东财）"""
        if self._mappings_loaded:
            return
        self._preload_sw_mapping()
        self._preload_em_mapping()
        self._mappings_loaded = True

    def _preload_sw_mapping(self):
        """从 stock_industry_clf_hist_sw 预加载申万行业映射"""
        logger.info("预加载申万行业映射...")
        try:
            import akshare as ak
            hist = ak.stock_industry_clf_hist_sw()
            hist["start_date"] = hist["start_date"].astype(str)
            # 取每个 symbol 最新的记录
            latest = hist.loc[hist.groupby("symbol")["start_date"].idxmax()]
            for _, row in latest.iterrows():
                code = str(row.get("symbol", "")).strip()
                industry_code = str(row.get("industry_code", "")).strip()
                if not code or not industry_code:
                    continue
                l1_raw = industry_code[:2]
                l2_raw = industry_code[:4]
                l1_code = L1_TO_801_MAPPING.get(l1_raw)
                l2_code = L2_TO_801_MAPPING.get(l2_raw)
                if l1_code and l2_code:
                    if code not in self._sw_mapping:
                        self._sw_mapping[code] = []
                    self._sw_mapping[code].append((l1_code, l2_code))
            logger.info(f"申万映射预加载完成，共 {len(self._sw_mapping)} 只股票")
        except Exception as e:
            logger.warning(f"预加载申万映射失败: {e}")

    def _preload_em_mapping(self):
        """从数据库预加载东财行业名称 -> code 映射"""
        logger.info("预加载东财行业名称映射...")
        try:
            rows = self.db.fetchall(
                "SELECT code, name FROM industry_category WHERE standard_code = 'EM' AND level = 2"
            )
            for code, name in rows:
                self._em_name_to_code[name] = code
            logger.info(f"东财名称映射预加载完成，共 {len(self._em_name_to_code)} 个板块")
        except Exception as e:
            logger.warning(f"预加载东财名称映射失败: {e}")

    def run(self):
        """全量采集所有 A 股公司信息"""
        logger.info("Starting full company task...")
        self._preload_mappings()
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
        self._preload_mappings()
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

        for idx, item in enumerate(stock_list):
            stock_code = item.get("code", "")
            stock_name = item.get("name", "")

            # 每 500 条打印进度
            if idx > 0 and idx % 500 == 0:
                logger.info(
                    f"Progress: {idx}/{total} ({idx * 100 // total}%), "
                    f"Created: {created}, Updated: {updated}, Failed: {failed}"
                )

            if not stock_code:
                logger.warning("Skip empty stock code")
                failed += 1
                continue

            try:
                detail = self.source.get_company_detail(stock_code)

                # 总是获取 EM 基本信息（用于行业映射）
                em_detail = self.source.get_company_info_em(stock_code)

                if not detail and em_detail:
                    logger.warning(
                        f"Primary source failed for {stock_code}, using EM fallback"
                    )

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

                    # 保存行业映射
                    em_industry_name = em_detail.get("行业") if em_detail else None
                    self._save_industry_mappings(company_id, stock_code, em_industry_name)

            except Exception as e:
                logger.error(f"Failed to process {stock_code} {stock_name}: {e}")
                failed += 1

        logger.info(
            f"Company task finished. Total: {total}, Created: {created}, Updated: {updated}, Failed: {failed}"
        )
        return created, updated, failed

    def _save_industry_mappings(
        self, company_id: int, stock_code: str, em_industry_name: Optional[str]
    ):
        """保存公司与行业的映射关系（申万 + 东财）"""
        mappings: List[CompanyIndustryMapping] = []

        # 申万映射
        sw_entries = self._sw_mapping.get(stock_code, [])
        for idx, (l1_code, l2_code) in enumerate(sw_entries):
            mappings.append(CompanyIndustryMapping(
                company_id=company_id,
                standard_code="SW",
                level1_code=l1_code,
                level2_code=l2_code,
                is_primary=(idx == 0),
            ))

        # 东财映射
        if em_industry_name:
            em_code = self._em_name_to_code.get(em_industry_name)
            if em_code:
                # EM 没有一级分类，level1_code 设为与 level2_code 相同（满足 NOT NULL 约束）
                mappings.append(CompanyIndustryMapping(
                    company_id=company_id,
                    standard_code="EM",
                    level1_code=em_code,
                    level2_code=em_code,
                    is_primary=True,
                ))
            else:
                logger.debug(f"未找到东财行业映射: {em_industry_name} ({stock_code})")

        if mappings:
            sql = CompanyIndustryMapping.upsert_sql()
            params = [m.to_upsert_tuple() for m in mappings]
            try:
                self.db.upsert_many(sql, params)
            except Exception as e:
                logger.warning(f"保存行业映射失败 {stock_code}: {e}")

    def _parse_company_entity(
        self,
        stock_name: str,
        detail: Optional[Dict[str, Any]],
        em_detail: Optional[Dict[str, Any]] = None,
    ) -> CompanyEntity:
        """解析公司法人实体信息"""
        if detail:
            return CompanyEntity(
                company_name=detail.get("公司名称", "") or stock_name,
                short_name=detail.get("A股简称", "") or stock_name,
                industry=detail.get("所属行业", None),
                region=extract_region(detail.get("注册地址", "")),
                establish_date=parse_date(detail.get("成立日期", "")),
                registered_capital=parse_capital(detail.get("注册资金", "")),
            )
        elif em_detail:
            return CompanyEntity(
                company_name=em_detail.get("股票简称", "") or stock_name,
                short_name=em_detail.get("股票简称", "") or stock_name,
                industry=em_detail.get("行业", None),
                region=None,
                establish_date=None,
                registered_capital=None,
            )
        else:
            return CompanyEntity(
                company_name=stock_name,
                short_name=stock_name,
            )

    def _parse_securities(
        self,
        stock_code: str,
        stock_name: str,
        detail: Optional[Dict[str, Any]],
        em_detail: Optional[Dict[str, Any]] = None,
    ) -> List[SecurityEntity]:
        """解析证券信息列表（支持多市场证券）"""
        securities: List[SecurityEntity] = []

        if detail:
            a_code = detail.get("A股代码", "") or stock_code
            a_name = detail.get("A股简称", "") or stock_name
            a_listing = parse_date(detail.get("上市日期", ""))
            a_market = infer_market(a_code) or "SH"
            securities.append(SecurityEntity(
                stock_code=a_code,
                stock_name=a_name,
                market=a_market,
                security_type="A股",
                listing_date=a_listing,
                listing_status="listed",
            ))

            b_code = detail.get("B股代码", "")
            b_name = detail.get("B股简称", "")
            if b_code and str(b_code).strip():
                securities.append(SecurityEntity(
                    stock_code=str(b_code).strip(),
                    stock_name=b_name or f"{stock_name}B",
                    market=infer_market(str(b_code).strip()) or "SZ",
                    security_type="B股",
                    listing_date=a_listing,
                    listing_status="listed",
                ))

            h_code = detail.get("H股代码", "")
            h_name = detail.get("H股简称", "")
            if h_code and str(h_code).strip():
                securities.append(SecurityEntity(
                    stock_code=str(h_code).strip(),
                    stock_name=h_name or f"{stock_name}H",
                    market="HK",
                    security_type="H股",
                    listing_status="listed",
                ))

        elif em_detail:
            listing_date = parse_date(em_detail.get("上市时间", ""))
            market = infer_market(stock_code) or "SH"
            securities.append(SecurityEntity(
                stock_code=stock_code,
                stock_name=stock_name or em_detail.get("股票简称", ""),
                market=market,
                security_type="A股",
                listing_date=listing_date,
                listing_status="listed",
            ))
        else:
            market = infer_market(stock_code) or "SH"
            securities.append(SecurityEntity(
                stock_code=stock_code,
                stock_name=stock_name or stock_code,
                market=market,
                security_type="A股",
                listing_status="listed",
            ))

        return securities

    def _upsert_company(self, company: CompanyEntity) -> Optional[int]:
        """UPSERT 公司数据，返回 company_id"""
        existing = self.db.fetchone(
            "SELECT id FROM company WHERE company_name = %s",
            (company.company_name,),
        )

        if existing:
            company_id = existing[0]
            sql = """
                UPDATE company
                SET short_name = %s, industry = %s, region = %s,
                    establish_date = %s, registered_capital = %s, updated_at = NOW()
                WHERE id = %s
            """
            self.db.execute(sql, (*company.to_update_tuple(), company_id))
            return company_id
        else:
            sql = """
                INSERT INTO company
                (company_name, short_name, industry, region, establish_date,
                 registered_capital, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, NOW(), NOW())
                RETURNING id
            """
            result = self.db.execute_returning(sql, company.to_insert_tuple())
            return result[0] if result else None

    def _upsert_securities(self, company_id: int, securities: List[SecurityEntity]) -> str:
        """UPSERT 证券数据，返回 insert / update / skip"""
        if not securities:
            return "skip"

        total_result = "skip"
        for sec in securities:
            existing = self.db.fetchone(
                "SELECT id FROM company_security WHERE stock_code = %s",
                (sec.stock_code,),
            )

            if existing:
                sql = """
                    UPDATE company_security
                    SET company_id = %s, stock_name = %s, market = %s,
                        security_type = %s, listing_date = %s, listing_status = %s,
                        updated_at = NOW()
                    WHERE stock_code = %s
                """
                self.db.execute(sql, sec.to_update_tuple(company_id))
                total_result = "update"
            else:
                sql = """
                    INSERT INTO company_security
                    (company_id, stock_code, stock_name, market, security_type,
                     listing_date, listing_status, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
                """
                self.db.execute(sql, sec.to_insert_tuple(company_id))
                total_result = "insert"

        return total_result
