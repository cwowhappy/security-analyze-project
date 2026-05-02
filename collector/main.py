#!/usr/bin/env python3
"""
数据采集模块入口

用法：
    python main.py                          # 启动调度器（常驻）
    python main.py --run-company            # 手动执行一次全量公司信息采集
    python main.py --company 贵州茅台       # 按公司名称采集
    python main.py --company 600519         # 按股票代码采集
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


def run_finance_task():
    logger.info("Manual trigger: full finance task")
    db = create_db()
    source = AkshareSource()
    monitor = Monitor(db)
    task = FinanceTask(db=db, source=source, monitor=monitor)
    task.run()


def run_finance_task_by_stock(stock_code: str):
    logger.info(f"Manual trigger: finance task by stock '{stock_code}'")
    db = create_db()
    source = AkshareSource()
    monitor = Monitor(db)
    task = FinanceTask(db=db, source=source, monitor=monitor)
    task.run_by_stock_code(stock_code)


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
    args = parser.parse_args()

    if args.finance:
        run_finance_task_by_stock(args.finance)
    elif args.run_finance:
        run_finance_task()
    elif args.company:
        run_company_task_by_name(args.company)
    elif args.run_company:
        run_company_task()
    else:
        run_scheduler()


if __name__ == "__main__":
    main()
