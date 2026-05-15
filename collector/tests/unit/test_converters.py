import datetime

import pytest

from data_collector.core.pipeline.converters import convert, register_converter


class TestConverters:
    def test_str_converter(self):
        assert convert("str", 123) == "123"

    def test_int_converter(self):
        assert convert("int", "42") == 42

    def test_float_converter(self):
        assert convert("float", "3.14") == 3.14

    def test_date_converter(self):
        assert convert("date", "20230101") == datetime.date(2023, 1, 1)

    def test_shares_10k_converter(self):
        assert convert("shares_10k", "10000") == 100000000

    def test_unknown_converter_raises(self):
        with pytest.raises(ValueError, match="未知的转换器"):
            convert("unknown", "x")

    def test_custom_converter(self):
        register_converter("double", lambda x: float(x) * 2)
        assert convert("double", "5") == 10.0

    def test_date_hyphen_converter(self):
        assert convert("date_hyphen", "2023-01-01") == datetime.date(2023, 1, 1)
        assert convert("date_hyphen", None) is None

    def test_capital_converter(self):
        assert convert("capital", "12,345.67万") == 12345.67
        assert convert("capital", "1.5亿") == 1.5
        assert convert("capital", "1000") == 1000.0
        assert convert("capital", None) is None
        assert convert("capital", "") is None
