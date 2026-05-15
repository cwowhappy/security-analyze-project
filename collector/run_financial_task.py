#!/usr/bin/env python3
"""财务数据采集 — TaskExecutor 程序化调用示例。

用法:
    poetry run python run_financial_task.py --type full --code 000001
    poetry run python run_financial_task.py --type income
    poetry run python run_financial_task.py --type indicator --code 600519
"""

import argparse
import sys

from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.infrastructure.db import close_pool, init_pool
from data_collector.infrastructure.logging.config import configure_logging
from data_collector.task_executor import TaskExecutor


def main() -> int:
    parser = argparse.ArgumentParser(description="财务数据采集（TaskExecutor 程序化调用）")
    parser.add_argument(
        "--type", "-t",
        required=True,
        choices=["income", "balance", "cashflow", "indicator", "full"],
        help="采集类型",
    )
    parser.add_argument("--code", "-c", help="股票代码，不传则全量")
    args = parser.parse_args()

    # 初始化
    settings = Settings()
    configure_logging(log_level=settings.log_level)
    init_pool(settings)

    try:
        # 构造任务
        task = CollectionTask(
            task_type=f"financial_{args.type}",
            task_params={"stock_code": args.code} if args.code else {},
        )

        # 执行
        executor = TaskExecutor(settings=settings)
        result = executor.execute(task)

        # 输出结果
        print("\n" + "=" * 40)
        print("任务执行结果")
        print("=" * 40)
        print(f"  任务类型 : {result.task_type}")
        print(f"  状态     : {result.status}")
        print(f"  数据源   : {result.data_source}")
        print(f"  总数     : {result.total_count}")
        print(f"  成功     : {result.success_count}")
        print(f"  失败     : {result.fail_count}")
        if result.error_message:
            print(f"  错误     : {result.error_message[:200]}")
        print("=" * 40)

        return 0 if result.status == "success" else 1
    finally:
        close_pool()


if __name__ == "__main__":
    sys.exit(main())
