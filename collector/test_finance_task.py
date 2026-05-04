#!/usr/bin/env python3
"""财务报告采集脚本测试 — 采集前 10 家公司"""

import os
import logging
from dotenv import load_dotenv

from collector.config import CollectorConfig
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.finance_task import FinanceTask
from collector.monitor import Monitor

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def create_db() -> PostgresDB:
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


def get_top_n_stock_codes(db: PostgresDB, n: int = 10) -> list[str]:
    sql = """
        SELECT stock_code FROM company_security
        WHERE listing_status = 'listed' AND security_type = 'A股'
        ORDER BY stock_code
        LIMIT %s
    """
    rows = db.fetchall(sql, (n,))
    return [row[0] for row in rows if row and row[0]]


def main():
    db = create_db()
    cfg = CollectorConfig.from_env()
    source = AkshareSource(
        max_retries=cfg.source_max_retries,
        retry_delay=cfg.source_retry_delay,
        retry_backoff=cfg.source_retry_backoff,
    )
    monitor = Monitor(db)
    task = FinanceTask(
        db=db, source=source, monitor=monitor,
        max_workers=cfg.finance_max_workers,
        batch_concurrent_workers=1,  # 测试环境串行执行
    )

    stock_codes = get_top_n_stock_codes(db, n=10)
    logger.info(f"准备采集 {len(stock_codes)} 家公司财务报告: {stock_codes}")

    total_created = 0
    total_updated = 0
    total_failed = 0

    for idx, stock_code in enumerate(stock_codes, 1):
        logger.info(f"[{idx}/{len(stock_codes)}] 开始采集 {stock_code}")
        try:
            created, updated = task.run_by_stock_code_and_years(stock_code, start_year=2020, end_year=2026)
            total_created += created
            total_updated += updated
            logger.info(f"[{idx}/{len(stock_codes)}] {stock_code} 完成，新建: {created}, 更新: {updated}")
        except Exception as e:
            total_failed += 1
            logger.error(f"[{idx}/{len(stock_codes)}] {stock_code} 采集失败: {e}")

    logger.info(
        f"测试采集结束。公司数: {len(stock_codes)}, "
        f"新建: {total_created}, 更新: {total_updated}, 失败: {total_failed}"
    )

    total_rows = db.fetchone("SELECT COUNT(*) FROM financial_report WHERE stock_code = ANY(%s)", (stock_codes,))
    logger.info(f"数据库验证: financial_report 表中这 10 家公司共 {total_rows[0]} 条记录")


if __name__ == "__main__":
    main()
