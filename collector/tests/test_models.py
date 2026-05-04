import pytest
from collector.models import CompanyEntity, SecurityEntity, FinancialReport


class TestCompanyEntity:
    def test_basic(self):
        c = CompanyEntity(company_name="贵州茅台", short_name="茅台", industry="酿酒")
        assert c.company_name == "贵州茅台"
        assert c.industry == "酿酒"

    def test_to_insert_tuple(self):
        c = CompanyEntity(
            company_name="Test Co",
            short_name="Test",
            industry="IT",
            region="北京",
            establish_date="2000-01-01",
            registered_capital=10000.0,
        )
        t = c.to_insert_tuple()
        assert len(t) == 6
        assert t[0] == "Test Co"
        assert t[4] == "2000-01-01"
        assert t[5] == 10000.0

    def test_to_update_tuple(self):
        c = CompanyEntity(company_name="Test Co", short_name="Test")
        t = c.to_update_tuple()
        assert len(t) == 5
        assert t[0] == "Test"


class TestSecurityEntity:
    def test_basic(self):
        s = SecurityEntity(stock_code="600519", stock_name="贵州茅台", market="SH")
        assert s.stock_code == "600519"
        assert s.security_type == "A股"

    def test_to_insert_tuple(self):
        s = SecurityEntity(
            stock_code="600519",
            stock_name="贵州茅台",
            market="SH",
            listing_date="2001-08-27",
        )
        t = s.to_insert_tuple(company_id=42)
        assert len(t) == 7
        assert t[0] == 42
        assert t[1] == "600519"
        assert t[6] == "listed"

    def test_to_update_tuple(self):
        s = SecurityEntity(stock_code="600519", stock_name="茅台")
        t = s.to_update_tuple(company_id=99)
        assert len(t) == 7
        assert t[0] == 99
        assert t[-1] == "600519"


class TestFinancialReport:
    def test_basic(self):
        r = FinancialReport(
            stock_code="600519",
            report_date="2024-03-31",
            report_type="一季报",
            report_year=2024,
        )
        assert r.stock_code == "600519"
        assert r.currency == "CNY"

    def test_to_insert_tuple(self):
        r = FinancialReport(
            stock_code="600519",
            report_date="2024-03-31",
            report_type="一季报",
            report_year=2024,
            total_assets=100.5,
            balance_sheet={"TOTAL_ASSETS": 100.5},
        )
        t = r.to_insert_tuple()
        assert len(t) == 35
        assert t[0] == "600519"
        assert t[5] == "CNY"
        assert t[6] == 100.5
        # JSONB 字段已被序列化为字符串
        assert isinstance(t[-3], str)

    def test_insert_sql_present(self):
        sql = FinancialReport.insert_sql()
        assert "INSERT INTO financial_report" in sql

    def test_upsert_sql_present(self):
        sql = FinancialReport.upsert_sql()
        assert "INSERT INTO financial_report" in sql
        assert "ON CONFLICT" in sql

    def test_validation_error(self):
        with pytest.raises(Exception):
            FinancialReport(stock_code="600519", report_date=None)  # report_date 为必填 str
