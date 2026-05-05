import logging
import re
import time
from typing import List, Dict, Any, Optional

import pandas as pd

from collector.utils import infer_market
from collector.decorators import retry

logger = logging.getLogger(__name__)

# DataFrame 关键列名常量
COL_REPORT_DATE = "REPORT_DATE"
COL_REPORT_TYPE = "REPORT_TYPE"
COL_NOTICE_DATE = "NOTICE_DATE"
COL_CURRENCY = "CURRENCY"


class AkshareSource:
    """akshare 数据源封装，预留多数据源扩展接口"""

    def __init__(self, max_retries: int = 3, retry_delay: float = 2.0, retry_backoff: float = 2.0):
        try:
            import akshare as ak
            self._ak = ak
            self._stock_list: List[Dict[str, Any]] = []
            self._max_retries = max_retries
            self._retry_delay = retry_delay
            self._retry_backoff = retry_backoff
            logger.info("Akshare source initialized")
        except ImportError:
            logger.error("akshare not installed")
            raise

    # ------------------------------------------------------------------
    # 公司信息
    # ------------------------------------------------------------------
    def get_stock_list(self) -> List[Dict[str, Any]]:
        """获取 A 股上市公司列表"""
        df = self._ak.stock_info_a_code_name()
        self._stock_list = df.to_dict(orient="records")
        return self._stock_list

    @retry(max_retries=3, delay=2.0, backoff=2.0, exceptions=(Exception,))
    def get_company_detail(self, stock_code: str) -> Optional[Dict[str, Any]]:
        """获取单公司详细信息（使用 stock_profile_cninfo，字段更完整）"""
        try:
            df = self._ak.stock_profile_cninfo(symbol=stock_code)
            if df.empty:
                logger.warning(f"Empty profile for {stock_code}")
                return None
            return df.iloc[0].to_dict()
        except Exception as e:
            logger.debug(f"Failed to get company profile for {stock_code}: {e}")
            return None

    @retry(max_retries=3, delay=2.0, backoff=2.0, exceptions=(Exception,))
    def get_company_info_em(self, stock_code: str) -> Optional[Dict[str, Any]]:
        """备用：使用 stock_individual_info_em 获取基本信息"""
        try:
            df = self._ak.stock_individual_info_em(symbol=stock_code)
            info = dict(zip(df["item"].tolist(), df["value"].tolist()))
            return info
        except Exception as e:
            logger.debug(f"Failed to get company info EM for {stock_code}: {e}")
            return None

    # ------------------------------------------------------------------
    # 财务报表
    # ------------------------------------------------------------------
    @retry(max_retries=3, delay=2.0, backoff=2.0, exceptions=(Exception,))
    def get_balance_sheet(
        self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None
    ) -> Optional[pd.DataFrame]:
        """获取资产负债表（东方财富，按报告期）"""
        try:
            df = self._ak.stock_balance_sheet_by_report_em(symbol=symbol)
            if df is None or df.empty:
                logger.warning(f"Empty balance sheet for {symbol}")
                return None
            return self._filter_by_year(df, start_year, end_year)
        except Exception as e:
            logger.debug(f"Failed to get balance sheet for {symbol}: {e}")
            return None

    @retry(max_retries=3, delay=2.0, backoff=2.0, exceptions=(Exception,))
    def get_profit_sheet(
        self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None
    ) -> Optional[pd.DataFrame]:
        """获取利润表（东方财富，按报告期）"""
        try:
            df = self._ak.stock_profit_sheet_by_report_em(symbol=symbol)
            if df is None or df.empty:
                logger.warning(f"Empty profit sheet for {symbol}")
                return None
            return self._filter_by_year(df, start_year, end_year)
        except Exception as e:
            logger.debug(f"Failed to get profit sheet for {symbol}: {e}")
            return None

    @retry(max_retries=3, delay=2.0, backoff=2.0, exceptions=(Exception,))
    def get_cash_flow_sheet(
        self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None
    ) -> Optional[pd.DataFrame]:
        """获取现金流量表（东方财富，按报告期）"""
        try:
            df = self._ak.stock_cash_flow_sheet_by_report_em(symbol=symbol)
            if df is None or df.empty:
                logger.warning(f"Empty cash flow sheet for {symbol}")
                return None
            return self._filter_by_year(df, start_year, end_year)
        except Exception as e:
            logger.debug(f"Failed to get cash flow sheet for {symbol}: {e}")
            return None

    # ------------------------------------------------------------------
    # 搜索
    # ------------------------------------------------------------------
    def search_by_name(self, query: str) -> List[Dict[str, Any]]:
        """根据公司名称或股票代码搜索匹配的上市公司"""
        query = query.strip()
        if not query:
            return []

        if re.match(r"^\d{6}$", query):
            return [{"code": query, "name": ""}]

        if not self._stock_list:
            self.get_stock_list()

        results = []
        query_lower = query.lower()

        for item in self._stock_list:
            code = item.get("code", "")
            name = item.get("name", "")

            if query == code:
                results.insert(0, item)
                continue

            if query_lower in name.lower():
                results.append(item)

        return results

    # ------------------------------------------------------------------
    # 静态工具
    # ------------------------------------------------------------------
    @staticmethod
    def _filter_by_year(
        df: pd.DataFrame, start_year: Optional[int], end_year: Optional[int]
    ) -> pd.DataFrame:
        """根据 REPORT_DATE 的年份过滤 DataFrame"""
        if df is None or df.empty:
            return df
        if start_year is None and end_year is None:
            return df
        if COL_REPORT_DATE not in df.columns:
            return df

        def _extract_year(val):
            if val is None or pd.isna(val):
                return None
            try:
                s = str(val).strip()
                if " " in s:
                    s = s.split(" ")[0]
                return int(s[:4])
            except (ValueError, TypeError):
                return None

        years = df[COL_REPORT_DATE].apply(_extract_year)
        mask = pd.Series(True, index=df.index)
        if start_year is not None:
            mask = mask & (years >= start_year)
        if end_year is not None:
            mask = mask & (years <= end_year)
        return df[mask].copy()

    @staticmethod
    def infer_market(stock_code: str) -> str:
        """根据股票代码推断市场板块前缀（用于 akshare 接口）"""
        return infer_market(stock_code) or "SH"
