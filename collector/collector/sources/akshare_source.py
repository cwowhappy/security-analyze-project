import logging
import re
import time
from typing import List, Dict, Any, Optional

import pandas as pd

logger = logging.getLogger(__name__)


class AkshareSource:
    """akshare 数据源封装，预留多数据源扩展接口"""

    def __init__(self):
        try:
            import akshare as ak
            self._ak = ak
            self._stock_list: List[Dict[str, Any]] = []
            logger.info("Akshare source initialized")
        except ImportError:
            logger.error("akshare not installed")
            raise

    def get_stock_list(self) -> List[Dict[str, Any]]:
        """获取 A 股上市公司列表"""
        df = self._ak.stock_info_a_code_name()
        self._stock_list = df.to_dict(orient="records")
        return self._stock_list

    def get_company_detail(self, stock_code: str) -> Optional[Dict[str, Any]]:
        """获取单公司详细信息（使用 stock_profile_cninfo，字段更完整）"""
        try:
            df = self._ak.stock_profile_cninfo(symbol=stock_code)
            if df.empty:
                logger.warning(f"Empty profile for {stock_code}")
                return None
            # 返回第一行数据作为字典
            return df.iloc[0].to_dict()
        except Exception as e:
            logger.warning(f"Failed to get company profile for {stock_code}: {e}")
            return None

    def get_company_info_em(self, stock_code: str) -> Optional[Dict[str, Any]]:
        """备用：使用 stock_individual_info_em 获取基本信息"""
        try:
            df = self._ak.stock_individual_info_em(symbol=stock_code)
            info = dict(zip(df['item'].tolist(), df['value'].tolist()))
            return info
        except Exception as e:
            logger.warning(f"Failed to get company info EM for {stock_code}: {e}")
            return None

    def get_balance_sheet(self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None) -> Optional[pd.DataFrame]:
        """获取资产负债表（东方财富，按报告期）"""
        try:
            df = self._ak.stock_balance_sheet_by_report_em(symbol=symbol)
            if df is None or df.empty:
                logger.warning(f"Empty balance sheet for {symbol}")
                return None
            return self._filter_by_year(df, start_year, end_year)
        except Exception as e:
            logger.warning(f"Failed to get balance sheet for {symbol}: {e}")
            return None

    def get_profit_sheet(self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None) -> Optional[pd.DataFrame]:
        """获取利润表（东方财富，按报告期）"""
        try:
            df = self._ak.stock_profit_sheet_by_report_em(symbol=symbol)
            if df is None or df.empty:
                logger.warning(f"Empty profit sheet for {symbol}")
                return None
            return self._filter_by_year(df, start_year, end_year)
        except Exception as e:
            logger.warning(f"Failed to get profit sheet for {symbol}: {e}")
            return None

    def get_cash_flow_sheet(self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None) -> Optional[pd.DataFrame]:
        """获取现金流量表（东方财富，按报告期）"""
        try:
            df = self._ak.stock_cash_flow_sheet_by_report_em(symbol=symbol)
            if df is None or df.empty:
                logger.warning(f"Empty cash flow sheet for {symbol}")
                return None
            return self._filter_by_year(df, start_year, end_year)
        except Exception as e:
            logger.warning(f"Failed to get cash flow sheet for {symbol}: {e}")
            return None

    def search_by_name(self, query: str) -> List[Dict[str, Any]]:
        """根据公司名称或股票代码搜索匹配的上市公司

        Args:
            query: 用户输入的关键词，可以是股票代码或公司名称（支持模糊匹配）

        Returns:
            匹配的公司列表，每个元素包含 code 和 name
        """
        query = query.strip()
        if not query:
            return []

        # 如果是纯数字且为 6 位，直接当作股票代码返回，无需拉取全量列表
        if re.match(r"^\d{6}$", query):
            return [{"code": query, "name": ""}]

        if not self._stock_list:
            self.get_stock_list()

        results = []
        query_lower = query.lower()

        for item in self._stock_list:
            code = item.get("code", "")
            name = item.get("name", "")

            # 精确匹配股票代码
            if query == code:
                results.insert(0, item)  # 精确匹配放最前面
                continue

            # 模糊匹配公司名称（包含子串即可）
            if query_lower in name.lower():
                results.append(item)

        return results

    @staticmethod
    def _filter_by_year(df: pd.DataFrame, start_year: Optional[int], end_year: Optional[int]) -> pd.DataFrame:
        """根据 REPORT_DATE 的年份过滤 DataFrame"""
        if df is None or df.empty:
            return df
        if start_year is None and end_year is None:
            return df
        if "REPORT_DATE" not in df.columns:
            return df

        # 提取年份：兼容 '2024-12-31' 和 '2024-12-31 00:00:00'
        def _extract_year(val):
            if val is None or pd.isna(val):
                return None
            try:
                s = str(val).strip()
                if ' ' in s:
                    s = s.split(' ')[0]
                return int(s[:4])
            except (ValueError, TypeError):
                return None

        years = df["REPORT_DATE"].apply(_extract_year)
        mask = pd.Series(True, index=df.index)
        if start_year is not None:
            mask = mask & (years >= start_year)
        if end_year is not None:
            mask = mask & (years <= end_year)
        return df[mask].copy()

    @staticmethod
    def infer_market(stock_code: str) -> str:
        """根据股票代码推断市场板块前缀（用于 akshare 接口）"""
        if not stock_code:
            return "SH"
        code = str(stock_code).strip()
        if len(code) != 6:
            return "SH"
        first = code[0]
        if first in ("6", "9"):
            return "SH"
        elif first in ("0", "2", "3"):
            return "SZ"
        elif first in ("4", "8"):
            return "BJ"
        return "SH"
