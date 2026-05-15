"""采集脚本纯函数单元测试。"""

from datetime import date, datetime
from decimal import Decimal

import pytest

from data_collector.scripts.company_full import _parse_capital, _parse_date as _parse_date_company
from data_collector.scripts.stock_full import _parse_date as _parse_date_stock, _to_exchange, _to_ts_code
from data_collector.scripts.field_supplement import _init_tushare, _parse_int, _parse_date as _parse_date_field


class TestParseDate:
    """日期解析函数测试。"""

    def test_parse_date_company_valid(self):
        assert _parse_date_company("2021-06-15") == date(2021, 6, 15)

    def test_parse_date_company_none(self):
        assert _parse_date_company(None) is None

    def test_parse_date_company_empty(self):
        assert _parse_date_company("") is None

    def test_parse_date_company_invalid(self):
        assert _parse_date_company("not-a-date") is None

    def test_parse_date_stock_valid(self):
        assert _parse_date_stock("2021-06-15") == date(2021, 6, 15)

    def test_parse_date_stock_none(self):
        assert _parse_date_stock(None) is None

    def test_parse_date_stock_invalid(self):
        assert _parse_date_stock("bad-date") is None

    def test_parse_date_field_valid(self):
        assert _parse_date_field("2021-06-15") == date(2021, 6, 15)

    def test_parse_date_field_YYYYMMDD(self):
        assert _parse_date_field("20210615") == date(2021, 6, 15)

    def test_parse_date_field_none(self):
        assert _parse_date_field(None) is None


class TestParseCapital:
    """注册资本解析函数测试。"""

    def test_parse_capital_with_wan(self):
        result = _parse_capital("5000万")
        assert result == Decimal("5000")

    def test_parse_capital_with_yi(self):
        result = _parse_capital("1.5亿")
        assert result == Decimal("1.5")

    def test_parse_capital_with_comma(self):
        result = _parse_capital("1,000万")
        assert result == Decimal("1000")

    def test_parse_capital_none(self):
        assert _parse_capital(None) is None

    def test_parse_capital_empty(self):
        assert _parse_capital("") is None

    def test_parse_capital_invalid(self):
        assert _parse_capital("未知") is None


class TestToTsCode:
    """ts_code 转换函数测试。"""

    def test_sh_sse(self):
        assert _to_ts_code("600001", "SH") == "600001.SH"

    def test_sz_szse(self):
        assert _to_ts_code("000001", "SZ") == "000001.SZ"

    def test_bj_bse(self):
        assert _to_ts_code("430001", "BJ") == "430001.BJ"

    def test_infer_sh(self):
        assert _to_ts_code("600001", None) == "600001.SH"

    def test_infer_sz_0(self):
        assert _to_ts_code("000001", None) == "000001.SZ"

    def test_infer_sz_3(self):
        assert _to_ts_code("300001", None) == "300001.SZ"

    def test_infer_bj_4(self):
        assert _to_ts_code("430001", None) == "430001.BJ"

    def test_infer_bj_8(self):
        assert _to_ts_code("830001", None) == "830001.BJ"

    def test_infer_bj_82(self):
        assert _to_ts_code("920001", None) == "920001.BJ"

    def test_infer_bj_88(self):
        assert _to_ts_code("880001", None) == "880001.BJ"

    def test_empty_code(self):
        assert _to_ts_code("", None) is None

    def test_unknown_prefix(self):
        assert _to_ts_code("900001", None) is None


class TestToExchange:
    """交易所代码推断函数测试。"""

    def test_sh_sse(self):
        assert _to_exchange("600001", "SH") == "SSE"

    def test_sz_szse(self):
        assert _to_exchange("000001", "SZ") == "SZSE"

    def test_bj_bse(self):
        assert _to_exchange("430001", "BJ") == "BSE"

    def test_infer_sse(self):
        assert _to_exchange("600001", None) == "SSE"

    def test_infer_szse(self):
        assert _to_exchange("000001", None) == "SZSE"

    def test_infer_bse(self):
        assert _to_exchange("430001", None) == "BSE"

    def test_infer_bse_92(self):
        assert _to_exchange("920001", None) == "BSE"

    def test_unknown_prefix(self):
        assert _to_exchange("900001", None) is None


class TestParseInt:
    """整数解析函数测试。"""

    def test_parse_int_valid(self):
        assert _parse_int("100") == 100

    def test_parse_int_float_string(self):
        assert _parse_int("100.0") == 100

    def test_parse_int_none(self):
        assert _parse_int(None) is None

    def test_parse_int_invalid(self):
        assert _parse_int("abc") is None


class TestInitTushare:
    """Tushare 初始化函数测试。"""

    def test_init_tushare_no_token(self, monkeypatch):
        """无 Token 时应返回 None。"""
        from data_collector.config import Settings

        settings = Settings()
        monkeypatch.setattr(settings, "tushare_token", None)
        assert _init_tushare(settings) is None
