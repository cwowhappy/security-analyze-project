"""采集器 CLI 入口。

以数据类型为维度组织子命令：
  stock    股票数据采集
  company  公司数据采集

每个子命令支持两种操作模式（互斥）：
  --full, -f        全量采集
  --code, -c        指定数据编号（如股票代码）

通用选项：
  --source, -s      指定数据源（akshare / tushare）
"""

import argparse
import sys
from typing import Sequence

import structlog

from data_collector.adapters.akshare_source import AkshareDataSource
from data_collector.adapters.db_collection_task_repository import DbCollectionTaskRepository
from data_collector.adapters.db_company_repository import DbCompanyRepository
from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.adapters.tushare_source import TushareDataSource
from data_collector.config import get_settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.infrastructure.db import close_pool, init_pool
from data_collector.infrastructure.logging.config import configure_logging
from data_collector.task_executor import TaskExecutor

logger = structlog.get_logger(__name__)


def _init_context() -> tuple[TaskExecutor, DbCollectionTaskRepository]:
    """初始化数据库连接、数据源、仓库和执行器。

    Returns:
        (任务执行器, 任务仓库)
    """
    settings = get_settings()
    configure_logging(
        log_level=settings.log_level,
        json_format=settings.log_format.lower() == "json",
    )

    init_pool(settings)

    sources = [AkshareDataSource(settings), TushareDataSource(settings)]
    stock_repo = DbStockRepository()
    company_repo = DbCompanyRepository()
    task_repo = DbCollectionTaskRepository()

    executor = TaskExecutor(
        sources=sources,
        stock_repo=stock_repo,
        company_repo=company_repo,
        settings=settings,
    )
    return executor, task_repo


def _build_task(data_type: str, args: argparse.Namespace) -> CollectionTask:
    """根据 CLI 参数构造 CollectionTask。

    Args:
        data_type: 数据类型（stock / company）。
        args: 解析后的命令行参数。

    Returns:
        构造好的采集任务。
    """
    if args.code:
        task_type = f"{data_type}_single"
    else:
        # 默认行为：全量采集
        task_type = f"{data_type}_full"

    task_params: dict = {}
    if args.code:
        task_params["stock_code"] = args.code

    return CollectionTask(
        task_type=task_type,
        task_params=task_params,
        data_source=args.source,
    )


def _execute_and_report(
    executor: TaskExecutor,
    task_repo: DbCollectionTaskRepository,
    task: CollectionTask,
) -> int:
    """执行任务并打印结果，返回退出码。

    Returns:
        0 表示成功，1 表示失败。
    """
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
    """执行指定数据类型的采集任务。

    Args:
        data_type: 数据类型（stock / company）。
        args: 解析后的命令行参数。

    Returns:
        进程退出码。
    """
    executor, task_repo = _init_context()
    try:
        task = _build_task(data_type, args)
        return _execute_and_report(executor, task_repo, task)
    finally:
        close_pool()


def _add_common_args(parser: argparse.ArgumentParser) -> None:
    """为子命令添加通用参数。"""
    parser.add_argument(
        "--source",
        "-s",
        choices=["akshare", "tushare"],
        help="指定数据源",
    )


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

    # API 子命令
    api_parser = subparsers.add_parser("api", help="启动 HTTP API 服务")
    api_parser.add_argument(
        "--host",
        default="0.0.0.0",
        help="绑定地址（默认: 0.0.0.0）",
    )
    api_parser.add_argument(
        "--port",
        type=int,
        default=8000,
        help="监听端口（默认: 8000）",
    )

    # stock 子命令
    stock_parser = subparsers.add_parser("stock", help="股票数据采集")
    _add_mode_args(stock_parser)
    _add_common_args(stock_parser)

    # company 子命令
    company_parser = subparsers.add_parser("company", help="公司数据采集")
    _add_mode_args(company_parser)
    _add_common_args(company_parser)

    return parser


def run_api(args: argparse.Namespace) -> int:
    """启动 FastAPI HTTP 服务。"""
    import uvicorn

    settings = get_settings()
    configure_logging(
        log_level=settings.log_level,
        json_format=settings.log_format.lower() == "json",
    )

    logger.info("启动采集器 API 服务", host=args.host, port=args.port)
    uvicorn.run(
        "data_collector.api:app",
        host=args.host,
        port=args.port,
        log_level=settings.log_level.lower(),
    )
    return 0


def main(argv: Sequence[str] | None = None) -> int:
    """CLI 主入口。"""
    parser = build_parser()
    args = parser.parse_args(argv)

    if args.command == "api":
        return run_api(args)
    elif args.command == "stock":
        return _run_data_type("stock", args)
    elif args.command == "company":
        return _run_data_type("company", args)
    else:
        parser.print_help()
        return 1


if __name__ == "__main__":
    sys.exit(main())
