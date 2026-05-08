"""数据采集模块命令行入口。

使用子命令风格组织 CLI，每个子命令代表一种数据类型：

    python main.py company              # 全量公司信息采集
    python main.py company --code 600941 # 指定公司采集
    python main.py finance              # 全量财务报告采集
    python main.py finance --code 600941 # 指定公司财务
    python main.py industry             # 行业分类全量同步
    python main.py industry --sw        # 仅同步申万
    python main.py index-basic          # 指数基本信息采集
    python main.py index-history        # 指数历史行情采集
    python main.py etf                  # ETF 基本信息采集
    python main.py quote                # 日行情采集
    python main.py schedule --company "0 2 * * *"  # 启动调度器

内部使用 TaskRunner 统一组装依赖并执行任务。
"""
import argparse
import logging
import time
from dotenv import load_dotenv

from collector.runner import TaskRunner
from collector.scheduler import Scheduler
from collector.tasks.company_task import CompanyTask
from collector.tasks.finance_task import FinanceTask
from collector.tasks.quote_task import QuoteTask
from collector.tasks.index_basic_task import IndexBasicTask
from collector.tasks.index_history_task import IndexHistoryTask
from collector.tasks.etf_basic_task import EtfBasicTask
from collector.tasks.industry_task import IndustryTask

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Security Analyze Collector")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # ------------------------------------------------------------------
    # company 子命令
    # ------------------------------------------------------------------
    company_parser = subparsers.add_parser("company", help="公司信息采集")
    company_parser.add_argument(
        "--stock-code",
        help="股票代码或公司名称（可选，不传则全量）",
    )

    # ------------------------------------------------------------------
    # finance 子命令
    # ------------------------------------------------------------------
    finance_parser = subparsers.add_parser("finance", help="财务报告采集")
    finance_parser.add_argument(
        "--stock-code",
        help="股票代码（可选，不传则全量）",
    )
    finance_parser.add_argument(
        "--start-year",
        type=int,
        metavar="YEAR",
        help="财务报告采集起始年份",
    )
    finance_parser.add_argument(
        "--end-year",
        type=int,
        metavar="YEAR",
        help="财务报告采集结束年份",
    )
    finance_parser.add_argument(
        "--incremental",
        action="store_true",
        help="增量模式：仅采集最新报告期之后的新增数据",
    )
    finance_parser.add_argument(
        "--batch-size",
        type=int,
        default=100,
        metavar="N",
        help="全量财务报告采集时的批次大小（默认100）",
    )
    finance_parser.add_argument(
        "--resume",
        metavar="UUID",
        help="恢复指定的财务报告采集 Session",
    )

    # ------------------------------------------------------------------
    # industry 子命令
    # ------------------------------------------------------------------
    industry_parser = subparsers.add_parser("industry", help="行业分类同步")
    industry_parser.add_argument(
        "--sw",
        action="store_true",
        help="仅同步申万行业分类",
    )
    industry_parser.add_argument(
        "--em",
        action="store_true",
        help="仅同步东方财富行业板块",
    )

    # ------------------------------------------------------------------
    # index-basic 子命令
    # ------------------------------------------------------------------
    subparsers.add_parser("index-basic", help="指数基本信息采集")

    # ------------------------------------------------------------------
    # index-history 子命令
    # ------------------------------------------------------------------
    index_history_parser = subparsers.add_parser("index-history", help="指数历史行情采集")
    index_history_parser.add_argument(
        "--index-code",
        help="指数代码（可选，不传则全量）",
    )
    index_history_parser.add_argument(
        "--incremental",
        action="store_true",
        help="增量模式：仅采集已有数据最大日期之后的数据",
    )
    index_history_parser.add_argument(
        "--resume",
        metavar="UUID",
        help="恢复指定的指数历史行情采集 Session",
    )

    # ------------------------------------------------------------------
    # etf 子命令
    # ------------------------------------------------------------------
    subparsers.add_parser("etf", help="ETF 基本信息采集")

    # ------------------------------------------------------------------
    # quote 子命令
    # ------------------------------------------------------------------
    quote_parser = subparsers.add_parser("quote", help="日行情采集")
    quote_parser.add_argument(
        "--date",
        metavar="YYYY-MM-DD",
        help="指定行情采集日期",
    )
    quote_parser.add_argument(
        "--stock-code",
        help="指定股票代码（可选，不传则采集全部持仓）",
    )

    # ------------------------------------------------------------------
    # schedule 子命令
    # ------------------------------------------------------------------
    schedule_parser = subparsers.add_parser("schedule", help="启动调度器")
    schedule_parser.add_argument(
        "--company",
        metavar="CRON",
        help="注册公司采集定时任务",
    )
    schedule_parser.add_argument(
        "--finance",
        metavar="CRON",
        help="注册财务采集定时任务",
    )
    schedule_parser.add_argument(
        "--industry",
        metavar="CRON",
        help="注册行业分类同步定时任务",
    )
    schedule_parser.add_argument(
        "--index-basic",
        metavar="CRON",
        help="注册指数基本信息采集定时任务",
    )
    schedule_parser.add_argument(
        "--index-history",
        metavar="CRON",
        help="注册指数历史行情采集定时任务",
    )
    schedule_parser.add_argument(
        "--etf",
        metavar="CRON",
        help="注册 ETF 基本信息采集定时任务",
    )
    schedule_parser.add_argument(
        "--quote",
        metavar="CRON",
        help="注册日行情采集定时任务",
    )

    return parser


def _dispatch_command(args, runner: TaskRunner) -> None:
    """根据子命令路由到对应任务。"""
    cmd = args.command

    # ------------------------------------------------------------------
    # company
    # ------------------------------------------------------------------
    if cmd == "company":
        if args.stock_code:
            runner.run(CompanyTask, mode="partial", identifiers=[args.stock_code])
        else:
            runner.run(CompanyTask, mode="full")

    # ------------------------------------------------------------------
    # finance
    # ------------------------------------------------------------------
    elif cmd == "finance":
        if args.resume:
            runner.run(
                FinanceTask,
                mode="resume",
                session_id=args.resume,
                batch_size=args.batch_size,
            )
        elif args.stock_code:
            runner.run(FinanceTask, mode="partial", identifiers=[args.stock_code])
        else:
            runner.run(
                FinanceTask,
                mode="full",
                start_year=args.start_year,
                end_year=args.end_year,
                incremental=args.incremental,
                batch_size=args.batch_size,
            )

    # ------------------------------------------------------------------
    # industry
    # ------------------------------------------------------------------
    elif cmd == "industry":
        if args.sw and args.em:
            runner.run(IndustryTask, mode="partial", identifiers=["SW", "EM"])
        elif args.sw:
            runner.run(IndustryTask, mode="partial", identifiers=["SW"])
        elif args.em:
            runner.run(IndustryTask, mode="partial", identifiers=["EM"])
        else:
            runner.run(IndustryTask, mode="full")

    # ------------------------------------------------------------------
    # index-basic
    # ------------------------------------------------------------------
    elif cmd == "index-basic":
        runner.run(IndexBasicTask, mode="full")

    # ------------------------------------------------------------------
    # index-history
    # ------------------------------------------------------------------
    elif cmd == "index-history":
        kwargs: dict = {}
        if args.index_code:
            kwargs["index_codes"] = [args.index_code]

        if args.resume:
            runner.run(
                IndexHistoryTask,
                mode="resume",
                session_id=args.resume,
                **kwargs,
            )
        elif args.incremental:
            runner.run(IndexHistoryTask, mode="incremental", **kwargs)
        else:
            runner.run(IndexHistoryTask, mode="full", **kwargs)

    # ------------------------------------------------------------------
    # etf
    # ------------------------------------------------------------------
    elif cmd == "etf":
        runner.run(EtfBasicTask, mode="full")

    # ------------------------------------------------------------------
    # quote
    # ------------------------------------------------------------------
    elif cmd == "quote":
        kwargs: dict = {}
        if args.date:
            kwargs["trade_date"] = args.date

        if args.stock_code:
            runner.run(QuoteTask, mode="partial", identifiers=[args.stock_code], **kwargs)
        else:
            runner.run(QuoteTask, mode="full", **kwargs)

    # ------------------------------------------------------------------
    # schedule
    # ------------------------------------------------------------------
    elif cmd == "schedule":
        run_scheduler_mode(args, runner)

    else:
        raise ValueError(f"Unknown command: {cmd}")


def run_scheduler_mode(args, runner: TaskRunner):
    """启动调度器模式，根据参数注册定时任务。"""
    from collector.config import CollectorConfig

    cfg = CollectorConfig.from_env()
    scheduler = Scheduler(db_cfg=cfg.db)

    if args.company:
        scheduler.register(
            "company_task", "Full Company Sync",
            args.company,
            lambda: runner.run(CompanyTask, mode="full"),
        )
    if args.finance:
        scheduler.register(
            "finance_task", "Full Finance Sync",
            args.finance,
            lambda: runner.run(FinanceTask, mode="full"),
        )
    if args.industry:
        scheduler.register(
            "industry_sync_task", "Industry Classification Sync",
            args.industry,
            lambda: runner.run(IndustryTask, mode="full"),
        )
    if args.index_basic:
        scheduler.register(
            "index_basic_task", "Index Basic Sync",
            args.index_basic,
            lambda: runner.run(IndexBasicTask, mode="full"),
        )
    if args.index_history:
        scheduler.register(
            "index_history_task", "Index History Sync",
            args.index_history,
            lambda: runner.run(IndexHistoryTask, mode="full"),
        )
    if args.etf:
        scheduler.register(
            "etf_basic_task", "ETF Basic Sync",
            args.etf,
            lambda: runner.run(EtfBasicTask, mode="full"),
        )
    if args.quote:
        scheduler.register(
            "quote_task", "Daily Quote Sync",
            args.quote,
            lambda: runner.run(QuoteTask, mode="full"),
        )

    scheduler.start()
    logger.info("Scheduler started. Press Ctrl+C to exit.")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down collector...")
        scheduler.stop()


def main():
    parser = build_parser()
    args = parser.parse_args()

    runner = TaskRunner()
    try:
        _dispatch_command(args, runner)
    finally:
        runner.close()


if __name__ == "__main__":
    main()
