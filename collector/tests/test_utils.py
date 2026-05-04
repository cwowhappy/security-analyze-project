import pytest
from collector.utils import infer_market, parse_date, parse_capital, extract_region


class TestInferMarket:
    def test_sh_6(self):
        assert infer_market("600519") == "SH"

    def test_sh_9(self):
        assert infer_market("900901") == "SH"

    def test_sz_0(self):
        assert infer_market("000001") == "SZ"

    def test_sz_2(self):
        assert infer_market("002594") == "SZ"

    def test_sz_3(self):
        assert infer_market("300750") == "SZ"

    def test_bj_4(self):
        assert infer_market("430047") == "BJ"

    def test_bj_8(self):
        assert infer_market("835305") == "BJ"

    def test_hk_5(self):
        assert infer_market("00700") == "HK"

    def test_empty(self):
        assert infer_market("") is None

    def test_none(self):
        assert infer_market(None) is None


class TestParseDate:
    def test_valid(self):
        assert parse_date("2024-05-01") == "2024-05-01"

    def test_invalid(self):
        assert parse_date("not-a-date") is None

    def test_none(self):
        assert parse_date(None) is None


class TestParseCapital:
    def test_plain_number(self):
        assert parse_capital("12345.67") == 12345.67

    def test_with_comma(self):
        assert parse_capital("12,345.67") == 12345.67

    def test_with_unit(self):
        assert parse_capital("注册资金10000万元") == 10000.0

    def test_none(self):
        assert parse_capital(None) is None


class TestExtractRegion:
    def test_province(self):
        assert extract_region("广东省深圳市南山区") == "广东省"

    def test_municipality(self):
        assert extract_region("北京市海淀区") == "北京"

    def test_autonomous_region(self):
        assert extract_region("广西壮族自治区南宁市") == "广西壮族自治区"

    def test_none(self):
        assert extract_region(None) is None

    def test_no_match(self):
        assert extract_region("Unknown Address") is None
