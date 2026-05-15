"""公司信息 Tushare 适配器。

Tushare stock_company 接口按 ts_code 查询，返回单条公司信息。
"""

from typing import Any

import structlog

from data_collector.adapters.stock_basic_tushare_adapter import _init_tushare
from data_collector.core.config.field_mapping_config import SourceConfig
from data_collector.scripts.stock_full import _to_ts_code

logger = structlog.get_logger(__name__)


def _guess_ts_code(stock_code: str) -> str | None:
    """根据股票代码推断 ts_code（如 000001 → 000001.SZ）。"""
    return _to_ts_code(stock_code, None)


class CompanyInfoTushareAdapter:
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        pro = _init_tushare()
        ts_code = _guess_ts_code(stock_code)
        if not ts_code:
            logger.warning("无法推断 ts_code", stock_code=stock_code)
            return {}

        try:
            df = pro.stock_company(
                ts_code=ts_code,
                fields="ts_code,exchange,chairman,manager,secretary,reg_capital,setup_date,employees,main_business",
            )
            if df is None or df.empty:
                return {}
            return df.iloc[0].to_dict()
        except Exception as e:
            logger.warning(
                "tushare stock_company 查询失败",
                stock_code=stock_code,
                ts_code=ts_code,
                error=str(e),
            )
            return {}
