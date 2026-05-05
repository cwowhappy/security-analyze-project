#!/usr/bin/env python3
"""
数据采集模块入口

用法：
    python main.py                          # 启动调度器（常驻）
    python main.py --run-company            # 手动执行一次全量公司信息采集
    python main.py --company 贵州茅台       # 按公司名称采集
    python main.py --company 600519         # 按股票代码采集
    python main.py --run-finance            # 手动执行一次全量财务报告采集（默认每批100家）
    python main.py --run-finance --finance-batch-size 50   # 每批50家
    python main.py --run-finance --finance-session-id <uuid>  # 恢复指定的 Session 继续采集
    python main.py --finance 600519         # 按股票代码采集指定公司财务报告
    python main.py --finance 600519 --finance-start-year 2020 --finance-end-year 2024 --finance-incremental
                                            # 按股票代码+年份范围+增量模式采集
    python main.py --scheduler-cron-company "0 9 * * *"   # 启动调度器并注册每日09:00公司采集
    python main.py --scheduler-cron-finance "0 2 * * 0"   # 启动调度器并注册每周日02:00财务采集
    python main.py --sync-industry                        # 手动执行一次行业分类同步
    python main.py --scheduler-cron-industry "0 3 * * 1"  # 启动调度器并注册每周一03:00行业同步
"""
import argparse
import os
import time
import logging
from dotenv import load_dotenv

from collector.config import CollectorConfig
from collector.scheduler import Scheduler
from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.finance_task import FinanceTask
from collector.tasks.industry_classification_sync import run as run_industry_sync
from collector.monitor import Monitor

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


def create_db(cfg=None) -> PostgresDB:
    if cfg is None:
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


def run_scheduler(cron_company: str = None, cron_finance: str = None, cron_industry: str = None):
    logger.info("Starting security analyze collector scheduler...")
    cfg = CollectorConfig.from_env()
    db = create_db(cfg)
    scheduler = Scheduler(db=db, db_cfg=cfg.db)

    if cron_company:
        scheduler.add_company_job(cron_company)
    if cron_finance:
        scheduler.add_finance_job(cron_finance)
    if cron_industry:
        scheduler.add_industry_sync_job(cron_industry)

    scheduler.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down collector...")
        scheduler.stop()


def run_company_task():
    logger.info("Manual trigger: full company task")
    db = create_db()
    scheduler = Scheduler(db=db)
    scheduler.run_company_task_now()


def run_company_task_by_name(query: str):
    logger.info(f"Manual trigger: company task by query '{query}'")
    db = create_db()
    scheduler = Scheduler(db=db)
    scheduler.run_company_task_by_name(query)


def run_industry_sync_task():
    logger.info("Manual trigger: industry classification sync task")
    db = create_db()
    run_industry_sync(db=db)


def run_finance_task(start_year=None, end_year=None, incremental=False, batch_size=100, session_id=None):
    cfg = CollectorConfig.from_env()
    if session_id:
        logger.info(f"Manual trigger: resume finance task with session_id={session_id}")
    else:
        logger.info("Manual trigger: full finance task")
        if start_year or end_year:
            logger.info(f"Year range: {start_year or 'all'} - {end_year or 'all'}")
        if incremental:
            logger.info("Incremental mode enabled")
        logger.info(f"Batch size: {batch_size}")
    db = create_db(cfg)
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
    task.run(start_year=start_year, end_year=end_year, incremental=incremental, batch_size=batch_size, session_id=session_id)


def run_finance_task_by_stock(stock_code: str, start_year=None, end_year=None, incremental=False):
    cfg = CollectorConfig.from_env()
    logger.info(f"Manual trigger: finance task by stock '{stock_code}'")
    if start_year or end_year:
        logger.info(f"Year range: {start_year or 'all'} - {end_year or 'all'}")
    if incremental:
        logger.info("Incremental mode enabled")
    db = create_db(cfg)
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
    if start_year is not None or end_year is not None:
        task.run_by_stock_code_and_years(stock_code, start_year=start_year, end_year=end_year, incremental=incremental)
    else:
        task.run_by_stock_code(stock_code, incremental=incremental)


def main():
    parser = argparse.ArgumentParser(description="Security Analyze Collector")
    parser.add_argument(
        "--run-company",
        action="store_true",
        help="手动执行一次全量公司信息采集任务",
    )
    parser.add_argument(
        "--company",
        type=str,
        metavar="QUERY",
        help="按公司名称或股票代码采集指定公司（例如：--company 贵州茅台 或 --company 600519）",
    )
    parser.add_argument(
        "--run-finance",
        action="store_true",
        help="手动执行一次全量财务报告采集任务",
    )
    parser.add_argument(
        "--finance",
        type=str,
        metavar="STOCK_CODE",
        help="按股票代码采集指定公司财务报告（例如：--finance 600519）",
    )
    parser.add_argument(
        "--finance-start-year",
        type=int,
        metavar="YEAR",
        help="财务报告采集起始年份（与 --finance 或 --run-finance 配合使用）",
    )
    parser.add_argument(
        "--finance-end-year",
        type=int,
        metavar="YEAR",
        help="财务报告采集结束年份（与 --finance 或 --run-finance 配合使用）",
    )
    parser.add_argument(
        "--finance-incremental",
        action="store_true",
        help="增量模式：仅采集最新报告期之后的新增数据",
    )
    parser.add_argument(
        "--finance-batch-size",
        type=int,
        default=100,
        metavar="N",
        help="全量财务报告采集时的批次大小（默认100，仅与 --run-finance 配合使用）",
    )
    parser.add_argument(
        "--finance-session-id",
        type=str,
        metavar="UUID",
        help="恢复指定的财务报告采集 Session（例如：--finance-session-id a1b2c3d4...）",
    )
    parser.add_argument(
        "--scheduler-cron-company",
        type=str,
        metavar="CRON",
        help="启动调度器并注册公司采集定时任务（cron 表达式，例如 '0 9 * * *'）",
    )
    parser.add_argument(
        "--scheduler-cron-finance",
        type=str,
        metavar="CRON",
        help="启动调度器并注册财务采集定时任务（cron 表达式，例如 '0 2 * * 0'）",
    )
    parser.add_argument(
        "--sync-industry",
        action="store_true",
        help="手动执行一次行业分类体系同步任务（申万 + 东财）",
    )
    parser.add_argument(
        "--scheduler-cron-industry",
        type=str,
        metavar="CRON",
        help="启动调度器并注册行业分类同步定时任务（cron 表达式，例如 '0 3 * * 1'）",
    )
    args = parser.parse_args()

    if args.finance:
        run_finance_task_by_stock(
            args.finance,
            start_year=args.finance_start_year,
            end_year=args.finance_end_year,
            incremental=args.finance_incremental,
        )
    elif args.run_finance:
        if args.finance_session_id:
            run_finance_task(session_id=args.finance_session_id)
        else:
            run_finance_task(
                start_year=args.finance_start_year,
                end_year=args.finance_end_year,
                incremental=args.finance_incremental,
                batch_size=args.finance_batch_size,
            )
    elif args.company:
        run_company_task_by_name(args.company)
    elif args.run_company:
        run_company_task()
    elif args.sync_industry:
        run_industry_sync_task()
    else:
        run_scheduler(
            cron_company=args.scheduler_cron_company,
            cron_finance=args.scheduler_cron_finance,
            cron_industry=args.scheduler_cron_industry,
        )


if __name__ == "__main__":
    main()
