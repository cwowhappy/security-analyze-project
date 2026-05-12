"""采集脚本 run_* 函数集成风格单元测试。

使用 unittest.mock 替换 akshare / tushare 调用与数据库仓储。
"""

from datetime import date
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pandas as pd
import pytest

from data_collector.config import Settings
from data_collector.core.domain.company import Company
from data_collector.core.domain.stock import Stock
from data_collector.scripts.company_full import run_company_full
from data_collector.scripts.stock_full import run_stock_full


class TestRunStockFull:
    """run_stock_full 测试。"""

    @patch("data_collector.scripts.stock_full.ak")
    @patch("data_collector.scripts.stock_full.DbStockRepository")
    def test_run_stock_full_success(self, mock_repo_cls, mock_ak):
        """正常获取股票列表并保存。"""
        mock_df = pd.DataFrame({
            "code": ["000001", "600001"],
            "name": ["平安银行", "Test"],
            "market": ["SZ", "SH"],
        })
        mock_ak.stock_info_a_code_name.return_value = mock_df

        mock_repo = MagicMock()
        mock_repo.save_all.return_value = (2, 0)
        mock_repo_cls.return_value = mock_repo

        settings = Settings()
        result = run_stock_full(settings)

        assert result["total"] == 2
        assert result["success"] == 2
        assert result["failed"] == 0
        mock_ak.stock_info_a_code_name.assert_called_once()
        mock_repo.save_all.assert_called_once()
        saved_stocks = mock_repo.save_all.call_args[0][0]
        assert len(saved_stocks) == 2
        assert saved_stocks[0].stock_code == "000001"
        assert saved_stocks[1].stock_code == "600001"

    @patch("data_collector.scripts.stock_full.ak")
    @patch("data_collector.scripts.stock_full.DbStockRepository")
    def test_run_stock_full_empty_data(self, mock_repo_cls, mock_ak):
        """akshare 返回空数据时应返回 0。"""
        mock_ak.stock_info_a_code_name.return_value = pd.DataFrame()

        mock_repo = MagicMock()
        mock_repo_cls.return_value = mock_repo

        settings = Settings()
        result = run_stock_full(settings)

        assert result["total"] == 0
        assert result["success"] == 0
        assert result["failed"] == 0
        mock_repo.save_all.assert_not_called()

    @patch("data_collector.scripts.stock_full.ak")
    @patch("data_collector.scripts.stock_full.DbStockRepository")
    def test_run_stock_full_akshare_exception(self, mock_repo_cls, mock_ak):
        """akshare 异常时应抛出。"""
        mock_ak.stock_info_a_code_name.side_effect = Exception("网络超时")

        settings = Settings()
        with pytest.raises(Exception, match="网络超时"):
            run_stock_full(settings)


class TestRunCompanyFull:
    """run_company_full 测试。"""

    @patch("data_collector.scripts.company_full.ak")
    @patch("data_collector.scripts.company_full.DbStockRepository")
    @patch("data_collector.scripts.company_full.DbCompanyRepository")
    def test_run_company_full_with_skipped(self, mock_comp_repo_cls, mock_stock_repo_cls, mock_ak):
        """跳过已有 company_id 的股票。"""
        mock_stock_repo = MagicMock()
        mock_stock_repo.find_all.return_value = [
            Stock(stock_code="000001", name="A", company_id="already-has"),
            Stock(stock_code="600001", name="B"),
        ]
        mock_stock_repo_cls.return_value = mock_stock_repo

        mock_comp_repo = MagicMock()
        mock_comp_repo_cls.return_value = mock_comp_repo

        # akshare 返回空数据（模拟未找到公司详情）
        mock_ak.stock_profile_cninfo.return_value = pd.DataFrame()

        settings = Settings()
        result = run_company_full(settings)

        assert result["total"] == 1  # 只处理没有 company_id 的
        assert result["success"] == 0
        assert result["failed"] == 1

    @patch("data_collector.scripts.company_full.ak")
    @patch("data_collector.scripts.company_full.DbStockRepository")
    @patch("data_collector.scripts.company_full.DbCompanyRepository")
    def test_run_company_full_save_success(self, mock_comp_repo_cls, mock_stock_repo_cls, mock_ak):
        """成功获取公司详情并保存。"""
        mock_stock_repo = MagicMock()
        mock_stock_repo.find_all.return_value = [
            Stock(stock_code="000001", name="A"),
        ]
        mock_stock_repo_cls.return_value = mock_stock_repo

        mock_comp_repo = MagicMock()
        mock_comp_repo_cls.return_value = mock_comp_repo

        mock_df = pd.DataFrame({
            "统一社会信用代码": ["91230000MA0000000A"],
            "公司名称": ["测试公司"],
            "证券简称": ["测试"],
            "法人代表": ["张三"],
        })
        mock_ak.stock_profile_cninfo.return_value = mock_df

        settings = Settings()
        result = run_company_full(settings)

        assert result["total"] == 1
        assert result["success"] == 1
        assert result["failed"] == 0
        mock_comp_repo.save.assert_called_once()
        mock_stock_repo.update_company_id.assert_called_once()
