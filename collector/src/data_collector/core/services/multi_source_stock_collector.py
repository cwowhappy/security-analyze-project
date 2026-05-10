"""多数据源股票信息采集服务。

实现 akshare + tushare 双源互补采集：
- akshare 提供股票代码骨架（免费、稳定）
- tushare 补充 industry/area/list_date/exchange/full_name 等字段
"""

import structlog

from data_collector.core.domain.data_source_error import DataSourceError
from data_collector.core.domain.stock import Stock
from data_collector.core.ports.data_source import DataSource

logger = structlog.get_logger(__name__)


class MultiSourceStockCollector:
    """多数据源股票列表采集器。

    先通过主数据源获取股票代码骨架列表，再通过补充数据源批量获取详情字段，
    以 stock_code 为关联键进行合并，最终返回字段更完整的 Stock 列表。
    """

    def __init__(self, primary_source: DataSource, supplement_source: DataSource) -> None:
        """
        Args:
            primary_source: 主数据源，负责提供股票代码骨架（推荐 akshare）。
            supplement_source: 补充数据源，负责提供丰富字段（推荐 tushare）。
        """
        self._primary = primary_source
        self._supplement = supplement_source

    def fetch_merged_stock_list(self) -> list[Stock]:
        """执行双源合并采集。

        流程：
        1. 调用主数据源获取全量股票骨架列表（code, name, market, ts_code）。
        2. 若补充数据源可用，调用其批量接口获取详情（industry, area, list_date 等）。
        3. 以 stock_code 为 key 合并，补充字段优先使用详情源数据。
        4. 若补充数据源不可用或失败，仅返回骨架数据。

        Returns:
            合并后的 Stock 领域实体列表。
        """
        logger.info(
            "开始双源合并采集股票列表",
            primary=self._primary.name,
            supplement=self._supplement.name,
        )

        # Step 1: 获取骨架数据
        skeleton_stocks = self._primary.fetch_stock_list()
        if not skeleton_stocks:
            logger.warning("主数据源返回空列表", primary=self._primary.name)
            return []

        logger.info("骨架数据获取完成", primary=self._primary.name, count=len(skeleton_stocks))

        # Step 2: 尝试获取补充详情
        detail_map: dict[str, Stock] = {}
        try:
            if self._supplement.is_available():
                detail_stocks = self._supplement.fetch_stock_list()
                detail_map = {s.stock_code: s for s in detail_stocks if s.stock_code}
                logger.info(
                    "补充详情获取完成",
                    supplement=self._supplement.name,
                    count=len(detail_map),
                )
            else:
                logger.warning(
                    "补充数据源不可用，仅使用骨架数据",
                    supplement=self._supplement.name,
                )
        except DataSourceError as e:
            logger.warning(
                "补充数据源请求失败，仅使用骨架数据",
                supplement=self._supplement.name,
                error=str(e),
            )

        # Step 3: 合并
        merged: list[Stock] = []
        matched_count = 0
        for sk in skeleton_stocks:
            detail = detail_map.get(sk.stock_code)
            if detail:
                merged.append(self._merge_stock(sk, detail))
                matched_count += 1
            else:
                merged.append(sk)

        logger.info(
            "双源合并完成",
            total=len(merged),
            matched=matched_count,
            unmatched=len(merged) - matched_count,
        )
        return merged

    @staticmethod
    def _merge_stock(skeleton: Stock, detail: Stock) -> Stock:
        """合并骨架与详情，生成字段更完整的 Stock 实体。

        合并策略：
        - stock_code / name / ts_code / market：以骨架数据（akshare）为准。
        - full_name：以详情数据（tushare fullname）为准，骨架的 full_name 通常与 name 相同。
        - exchange / list_date / industry / area：骨架无此字段，以详情为准。
        - total_shares / float_shares：两源批量接口均无，保持原值。
        """
        return Stock(
            stock_code=skeleton.stock_code,
            name=skeleton.name,
            id=skeleton.id,
            ts_code=skeleton.ts_code or detail.ts_code,
            full_name=detail.full_name or skeleton.full_name,
            market=skeleton.market or detail.market,
            exchange=detail.exchange or skeleton.exchange,
            list_date=detail.list_date,
            industry=detail.industry,
            area=detail.area,
            total_shares=skeleton.total_shares or detail.total_shares,
            float_shares=skeleton.float_shares or detail.float_shares,
            created_at=skeleton.created_at,
            updated_at=skeleton.updated_at,
        )
