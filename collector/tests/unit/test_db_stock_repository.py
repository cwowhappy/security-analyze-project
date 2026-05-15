"""PostgreSQL 股票仓库单元测试。"""

from unittest.mock import MagicMock, patch

from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.core.domain.stock import Stock


class TestDbStockRepository:
    """DbStockRepository 测试。"""

    def setup_method(self) -> None:
        self.repo = DbStockRepository()

    def test_should_save_stock(self) -> None:
        with patch("data_collector.adapters.db_stock_repository.execute_update") as mock_update:
            mock_update.return_value = 1
            stock = Stock(stock_code="000001", name="平安银行")
            self.repo.save(stock)
            assert stock.id is not None
            mock_update.assert_called_once()
            sql = mock_update.call_args[0][0]
            assert "INSERT INTO tb_stock_basic" in sql
            assert "ON CONFLICT (stock_code)" in sql

    def test_should_save_all_stocks(self) -> None:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value = mock_cursor

        with patch("data_collector.adapters.db_stock_repository.transaction") as mock_tx:
            mock_tx.return_value.__enter__.return_value = mock_conn
            stocks = [
                Stock(stock_code="000001", name="平安银行"),
                Stock(stock_code="000002", name="万科A"),
            ]
            success, failed = self.repo.save_all(stocks)
            assert success == 2
            assert failed == 0
            assert mock_cursor.execute.call_count == 2

    def test_should_handle_save_all_failure(self) -> None:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.execute.side_effect = [None, Exception("DB Error")]
        mock_conn.cursor.return_value = mock_cursor

        with patch("data_collector.adapters.db_stock_repository.transaction") as mock_tx:
            mock_tx.return_value.__enter__.return_value = mock_conn
            stocks = [
                Stock(stock_code="000001", name="平安银行"),
                Stock(stock_code="000002", name="万科A"),
            ]
            success, failed = self.repo.save_all(stocks)
            assert success == 1
            assert failed == 1

    def test_should_find_by_symbol(self) -> None:
        with patch("data_collector.adapters.db_stock_repository.execute_query") as mock_query:
            mock_query.return_value = [{
                "id": "test-id",
                "stock_code": "000001",
                "name": "平安银行",
                "ts_code": "000001.SZ",
            }]
            result = self.repo.find_by_symbol("000001")
            assert result is not None
            assert result.stock_code == "000001"
            assert result.name == "平安银行"
            mock_query.assert_called_once()

    def test_should_return_none_when_not_found(self) -> None:
        with patch("data_collector.adapters.db_stock_repository.execute_query") as mock_query:
            mock_query.return_value = []
            result = self.repo.find_by_symbol("999999")
            assert result is None

    def test_should_find_all(self) -> None:
        with patch("data_collector.adapters.db_stock_repository.execute_query") as mock_query:
            mock_query.return_value = [
                {"id": "1", "stock_code": "000001", "name": "平安银行"},
                {"id": "2", "stock_code": "000002", "name": "万科A"},
            ]
            results = self.repo.find_all()
            assert len(results) == 2
            assert results[0].stock_code == "000001"

    def test_should_count(self) -> None:
        with patch("data_collector.adapters.db_stock_repository.execute_query") as mock_query:
            mock_query.return_value = [{"cnt": 150}]
            result = self.repo.count()
            assert result == 150
