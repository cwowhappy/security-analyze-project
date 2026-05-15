"""采集器 CLI 入口。

以数据类型为维度组织子命令：
  stock      股票数据采集
  company    公司数据采集
  supplement 字段补充采集
  financial  财务数据采集

每个子命令支持两种操作模式（互斥）：
  --full, -f        全量采集
  --code, -c        指定数据编号（如股票代码）
"""

import argparse
import sys
from typing import Sequence

import structlog

from data_collector.adapters.db_collection_task_repository import DbCollectionTaskRepository
from data_collector.config import get_settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.infrastructure.db import close_pool, init_pool
from data_collector.infrastructure.logging.config import configure_logging
from data_collector.task_executor import TaskExecutor

logger = structlog.get_logger(__name__)


def _init_context() -> tuple[TaskExecutor, DbCollectionTaskRepository]:
    """初始化数据库连接和执行器。"""
    settings = get_settings()
    configure_logging(
        log_level=settings.log_level,
        json_format=settings.log_format.lower() == "json",
    )

    init_pool(settings)

    executor = TaskExecutor(settings=settings)
    task_repo = DbCollectionTaskRepository()
    return executor, task_repo


def _build_task(data_type: str, args: argparse.Namespace) -> CollectionTask:
    """根据 CLI 参数构造 CollectionTask。"""
    if args.code:
        task_type = f"{data_type}_single"
    else:
        task_type = f"{data_type}_full"

    task_params: dict = {}
    if args.code:
        task_params["stock_code"] = args.code

    return CollectionTask(
        task_type=task_type,
        task_params=task_params,
    )


def _execute_and_report(
    executor: TaskExecutor,
    task_repo: DbCollectionTaskRepository,
    task: CollectionTask,
) -> int:
    """执行任务并打印结果，返回退出码。"""
    result = executor.execute(task)
    task_repo.save(result)

    print("\n任务执行结果:")
    print(f"  数据类型: {result.task_type.split('_')[0]}")
    print(f"  任务类型: {result.task_type}")
    print(f"  状态: {result.status}")
    print(f"  数据源: {result.data_source}")
    print(f"  总数: {result.total_count}")
    print(f"  成功: {result.success_count}")
    print(f"  失败: {result.fail_count}")
    if result.error_message:
        print(f"  错误: {result.error_message[:200]}")

    return 0 if result.status == "success" else 1


def _run_data_type(data_type: str, args: argparse.Namespace) -> int:
    """执行指定数据类型的采集任务。"""
    executor, task_repo = _init_context()
    try:
        task = _build_task(data_type, args)
        return _execute_and_report(executor, task_repo, task)
    finally:
        close_pool()


def _run_financial(args: argparse.Namespace) -> int:
    """执行财务数据采集任务。"""
    executor, task_repo = _init_context()
    try:
        task_type = f"financial_{args.type}"
        task_params: dict = {}
        if args.code:
            task_params["stock_code"] = args.code

        task = CollectionTask(
            task_type=task_type,
            task_params=task_params,
        )
        return _execute_and_report(executor, task_repo, task)
    finally:
        close_pool()


def _add_mode_args(parser: argparse.ArgumentParser) -> None:
    """为子命令添加互斥的操作模式参数。"""
    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument(
        "--full",
        "-f",
        action="store_true",
        help="全量采集",
    )
    mode_group.add_argument(
        "--code",
        "-c",
        metavar="CODE",
        help="指定数据编号（如股票代码）",
    )


def build_parser() -> argparse.ArgumentParser:
    """构建 CLI 参数解析器。"""
    parser = argparse.ArgumentParser(
        prog="data-collector",
        description="股票数据采集器 CLI",
    )
    subparsers = parser.add_subparsers(dest="command", help="可用子命令")

    # stock 子命令
    stock_parser = subparsers.add_parser("stock", help="股票数据采集")
    _add_mode_args(stock_parser)

    # company 子命令
    company_parser = subparsers.add_parser("company", help="公司数据采集")
    _add_mode_args(company_parser)

    # supplement 子命令
    supplement_parser = subparsers.add_parser("supplement", help="字段补充采集（Tushare）")
    _add_mode_args(supplement_parser)

    # financial 子命令
    financial_parser = subparsers.add_parser("financial", help="财务数据采集")
    financial_parser.add_argument(
        "--type", "-t",
        required=True,
        choices=["income", "balance", "cashflow", "indicator", "full"],
        help="采集类型: income(利润表) / balance(资产负债表) / cashflow(现金流量表) / indicator(指标计算) / full(三表+指标全量)",
    )
    _add_mode_args(financial_parser)

    return parser


def main(argv: Sequence[str] | None = None) -> int:
    """CLI 主入口。"""
    parser = build_parser()
    args = parser.parse_args(argv)

    if args.command == "stock":
        return _run_data_type("stock", args)
    elif args.command == "company":
        return _run_data_type("company", args)
    elif args.command == "supplement":
        return _run_data_type("field", args)
    elif args.command == "financial":
        return _run_financial(args)
    else:
        parser.print_help()
        return 1


if __name__ == "__main__":
    sys.exit(main())
