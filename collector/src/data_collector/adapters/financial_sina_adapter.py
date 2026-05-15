"""新浪财经财务报告共享适配器。

支持利润表、资产负债表、现金流量表，通过 params 中的 symbol 区分。
"""

from typing import Any

import akshare as ak

from data_collector.core.config.field_mapping_config import SourceConfig


class FinancialSinaAdapter:
    def fetch(self, stock_code: str, source_config: SourceConfig) -> list[dict[str, Any]]:
        """调用 AKShare 新浪财经财务报告接口，返回多期报告数据列表。"""
        symbol = source_config.params.get("symbol")
        if not symbol:
            raise ValueError("source_config.params 必须包含 symbol")

        df = ak.stock_financial_report_sina(stock=stock_code, symbol=symbol)
        if df is None or df.empty:
            return []

        return [row.to_dict() for _, row in df.iterrows()]
