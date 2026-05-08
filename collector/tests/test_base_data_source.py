"""Test BaseDataSource abstract interface."""
from abc import ABC
from unittest.mock import MagicMock
import pytest
import pandas as pd

from collector.sources.base import BaseDataSource


class TestBaseDataSource:
    def test_is_abstract_class(self):
        """BaseDataSource 应为抽象类，不能直接实例化"""
        with pytest.raises(TypeError):
            BaseDataSource()

    def test_subclass_must_implement_all_methods(self):
        """子类必须实现所有抽象方法"""

        class PartialSource(BaseDataSource):
            def get_stock_list(self):
                return []

        with pytest.raises(TypeError):
            PartialSource()

    def test_full_implementation_can_instantiate(self):
        """完整实现所有抽象方法后可以实例化"""

        class FullSource(BaseDataSource):
            def get_stock_list(self):
                return []

            def get_company_detail(self, stock_code):
                return None

            def get_stock_daily_quote(self, stock_code, start_date, end_date):
                return None

            def get_balance_sheet(self, symbol, start_year=None, end_year=None):
                return None

            def get_profit_sheet(self, symbol, start_year=None, end_year=None):
                return None

            def get_cash_flow_sheet(self, symbol, start_year=None, end_year=None):
                return None

            def get_index_list(self):
                return []

            def get_index_history(self, symbol, period, start_date=None, end_date=None):
                return None

            def get_etf_spot_list(self):
                return []

            def search_by_name(self, query):
                return []

        source = FullSource()
        assert isinstance(source, BaseDataSource)
        assert isinstance(source, ABC)

    def test_akshare_source_is_subclass(self):
        """AkshareSource 应实现 BaseDataSource"""
        from collector.sources.akshare_source import AkshareSource
        assert issubclass(AkshareSource, BaseDataSource)
