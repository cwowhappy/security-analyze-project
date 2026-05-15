"""PostgreSQL 公司仓库单元测试。"""

from unittest.mock import MagicMock, patch

from data_collector.adapters.db_company_repository import DbCompanyRepository
from data_collector.core.domain.company import Company


class TestDbCompanyRepository:
    """DbCompanyRepository 测试。"""

    def setup_method(self) -> None:
        self.repo = DbCompanyRepository()

    def test_should_save_company(self) -> None:
        with patch("data_collector.adapters.db_company_repository.execute_update") as mock_update:
            mock_update.return_value = 1
            company = Company(name="平安银行股份有限公司")
            self.repo.save(company)
            assert company.id is not None
            mock_update.assert_called_once()
            sql = mock_update.call_args[0][0]
            assert "INSERT INTO tb_company_basic" in sql
            assert "ON CONFLICT (unified_social_credit_code)" in sql

    def test_should_save_all_companies(self) -> None:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value = mock_cursor

        with patch("data_collector.adapters.db_company_repository.transaction") as mock_tx:
            mock_tx.return_value.__enter__.return_value = mock_conn
            companies = [
                Company(name="平安银行股份有限公司"),
                Company(name="万科企业股份有限公司"),
            ]
            success, failed = self.repo.save_all(companies)
            assert success == 2
            assert failed == 0
            assert mock_cursor.execute.call_count == 2

    def test_should_find_by_usc_code(self) -> None:
        with patch("data_collector.adapters.db_company_repository.execute_query") as mock_query:
            mock_query.return_value = [{
                "id": "test-id",
                "name": "平安银行股份有限公司",
                "unified_social_credit_code": "9144030019218537XX",
            }]
            result = self.repo.find_by_usc_code("9144030019218537XX")
            assert result is not None
            assert result.name == "平安银行股份有限公司"

    def test_should_return_none_when_usc_not_found(self) -> None:
        with patch("data_collector.adapters.db_company_repository.execute_query") as mock_query:
            mock_query.return_value = []
            result = self.repo.find_by_usc_code("NOTEXIST")
            assert result is None

    def test_should_find_all(self) -> None:
        with patch("data_collector.adapters.db_company_repository.execute_query") as mock_query:
            mock_query.return_value = [
                {"id": "1", "name": "平安银行股份有限公司"},
                {"id": "2", "name": "万科企业股份有限公司"},
            ]
            results = self.repo.find_all()
            assert len(results) == 2

    def test_should_count(self) -> None:
        with patch("data_collector.adapters.db_company_repository.execute_query") as mock_query:
            mock_query.return_value = [{"cnt": 50}]
            result = self.repo.count()
            assert result == 50
