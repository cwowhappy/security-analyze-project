import json
import logging
import os
from typing import List, Dict, Any, Optional

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
import numpy as np
from collector.utils import parse_date, parse_capital, extract_region, infer_market
from collector.models import CompanyEntity, SecurityEntity, CompanyIndustryMapping
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)


def _load_sw_mapping_json() -> tuple[dict, dict]:
    """从 JSON 文件加载申万行业编码映射。"""
    json_path = os.path.join(
        os.path.dirname(__file__), "..", "data", "sw_industry_mapping.json"
    )
    try:
        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data.get("L1", {}), data.get("L2", {})
    except Exception as e:
        logger.warning(f"加载申万行业映射文件失败: {e}")
        return {}, {}


def _safe(val, default=""):
    """清理 NaN / None，避免 Pydantic 验证失败"""
    if val is None or (isinstance(val, float) and np.isnan(val)):
        return default
    return val


class CompanyTask(BaseTask):
    """采集公司基本信息任务（支持公司-证券分离模型）"""

    task_name = "company"
    data_type = "company"

    def __init__(self, db: PostgresDB, source: BaseDataSource, monitor: Monitor = None):
        super().__init__(db=db, source=source, monitor=monitor)
        self._sw_mapping: Dict[str, List[tuple]] = {}
        self._em_name_to_code: Dict[str, str] = {}
        self._mappings_loaded = False
        self._l1_mapping, self._l2_mapping = _load_sw_mapping_json()

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
                l1_code = self._l1_mapping.get(l1_raw)
                l2_code = self._l2_mapping.get(l2_raw)
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
        """向后兼容的手动执行入口。"""
        result = self.run_full()
        return result.rows

    def run_full(self, **kwargs) -> TaskResult:
        """全量采集所有 A 股公司信息"""
        logger.info("Starting full company task...")
        self._preload_mappings()

        stock_list = self.source.get_stock_list()
        logger.info(f"Fetched {len(stock_list)} stocks from source")

        created, updated, failed = self._process_stocks(stock_list)
        rows = created + updated
        return TaskResult(created=created, updated=updated, failed=failed, rows=rows)

    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """按公司名称或股票代码列表采集指定公司信息"""
        logger.info(f"Starting company task by identifiers: {identifiers}")
        self._preload_mappings()

        all_matches = []
        for query in identifiers:
            matches = self.source.search_by_name(query)
            all_matches.extend(matches)

        if not all_matches:
            logger.warning(f"No company found matching any of {identifiers}")
            return TaskResult(rows=0)

        # 去重
        seen = set()
        unique_matches = []
        for m in all_matches:
            code = m.get("code", "")
            if code and code not in seen:
                seen.add(code)
                unique_matches.append(m)

        logger.info(f"Found {len(unique_matches)} unique matches")
        created, updated, failed = self._process_stocks(unique_matches)
        rows = created + updated
        return TaskResult(created=created, updated=updated, failed=failed, rows=rows)

    def run_incremental(self, **kwargs) -> TaskResult:
        """增量采集：基于 company.updated_at 只采集近期变更（暂按全量处理）。"""
        logger.info("Company task incremental mode (fallback to full)")
        return self.run_full(**kwargs)

    def run_by_name(self, query: str):
        """【向后兼容】按公司名称或股票代码采集指定公司信息"""
        result = self.run_partial(identifiers=[query])
        return result.rows

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
                        stock_name = _safe(detail.get("A股简称")) or _safe(detail.get("公司名称"))
                    elif em_detail:
                        stock_name = _safe(em_detail.get("股票简称"))

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
                company_name=_safe(detail.get("公司名称")) or _safe(stock_name) or "",
                short_name=_safe(detail.get("A股简称")) or _safe(stock_name) or "",
                industry=_safe(detail.get("所属行业"), None),
                region=extract_region(_safe(detail.get("注册地址"), "")),
                establish_date=parse_date(_safe(detail.get("成立日期"), "")),
                registered_capital=parse_capital(_safe(detail.get("注册资金"), "")),
            )
        elif em_detail:
            return CompanyEntity(
                company_name=_safe(em_detail.get("股票简称")) or _safe(stock_name) or "",
                short_name=_safe(em_detail.get("股票简称")) or _safe(stock_name) or "",
                industry=_safe(em_detail.get("行业"), None),
                region=None,
                establish_date=None,
                registered_capital=None,
            )
        else:
            return CompanyEntity(
                company_name=_safe(stock_name) or "",
                short_name=_safe(stock_name) or "",
            )

    @staticmethod
    def _parse_shares(raw_value: Any) -> Optional[float]:
        """解析股本字段，统一转换为'股'为单位。akshare 可能返回数值或科学计数法字符串。"""
        if raw_value is None:
            return None
        try:
            val = float(raw_value)
        except (ValueError, TypeError):
            return None
        if np.isnan(val):
            return None
        # akshare stock_individual_info_em 的"总股本"通常以"股"返回，但数值可能很大（如1.2561e+10）。
        # 如果值小于 1e6，可能是以"亿股"为单位，需要乘 1e8
        if val > 0 and val < 1_000_000:
            val = val * 100_000_000
        return val

    def _parse_securities(
        self,
        stock_code: str,
        stock_name: str,
        detail: Optional[Dict[str, Any]],
        em_detail: Optional[Dict[str, Any]] = None,
    ) -> List[SecurityEntity]:
        """解析证券信息列表（支持多市场证券）"""
        securities: List[SecurityEntity] = []

        # 从 EM 详情提取股本信息（如有）
        total_shares = self._parse_shares(em_detail.get("总股本")) if em_detail else None
        circulating_shares = self._parse_shares(em_detail.get("流通股")) if em_detail else None

        if detail:
            a_code = _safe(detail.get("A股代码")) or stock_code
            a_name = _safe(detail.get("A股简称")) or _safe(stock_name) or stock_code
            a_listing = parse_date(_safe(detail.get("上市日期"), ""))
            a_market = infer_market(a_code) or "SH"
            securities.append(SecurityEntity(
                stock_code=a_code,
                stock_name=a_name,
                market=a_market,
                security_type="A股",
                listing_date=a_listing,
                listing_status="listed",
                total_shares=total_shares,
                circulating_shares=circulating_shares,
            ))

            b_code = _safe(detail.get("B股代码"), "")
            b_name = _safe(detail.get("B股简称"), "")
            if b_code and str(b_code).strip():
                securities.append(SecurityEntity(
                    stock_code=str(b_code).strip(),
                    stock_name=b_name or f"{stock_name}B",
                    market=infer_market(str(b_code).strip()) or "SZ",
                    security_type="B股",
                    listing_date=a_listing,
                    listing_status="listed",
                ))

            h_code = _safe(detail.get("H股代码"), "")
            h_name = _safe(detail.get("H股简称"), "")
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
                total_shares=total_shares,
                circulating_shares=circulating_shares,
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
                        total_shares = %s, circulating_shares = %s, market_cap = %s,
                        updated_at = NOW()
                    WHERE stock_code = %s
                """
                self.db.execute(sql, sec.to_update_tuple(company_id))
                total_result = "update"
            else:
                sql = """
                    INSERT INTO company_security
                    (company_id, stock_code, stock_name, market, security_type,
                     listing_date, listing_status, total_shares, circulating_shares, market_cap,
                     created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
                """
                self.db.execute(sql, sec.to_insert_tuple(company_id))
                total_result = "insert"

        return total_result
