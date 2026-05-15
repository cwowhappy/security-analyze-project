"""公司信息 AkShare 适配器。"""

from typing import Any

import akshare as ak

from data_collector.core.config.field_mapping_config import SourceConfig


class CompanyInfoAkshareAdapter:
    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        df = ak.stock_profile_cninfo(symbol=stock_code)
        if df is None or df.empty:
            raise ValueError(f"未找到公司信息: {stock_code}")
        return df.iloc[0].to_dict()
