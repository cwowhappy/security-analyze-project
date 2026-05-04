#!/usr/bin/env python3
"""批量导入公司信息，使用 akshare 批量接口替代逐个请求"""
import os
import logging

import pandas as pd
import akshare as ak
from collector.db.postgres import PostgresDB
from collector.config import CollectorConfig
from collector.utils import parse_date, infer_market

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def get_db():
    cfg = CollectorConfig.from_env()
    db_cfg = cfg.db
    return PostgresDB(
        host=db_cfg.host,
        port=db_cfg.port,
        database=db_cfg.database,
        user=db_cfg.user,
        password=db_cfg.password,
        pool_min_size=cfg.db_pool_min_size,
        pool_max_size=cfg.db_pool_max_size,
        pool_max_idle=cfg.db_pool_max_idle,
        pool_max_lifetime=cfg.db_pool_max_lifetime,
    )


def fetch_all_stocks_batch():
    """使用批量接口获取所有市场数据"""
    records = []

    # 上海市场
    logger.info("Fetching SH market...")
    df_sh = ak.stock_info_sh_name_code()
    for _, row in df_sh.iterrows():
        records.append({
            "stock_code": str(row["证券代码"]).strip(),
            "stock_name": str(row["证券简称"]).strip(),
            "company_name": str(row["公司全称"]).strip() if pd.notna(row.get("公司全称")) else str(row["证券简称"]).strip(),
            "short_name": str(row["公司简称"]).strip() if pd.notna(row.get("公司简称")) else str(row["证券简称"]).strip(),
            "listing_date": parse_date(row.get("上市日期")),
            "market": "SH",
            "industry": None,
            "region": None,
        })

    # 深圳市场
    logger.info("Fetching SZ market...")
    df_sz = ak.stock_info_sz_name_code()
    for _, row in df_sz.iterrows():
        records.append({
            "stock_code": str(row["A股代码"]).strip(),
            "stock_name": str(row["A股简称"]).strip(),
            "company_name": str(row["A股简称"]).strip(),
            "short_name": str(row["A股简称"]).strip(),
            "listing_date": parse_date(row.get("A股上市日期")),
            "market": "SZ",
            "industry": str(row["所属行业"]).strip() if pd.notna(row.get("所属行业")) else None,
            "region": None,
        })

    # 北京市场
    logger.info("Fetching BJ market...")
    df_bj = ak.stock_info_bj_name_code()
    for _, row in df_bj.iterrows():
        records.append({
            "stock_code": str(row["证券代码"]).strip(),
            "stock_name": str(row["证券简称"]).strip(),
            "company_name": str(row["证券简称"]).strip(),
            "short_name": str(row["证券简称"]).strip(),
            "listing_date": parse_date(row.get("上市日期")),
            "market": "BJ",
            "industry": str(row["所属行业"]).strip() if pd.notna(row.get("所属行业")) else None,
            "region": str(row["地区"]).strip() if pd.notna(row.get("地区")) else None,
        })

    logger.info(f"Total fetched: {len(records)}")
    return records


def upsert_company(db, company):
    existing = db.fetchone(
        "SELECT id FROM company WHERE company_name = %s",
        (company["company_name"],),
    )
    if existing:
        company_id = existing[0]
        db.execute(
            """
            UPDATE company
            SET short_name = %s, industry = %s, region = %s,
                establish_date = %s, registered_capital = %s, updated_at = NOW()
            WHERE id = %s
            """,
            (
                company["short_name"],
                company["industry"],
                company["region"],
                company.get("establish_date"),
                company.get("registered_capital"),
                company_id,
            ),
        )
        return company_id, "update"
    else:
        result = db.execute_returning(
            """
            INSERT INTO company
            (company_name, short_name, industry, region, establish_date,
             registered_capital, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, NOW(), NOW())
            RETURNING id
            """,
            (
                company["company_name"],
                company["short_name"],
                company["industry"],
                company["region"],
                company.get("establish_date"),
                company.get("registered_capital"),
            ),
        )
        return result[0] if result else None, "insert"


def upsert_security(db, company_id, sec):
    existing = db.fetchone(
        "SELECT id FROM company_security WHERE stock_code = %s",
        (sec["stock_code"],),
    )
    if existing:
        db.execute(
            """
            UPDATE company_security
            SET company_id = %s, stock_name = %s, market = %s,
                security_type = %s, listing_date = %s, listing_status = %s,
                updated_at = NOW()
            WHERE stock_code = %s
            """,
            (
                company_id,
                sec["stock_name"],
                sec["market"],
                sec.get("security_type", "A股"),
                sec["listing_date"],
                sec.get("listing_status", "listed"),
                sec["stock_code"],
            ),
        )
        return "update"
    else:
        db.execute(
            """
            INSERT INTO company_security
            (company_id, stock_code, stock_name, market, security_type,
             listing_date, listing_status, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            """,
            (
                company_id,
                sec["stock_code"],
                sec["stock_name"],
                sec["market"],
                sec.get("security_type", "A股"),
                sec["listing_date"],
                sec.get("listing_status", "listed"),
            ),
        )
        return "insert"


def main():
    db = get_db()
    db.connect()

    logger.info("Fetching all stocks via batch API...")
    records = fetch_all_stocks_batch()

    created = 0
    updated = 0
    failed = 0

    for i, rec in enumerate(records, 1):
        try:
            company = {
                "company_name": rec["company_name"],
                "short_name": rec["short_name"],
                "industry": rec["industry"],
                "region": rec["region"],
                "establish_date": None,
                "registered_capital": None,
            }
            company_id, company_action = upsert_company(db, company)
            if company_id:
                sec_action = upsert_security(db, company_id, rec)
                if sec_action == "insert":
                    created += 1
                else:
                    updated += 1
            if i % 500 == 0:
                logger.info(f"Progress: {i}/{len(records)}, Created: {created}, Updated: {updated}")
        except Exception as e:
            logger.error(f"Failed to process {rec['stock_code']}: {e}")
            failed += 1

    logger.info(f"Done. Total: {len(records)}, Created: {created}, Updated: {updated}, Failed: {failed}")


if __name__ == "__main__":
    main()
