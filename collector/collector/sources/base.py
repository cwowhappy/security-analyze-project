"""数据源抽象接口，所有具体数据源须实现此接口。"""
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional

import pandas as pd


class BaseDataSource(ABC):
    """数据采集源抽象接口。"""

    @abstractmethod
    def get_stock_list(self) -> List[Dict[str, Any]]:
        """获取 A 股上市公司列表"""
        ...

    @abstractmethod
    def get_company_detail(self, stock_code: str) -> Optional[Dict[str, Any]]:
        """获取单公司详细信息"""
        ...

    @abstractmethod
    def get_stock_daily_quote(
        self, stock_code: str, start_date: str, end_date: str
    ) -> Optional[pd.DataFrame]:
        """获取 A 股日行情数据"""
        ...

    @abstractmethod
    def get_balance_sheet(
        self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None
    ) -> Optional[pd.DataFrame]:
        """获取资产负债表"""
        ...

    @abstractmethod
    def get_profit_sheet(
        self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None
    ) -> Optional[pd.DataFrame]:
        """获取利润表"""
        ...

    @abstractmethod
    def get_cash_flow_sheet(
        self, symbol: str, start_year: Optional[int] = None, end_year: Optional[int] = None
    ) -> Optional[pd.DataFrame]:
        """获取现金流量表"""
        ...

    @abstractmethod
    def get_index_list(self) -> List[Dict[str, Any]]:
        """获取 A 股指数列表"""
        ...

    @abstractmethod
    def get_index_history(
        self,
        symbol: str,
        period: str,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
    ) -> Optional[pd.DataFrame]:
        """获取指数历史行情"""
        ...

    @abstractmethod
    def get_etf_spot_list(self) -> List[Dict[str, Any]]:
        """获取 ETF 实时行情列表"""
        ...

    @abstractmethod
    def search_by_name(self, query: str) -> List[Dict[str, Any]]:
        """根据公司名称或股票代码搜索"""
        ...
