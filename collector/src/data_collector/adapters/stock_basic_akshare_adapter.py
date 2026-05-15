"""股票基础信息 AkShare 适配器。"""

from typing import Any

import akshare as ak

from data_collector.core.config.field_mapping_config import SourceConfig


class StockBasicAkshareAdapter:
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        api_name = source_config.params.get("api_name", "stock_info_a_code_name")
        df = getattr(ak, api_name)()
        row = df[df["代码"] == stock_code]
        if row.empty:
            raise ValueError(f"未找到股票: {stock_code}")
        return row.iloc[0].to_dict()
