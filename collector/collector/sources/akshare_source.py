import logging
from typing import List, Dict, Any

logger = logging.getLogger(__name__)


class AkshareSource:
    """akshare 数据源封装，预留多数据源扩展接口"""

    def __init__(self):
        try:
            import akshare as ak
            self._ak = ak
            logger.info("Akshare source initialized")
        except ImportError:
            logger.error("akshare not installed")
            raise

    def get_stock_list(self) -> List[Dict[str, Any]]:
        """获取 A 股上市公司列表"""
        df = self._ak.stock_info_a_code_name()
        return df.to_dict(orient="records")

    def get_financial_report(self, stock_code: str) -> List[Dict[str, Any]]:
        """获取个股财务报告，预留接口"""
        raise NotImplementedError("TODO: implement financial report fetch")
