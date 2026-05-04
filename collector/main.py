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
"""
import argparse
import os
import time
import logging
from dotenv import load_dotenv

from collector.scheduler import Scheduler
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
    db = PostgresDB(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        database=os.getenv("DB_NAME", "security_analyze"),
        user=os.getenv("DB_USER", "stock"),
        password=os.getenv("DB_PASSWORD", "stock"),
    )
    return db


def run_scheduler():
    logger.info("Starting security analyze collector scheduler...")

    db = create_db()
    scheduler = Scheduler(db=db)
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


def run_finance_task(start_year=None, end_year=None, incremental=False, batch_size=100, session_id=None):
    if session_id:
        logger.info(f"Manual trigger: resume finance task with session_id={session_id}")
    else:
        logger.info("Manual trigger: full finance task")
        if start_year or end_year:
            logger.info(f"Year range: {start_year or 'all'} - {end_year or 'all'}")
        if incremental:
            logger.info("Incremental mode enabled")
        logger.info(f"Batch size: {batch_size}")
    db = create_db()
    source = AkshareSource()
    monitor = Monitor(db)
    task = FinanceTask(db=db, source=source, monitor=monitor)
    task.run(start_year=start_year, end_year=end_year, incremental=incremental, batch_size=batch_size, session_id=session_id)


def run_finance_task_by_stock(stock_code: str, start_year=None, end_year=None, incremental=False):
    logger.info(f"Manual trigger: finance task by stock '{stock_code}'")
    if start_year or end_year:
        logger.info(f"Year range: {start_year or 'all'} - {end_year or 'all'}")
    if incremental:
        logger.info("Incremental mode enabled")
    db = create_db()
    source = AkshareSource()
    monitor = Monitor(db)
    task = FinanceTask(db=db, source=source, monitor=monitor)
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
    else:
        run_scheduler()


if __name__ == "__main__":
    main()
