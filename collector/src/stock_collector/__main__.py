"""CLI 入口。"""

import os

from stock_collector.adapters.memory_stock_repository import MemoryStockRepository
from stock_collector.core.use_cases.collect_stock import CollectStockUseCase
from stock_collector.infrastructure.logging.config import configure_logging


def main() -> None:
    """主函数。"""
    log_level = os.environ.get("LOG_LEVEL", "INFO")
    json_format = os.environ.get("LOG_FORMAT", "text").lower() == "json"
    configure_logging(log_level=log_level, json_format=json_format)

    repository = MemoryStockRepository()
    use_case = CollectStockUseCase(repository)

    use_case.execute(
        symbol="000001",
        name="平安银行",
        market="SZ",
    )

    print(f"已采集 {len(repository.find_all())} 只股票")


if __name__ == "__main__":
    main()
