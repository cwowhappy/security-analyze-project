#!/usr/bin/env python3
"""按股票代码范围采集财务报告"""
import os
import logging
import sys

from collector.config import CollectorConfig
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor
from collector.tasks.finance_task import FinanceTask

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


def get_stock_codes_in_range(db, start_code: str, end_code: str):
    """从数据库获取指定代码区间的 A 股股票"""
    sql = """
        SELECT stock_code FROM company_security
        WHERE stock_code >= %s AND stock_code <= %s
          AND security_type = 'A股' AND listing_status = 'listed'
        ORDER BY stock_code
    """
    rows = db.fetchall(sql, (start_code, end_code))
    return [row[0] for row in rows if row and row[0]]


def main():
    db = get_db()
    db.connect()
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
        batch_concurrent_workers=cfg.finance_batch_concurrent_workers,
    )

    start_code = "000001"
    end_code = "001000"

    stock_codes = get_stock_codes_in_range(db, start_code, end_code)
    total = len(stock_codes)
    logger.info(f"区间 {start_code}-{end_code} 共有 {total} 只 A 股，开始采集财务报告...")

    created = 0
    updated = 0
    failed = 0
    skipped = 0

    for i, stock_code in enumerate(stock_codes, 1):
        try:
            logger.info(f"[{i}/{total}] 采集 {stock_code} 财务报告...")
            c, u = task.run_by_stock_code(stock_code, incremental=False)
            created += c
            updated += u
            if c == 0 and u == 0:
                skipped += 1
        except Exception as e:
            logger.error(f"[{i}/{total}] {stock_code} 采集失败: {e}")
            failed += 1

        if i % 50 == 0:
            logger.info(f"进度: {i}/{total}, 新建: {created}, 更新: {updated}, 跳过: {skipped}, 失败: {failed}")

    logger.info(
        f"区间采集完成. 总数: {total}, 新建: {created}, 更新: {updated}, "
        f"跳过: {skipped}, 失败: {failed}"
    )


if __name__ == "__main__":
    main()
